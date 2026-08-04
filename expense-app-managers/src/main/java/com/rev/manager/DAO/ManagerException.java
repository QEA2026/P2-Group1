package com.rev.manager.DAO;

/**
 * Generic exception for all exceptions that can be thrown by the JDBCManagerDAO
 * ManagerException
 */
public class ManagerException extends Exception{
    public ManagerException(String message){
        super(message);
    }

    /**
     * Thrown when an invalid username and password combination are given in the login function.
     * InvalidLoginException
     */
    public static class InvalidLoginException extends ManagerException{
        public InvalidLoginException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when an expense id is provided that is not found in the database.
     * ExpenseNotFoundException
     */
    public static class ExpenseNotFoundException extends ManagerException{
        public ExpenseNotFoundException(String message){
            super(message);
        }
    }

    /**
     * Thrown when an expense is supposed to be pending approval, but is not (either approved or denied)
     * ExpenseNotPendingException
     */
    public static class ExpenseNotPendingException extends ManagerException{
        public ExpenseNotPendingException(String message){
            super(message);
        }
    }

    /**
     * Thrown when a username is not found inside the database.
     * UserNotFoundException
     */
    public static class UserNotFoundException extends ManagerException{
        public UserNotFoundException(String message){
            super(message);
        }
    }
}
