package com.rev.manager;
import java.io.Console;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

import com.rev.manager.DAO.JDBCManagerDAO;
import com.rev.manager.DAO.ManagerException;
import com.rev.manager.DAO.ManagerException.InvalidLoginException;
import com.rev.manager.DAO.ManagerException.UserNotFoundException; //For reading in passwords
import com.rev.manager.model.Category;
import com.rev.manager.model.Expense;

/**
 * A class to run the manager app, that allows for managers to view, approve, deny, and generate reports about expenses through the command line.
 * When run, it will first prompt the manager to enter a username and password to log them in.
 * After logging in, the manager will select from a list of options on what they want to do (ex: view expenses).
 */
public class ManagerApp{

    private static String DATAURL = "jdbc:sqlite:revExpenseData.db";
    private static long manager_id = -1; //Set to the id of whatever manager is logged in


    /**
     * Static method to get a numerical input from the user, with error checking.
     * Continually prompts the user for input until they input an integer.
     * @param sc A scanner object that will read in inputs.
     * @param prompt A message to prompt the user for input with.
     * @return The integer the user inputs.
     */
    private static int get_num_input(Scanner sc, String prompt){
        int user_int = -1;
        while(true){
            try{
                System.out.print(prompt);
                user_int = Integer.parseInt(sc.nextLine());
                break;
            }
            catch(NumberFormatException e){
                System.out.println("Error, please enter a valid number.");
            }
        }
        return user_int;
    }

    /**
     * Gets a string response from the user.
     * Will ask user to confirm if their response is correct before exiting.
     * @param sc A scanner object that will read in inputs.
     * @param prompt A message to prompt the user for input with.
     * @return The string the user inputs.
     */
    private static String get_string_input(Scanner sc, String prompt, boolean verify){
        String user_string = "";
        while(true){
            System.out.print(prompt);
            user_string = sc.nextLine();
            if(verify){
                System.out.println("Entered reason is...\n"+user_string+"\nIs this correct?");
                if(get_num_input(sc,"Enter 1 to confirm:") == 1)
                    break;
            }
            else
                break;
        }
        return user_string;
    }

