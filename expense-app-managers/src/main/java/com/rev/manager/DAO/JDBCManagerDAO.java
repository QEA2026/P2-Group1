package com.rev.manager.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.rev.manager.DAO.ManagerException.ExpenseNotFoundException;
import com.rev.manager.DAO.ManagerException.ExpenseNotPendingException;
import com.rev.manager.DAO.ManagerException.InvalidLoginException;
import com.rev.manager.DAO.ManagerException.UserNotFoundException;
import com.rev.manager.model.Category;
import com.rev.manager.model.Expense;
import com.rev.manager.model.Status;

public class JDBCManagerDAO implements ManagerDAO{

    // String to join all the tables needed to represent a full expense, with all the information from
    // the user, expense, and approval tables
    static final String FULL_EXPENSE_SQL = """
            SELECT
            e.id, eu.username AS username_emp, e.amount, e.description, e.date, e.category,
            a.status, mu.username AS username_manager, a.comment, a.review_date
            FROM expenses e
            INNER JOIN approvals a ON e.id = a.expense_id
            INNER JOIN users eu ON e.user_id = eu.id
            LEFT JOIN users mu ON a.reviewer = mu.id
            """;

    private final Connection conn;
    private final boolean DEBUG_MODE = true;

    /**
     * Creates a new JDBCManagerDAO on the given connection.
     * Allows you to run all the functions in the ManagerDAO through JDBC.
     * @param conn A Connection object that is connected to the database.
     */
    public JDBCManagerDAO(Connection conn){
        this.conn=conn;
    }

