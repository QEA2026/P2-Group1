package com.rev.manager.DAO;

import java.util.List;
import java.util.Optional;

import com.rev.manager.DAO.ManagerException.ExpenseNotFoundException;
import com.rev.manager.DAO.ManagerException.ExpenseNotPendingException;
import com.rev.manager.DAO.ManagerException.InvalidLoginException;
import com.rev.manager.DAO.ManagerException.UserNotFoundException;
import com.rev.manager.model.Category;
import com.rev.manager.model.Expense;

public interface ManagerDAO {
    public Optional<Expense> findExpenseById(long id);
    public long login(String username, String password) throws InvalidLoginException;

    public List<Expense> view_expenses(); //Only pending expenses.
    public List<Expense> view_all_expenses();
    public Expense approve_exp(long manager_id, long exp_id, String comment) throws ExpenseNotFoundException, ExpenseNotPendingException;
    public Expense deny_exp(long manager_id, long exp_id, String comment) throws ExpenseNotFoundException, ExpenseNotPendingException;

    public List<Expense> gen_report_emp(String empUsername) throws UserNotFoundException;
    public List<Expense> gen_report_cat(Category category);
    public List<Expense> gen_report_date(String startDate, String endDate);
}