    /**
     * Runner for the manager app, which will first get the login for a manager, then prompt them with options until they exit.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        try(Connection conn = DriverManager.getConnection(DATAURL)){
        JDBCManagerDAO m = new JDBCManagerDAO(conn);
        

        while(true){ // Loop until we get a valid username and password.
            System.out.println("To login, please enter username and password.");
            String username = get_string_input(sc,"Username: ",false);
            //String password = get_string_input(sc,"Password:  ",false); //Lacks masking
            Console console = System.console();
            char[] passArray = console.readPassword("Password: "); // Can enter password without it showing in the terminal
            String password = new String(passArray);
            
            try{
                manager_id = m.login(username,password);
                System.out.println("Welcome, "+username);
                break;
            } catch(InvalidLoginException e){
                System.out.println(e.getMessage());
            }
        }

        while(true){
            System.out.println("-------------------\nWhat do you want to do? (Type the number of the option you want)");
            System.out.println("1: View expenses");
            System.out.println("2: Approve expense");
            System.out.println("3: Deny expense");
            System.out.println("4: Generate report");
            System.out.println("5: Exit");
            int option = get_num_input(sc,"Enter number choice:");
            System.out.println("-------------------");
            switch(option) {
                case 1: //View expenses
                    List<Expense> expenses = m.view_expenses();
                    break;
                case 2: //Approve expense
                    //If exp_id can not be found, m.approve_exp() will return false, and the user should be prompted if they want to try again.
                    System.out.println("(Get expense ids through the View expenses option)");
                    while(true){
                        long exp_id = get_num_input(sc,"Enter expense id for approval (or -1 to exit): ");
                        if(exp_id == -1){
                            break;
                        }
                        String approve_reason = get_string_input(sc,"Give a reason for the approval: ",false);
                        try{
                            Expense approved = m.approve_exp(manager_id, exp_id,approve_reason);
                            System.out.println("Expense "+exp_id+" approved!");
                            if(! get_string_input(sc,"Do you want to approve any more expenses? (y/n): ", false).equals("y")){
                                break;
                            }
                        } catch (ManagerException e){
                            System.out.println(e.getMessage());
                        }
                    }
                    break;
                case 3: //Deny an expense
                    System.out.println("(Get expense ids through the View expenses option)");
                    while(true){
                        long exp_id = get_num_input(sc,"Enter expense id for denial (or -1 to exit): ");
                        if(exp_id == -1){
                            break;
                        }
                        String deny_reason = get_string_input(sc,"Give a reason for the denial: ",false);
                        try{
                            Expense denied = m.deny_exp(manager_id, exp_id,deny_reason);
                            System.out.println("Expense "+exp_id+" denied!");
                            if(! get_string_input(sc,"Do you want to deny any more expenses? (y/n): ", false).equals("y")){
                                break;
                            }
                        } catch (ManagerException e){
                            System.out.println(e.getMessage());
                        }
                    }
                    break;
                case 4: // Generating reports
                    while(true){
                        System.out.println("What kind of report do you want?");
                        int option2 = get_num_input(sc,"1 for employee report, 2 for category report, 3 for time report, 4 to exit\nOption: ");
                        switch(option2){
                            case 1:
                                System.out.println("(To generate a list of employee usernames, type \"List\")");
                                while(true){
                                    String empUsername = get_string_input(sc, "Enter the username of the employee you want a report on (or exit to exit out): ",false);
                                    if(empUsername.equals("exit")){
                                        break;
                                    }
                                    else if(empUsername.toLowerCase().equals("list")){
                                        m.get_valid_emp_usernames();
                                        continue;
                                    }
                                    try{
                                        List<Expense> expenses2 = m.gen_report_emp(empUsername);
                                        break;
                                    } catch (UserNotFoundException e){
                                        System.out.println(e.getMessage());
                                    }
                                }
                                break;
                            case 2:
                                System.out.println("Enter the number corresponding to the category you want a report on...");
                                System.out.println("1) Supplies");
                                System.out.println("2) Travel");
                                System.out.println("3) Services");
                                System.out.println("4) Repairs");
                                System.out.println("5) Meals");
                                System.out.println("6) Certifications");
                                System.out.println("7) Other");
                                boolean isValidOption = true;
                                while(true){
                                    int choice = get_num_input(sc, "Enter your option here (or -1 to exit): ");
                                    isValidOption = true;
                                    Category c = Category.Other;
                                    if(choice == -1){
                                        break;
                                    }
                                    switch(choice){
                                        case 1:
                                            c = Category.Supplies;
                                            break;
                                        case 2:
                                            c = Category.Travel;
                                            break;
                                        case 3:
                                            c = Category.Services;
                                            break;
                                        case 4:
                                            c = Category.Repairs;
                                            break;
                                        case 5:
                                            c = Category.Meals;
                                            break;
                                        case 6:
                                            c = Category.Certifications;
                                            break;
                                        case 7:
                                            c = Category.Other;
                                            break;
                                        default:
                                            System.out.println("ERROR: Input out of range, try again.");
                                            isValidOption = false;
                                    }
                                    if(isValidOption){
                                        List<Expense> expenses3 = m.gen_report_cat(c);
                                        break;
                                    }
                                }
                                break;
                            case 3:
                                //TODO: Validate inputs are correctly formatted, or force correct user input.
                                System.out.println("Dates are formatted as YYYY/mm/dd");
                                while(true){
                                    String start_date = get_string_input(sc, "Enter the start date for the report (exactly as YYYY/mm/dd): ", false);
                                    String end_date = get_string_input(sc, "Enter the end date for the report (exactly as YYYY/mm/dd): ", false);
                                    List<Expense> expenses3 = m.gen_report_date(start_date, end_date);
                                    break;
                                }
                            case 4:
                                break;
                            default:
                                break;
                        }
                        if(option2 < 0 || option2 > 4)// Invalid input
                            System.out.println("ERROR: Input out of range.");
                        else
                            break;
                        
                    }
                    break;
                case 5:
                    System.out.println("Thank you for using the ManagerApp!");
                    System.out.println("Exiting...");
                    sc.close();
                    System.exit(0);
                default: //Invalid input
                    System.out.println("ERROR: Input out of range, try again.");
                    break;
            }
        }
    } catch( SQLException e){
        e.printStackTrace();
    }
    }
}