    /**
     * Method to validate that the username and password are in the database.
     * If the username and password are valid login credentials for a manager, returns manager id.
     * If the username and password are not a valid combination, then an InvalidLoginException is thrown.
     */
    @Override
    public long login(String username, String password) throws InvalidLoginException{
        try(PreparedStatement p = conn.prepareStatement("SELECT id FROM users WHERE username = ? AND password = ? AND role = 'Manager';")){
            p.setString(1, username);
            p.setString(2, password);
            try(ResultSet rs = p.executeQuery()){
                if(rs.next()){
                    return rs.getLong("id");
                }
                else{
                    throw new InvalidLoginException("Error: username and password combination not found in database.");
                }
            }
        } catch (SQLException e) {
            if(DEBUG_MODE){
                e.printStackTrace();
            }
            else{
                System.out.println("FATAL DATABASE ERROR, please contact support.");
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Prints a list of all pending expenses, giving their id, the username of who submitted it, the amount,
     * a description of the expense, the date, the category, and the status, which will be pending.
     * If there are no expenses pending approval, a message stating so will be printed instead.
     * Returns a list of expenses
     */
    @Override
    public List<Expense> view_expenses(){
        List<Expense> expenses = new ArrayList<>();

        try(PreparedStatement p = conn.prepareStatement(FULL_EXPENSE_SQL + " WHERE a.status = 'pending' ORDER BY e.date DESC, e.amount DESC")){
            try(ResultSet rs = p.executeQuery()){
                if(rs.next()){
                    Expense.printTableHeader();
                    do{
                        Expense exp = mapRow(rs);
                        expenses.add(exp);
                        System.out.println(exp);
                    }while(rs.next());
                    Expense.printTableCloser();
                }
                else{
                    System.out.println("No expenses pending approval at this time.");
                }
            }
        } catch (SQLException e) {
            if(DEBUG_MODE){
                e.printStackTrace();
            }
            else{
                System.out.println("FATAL DATABASE ERROR, please contact support.");
            }
        }
        
        return expenses;
    }

    /**
     * Returns a list of all expenses in the database.
     */
    @Override
    public List<Expense> view_all_expenses(){
        List<Expense> expenses = new ArrayList<>();

        try(PreparedStatement p = conn.prepareStatement(FULL_EXPENSE_SQL + " ORDER BY a.status ASC, e.date DESC, e.amount DESC")){
            try(ResultSet rs = p.executeQuery()){
                if(rs.next()){
                    //Expense.printTableHeader();
                    do{
                        Expense exp = mapRow(rs);
                        expenses.add(exp);
                        //System.out.println(exp);
                    }while(rs.next());
                    //Expense.printTableCloser();
                }
                else{
                    System.out.println("No expenses pending approval at this time.");
                }
            }
        } catch (SQLException e) {
            if(DEBUG_MODE){
                e.printStackTrace();
            }
            else{
                System.out.println("FATAL DATABASE ERROR, please contact support.");
            }
        }
        
        return expenses;
    }

    /**
     * Approves a pending expense in the database, with the given exp_id.
     * @param exp_id The id of the expense in the database to be approved.
     * @param comment A comment to put in the database as to why this expense is approved.
     * @throws ExpenseNotFoundException if the given exp_id is not found in the database.
     * @throws ExpenseNotPendingException if the given exp_id is found but has a status that is not pending.
     * @return Expense if the operation was successful, ExpenseNotFoundException otherwise.
     */
    @Override
    public Expense approve_exp(long manager_id, long exp_id, String comment) throws ExpenseNotFoundException, ExpenseNotPendingException{
        
        Expense exp = findExpenseById(exp_id)
            .orElseThrow(() ->
                new ExpenseNotFoundException(
                    "No expense with id " + exp_id + " found."
                )
            );

        if (exp.getStatus() != Status.pending) {
            throw new ExpenseNotPendingException(
                    "Expense " + exp_id +
                    " cannot be approved because it is already "
                    + exp.getStatus()
            );
        }

        try (PreparedStatement p2 = conn.prepareStatement("""
                                UPDATE approvals
                                SET status = 'approved', reviewer = ?, comment = ?, review_date = ?
                                WHERE expense_id = ?
                                """)) {

            p2.setLong(1, manager_id);
            p2.setString(2, comment);
            p2.setString(3, LocalDate.now().toString().replace('-', '/'));
            p2.setLong(4, exp_id);

            int rowsUpdated = p2.executeUpdate();

            if (rowsUpdated != 1) {
                throw new ExpenseNotFoundException(
                    "Expense " + exp_id + " could not be approved."
                );
            }
        } catch (SQLException e) {
            if(DEBUG_MODE){
                e.printStackTrace();
            }

            throw new RuntimeException("Database error approving expense.", e);
        }

        // Return the database version of Expense after update
        return findExpenseById(exp_id)
                .orElseThrow(() ->
                    new ExpenseNotFoundException(
                        "Error: the expense with id "+exp_id+" no longer  in database found after update"
                    )
                );
    }

    /**
     * Denies a pending expense in the database, with the given exp_id.
     * @param exp_id The id of the expense in the database to be denied.
     * @param comment A comment to put in the database as to why this expense is denied.
     * @throws ExpenseNotFoundException if the given exp_id is not found in the database.
     * @throws ExpenseNotPendingException if the given exp_id is found but has a status that is not pending.
     * @return Expense if the operation was successful, ExpenseNotFoundException otherwise.
     */
    @Override
    public Expense deny_exp(long manager_id, long exp_id, String comment) throws ExpenseNotFoundException, ExpenseNotPendingException{
        Expense exp = findExpenseById(exp_id)
            .orElseThrow(() ->
                new ExpenseNotFoundException(
                    "No expense with id " + exp_id + " found."
                )
            );

        if (exp.getStatus() != Status.pending) {
            throw new ExpenseNotPendingException(
                    "Error: expense "+exp_id+" can not be denied, it is already "+exp.getStatus()
            );
        }

        try (PreparedStatement p2 = conn.prepareStatement("""
                                UPDATE approvals
                                SET status = 'denied', reviewer = ?, comment = ?, review_date = ?
                                WHERE expense_id = ?
                                """)) {

            p2.setLong(1, manager_id);
            p2.setString(2, comment);
            p2.setString(3, LocalDate.now().toString().replace('-', '/'));
            p2.setLong(4, exp_id);

            int rowsUpdated = p2.executeUpdate();

            if (rowsUpdated != 1) {
                throw new ExpenseNotFoundException(
                    "Expense " + exp_id + " could not be denied."
                );
            }
        } catch (SQLException e) {
            if(DEBUG_MODE){
                e.printStackTrace();
            }

            throw new RuntimeException("Database error approving expense.", e);
        }

        // Return the database version of Expense after update
        return findExpenseById(exp_id)
                .orElseThrow(() ->
                    new ExpenseNotFoundException(
                        "Error: the expense with id "+exp_id+" no longer found in database after update"
                    )
                );
    }

    /**
     * Prints a report containing all expenses that belong to a given user.
     * The expenses can have any status (pending, approved, denied)
     * If the employee has reported no expenses, a message saying so will be printed instead.
     * @param empUsername The username of the employee to generate a report on.
     * @throws UserNotFoundException if no employee with the given empUsername exists in the database.
     * @return List<Expense> if the employee exists, UserNotFoundException otherwise
     */
    @Override
    public List<Expense> gen_report_emp(String empUsername) throws UserNotFoundException{
        //First check that the empUsername exists in the database.
        List<Expense> expenses = new ArrayList<>();

        try(PreparedStatement p = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND role = 'Employee'")){
            p.setString(1,empUsername);
            try(ResultSet rs = p.executeQuery()){
                if(! rs.next()){
                    throw new UserNotFoundException("Error: No employee found with username " + empUsername + ".");
                }
            }
        }catch (SQLException e){
            throw new RuntimeException("Database error finding employee.", e);
        }

        try(PreparedStatement p = conn.prepareStatement(FULL_EXPENSE_SQL + " WHERE eu.username = ? ORDER BY a.status DESC, e.date DESC, e.amount DESC;")){
            p.setString(1, empUsername);
            try(ResultSet rs = p.executeQuery()){
                if(rs.next()){
                    Expense.printTableHeader();
                    do{
                        Expense exp = mapRow(rs);
                        expenses.add(exp);
                        System.out.println(exp);
                    }while(rs.next());
                    Expense.printTableCloser();
                }
                else{
                    System.out.println("No expenses reported by " + empUsername);
                }
            }
        }catch (SQLException e){
            if(DEBUG_MODE){
                e.printStackTrace();
            }

            throw new RuntimeException("Database error generating report by employee", e);
        }

        return expenses;
    }


    /**
     * Prints a report containing all expenses that belong to a given category.
     * The expenses can have any status (pending, approved, denied)
     * If no expenses are found under the category, a message saying so will be printed instead.
     * @param category One of the categories specified in the Category enum.
     * @return List<Expense> if expenses exists for that catergory, RuntimeException otherwise
     */
    @Override
    public List<Expense> gen_report_cat(Category category) {
        List<Expense> expenses = new ArrayList<>();

        try(PreparedStatement p = conn.prepareStatement(FULL_EXPENSE_SQL + " WHERE e.category = ? ORDER BY a.status DESC, e.date DESC, e.amount DESC;")){
            p.setString(1, category.toString());
            try(ResultSet rs = p.executeQuery()){
                if(rs.next()){
                    Expense.printTableHeader();
                    do{
                        Expense exp = mapRow(rs);
                        expenses.add(exp);
                        System.out.println(exp);
                    }while(rs.next());
                    Expense.printTableCloser();
                }
                else{
                    System.out.println("No expenses found under category " + category);
                }
            }
        }catch (SQLException e){
            if(DEBUG_MODE){
                e.printStackTrace();
            }

            throw new RuntimeException("Database error generating report by category", e);
        }

        return expenses;
    }


    /**
     * Prints a report containing all expenses that were submitted between the given start and end dates.
     * The expenses can have any status (pending, approved, denied)
     * If no expenses are found between the given times, a message saying so will be printed instead.
     * @param startDate A string representing the time in the format "YYYY/MM/DD"
     * @param endDate A string representing the time in the format "YYYY/MM/DD"
     * @return List<Expense> if expenses exists for that date range, RuntimeException otherwise
     */
    @Override
    public List<Expense> gen_report_date(String startDate, String endDate) {
        List<Expense> expenses = new ArrayList<>();

        try(PreparedStatement p = conn.prepareStatement(FULL_EXPENSE_SQL + " WHERE e.date >= ? AND e.date <= ? ORDER BY e.date DESC, a.status DESC, e.amount DESC;")){
            p.setString(1, startDate);
            p.setString(2, endDate);
            try(ResultSet rs = p.executeQuery()){
                if(rs.next()){
                    Expense.printTableHeader();
                    do{
                        Expense exp = mapRow(rs);
                        expenses.add(exp);
                        System.out.println(exp);
                    }while(rs.next());
                    Expense.printTableCloser();
                }
                else{
                    System.out.println("No reports found between dates " + startDate + " and " + endDate + ".");
                }
            }
        }catch (SQLException e){
            if(DEBUG_MODE){
                e.printStackTrace();
            }

            throw new RuntimeException("Database error generating report by date range", e);
        }

        return expenses;
    }

    /**
     * Finds an Expense by id
     * @param id The id of the expense to be retrieved
     * @return Optional<Expense> return an Optional<Expense> if an expense with given id is found, empty otherwise 
     */
    @Override
    public Optional<Expense> findExpenseById(long id) {

        try (PreparedStatement p = conn.prepareStatement(
                FULL_EXPENSE_SQL + " WHERE e.id = ?")) {

            p.setLong(1, id);

            try (ResultSet rs = p.executeQuery()) {

                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            if (DEBUG_MODE) {
                e.printStackTrace();
            }
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    private Expense mapRow(ResultSet rs){
        Category cat = Category.Other;
        try{
            cat = Category.valueOf(rs.getString("category"));
        }
        catch(SQLException e){
            if(DEBUG_MODE){
                e.printStackTrace();
            }
            else{
                System.out.println("FATAL DATABASE ERROR, please contact support.");
            }
            return null;
        }
        catch(IllegalArgumentException e){ //On invalid category, default to other
            if(DEBUG_MODE){
                System.out.println("Invalid category found in database");
            }
        }
        try{
            if(Status.valueOf(rs.getString("status")).equals(Status.pending)){
                return new Expense(
                    rs.getLong("id"),
                    rs.getString("username_emp"),
                    rs.getDouble("amount"),
                    rs.getString("description"),
                    rs.getString("date"),
                    cat
                );
            }
            else{
                return new Expense(
                    rs.getLong("id"),
                    rs.getString("username_emp"),
                    rs.getDouble("amount"),
                    rs.getString("description"),
                    rs.getString("review_date"),
                    cat,
                    Status.valueOf(rs.getString("status")),
                    rs.getString("username_manager"),
                    rs.getString("comment"),
                    rs.getString("review_date")
                );
            }
        }
        catch(SQLException e){
            if(DEBUG_MODE){
                e.printStackTrace();
            }
            else{
                System.out.println("FATAL DATABASE ERROR, please contact support.");
            }
            return null;
        }
    }

    // QUALITY OF LIFE FUNCTIONS:
    
    /**
     * Will print out a list of all the employees in the database, so that a manager can chose who to generate a report on.
     */
    public void get_valid_emp_usernames(){
        try(PreparedStatement p = conn.prepareStatement("SELECT username FROM users WHERE role = 'Employee' ORDER BY username")){
            try(ResultSet rs = p.executeQuery()){
                int count = 1;
                System.out.println("List of employee names...");
                while(rs.next()){
                    System.out.println(count + ")" +rs.getString("username"));
                    count++;
                }
            }
        }
        catch(SQLException e){
            if(DEBUG_MODE){
                e.printStackTrace();
            }
            else{
                System.out.println("FATAL DATABASE ERROR, please contact support.");
            }
        }
    }
}
