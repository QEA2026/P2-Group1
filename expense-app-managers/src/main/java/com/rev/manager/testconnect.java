package com.rev.manager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import java.time.LocalDate;

import java.io.Console;

import com.rev.manager.model.*;

import java.util.ArrayList;

public class testconnect {
public static void main(String[] args) {
    try(Connection con = DriverManager.getConnection("jdbc:sqlite:testjava.db")){
        System.out.println("Connection established");
        PreparedStatement p = con.prepareStatement("SELECT * FROM expenses;");
        ResultSet r = p.executeQuery();
    } catch (SQLException e) {
        e.printStackTrace();
    }
   //LocalDate today = LocalDate.now();
   //System.out.println(today);
   
   //System.out.println("2026/06/24".compareTo("2026/06/25"));

   //System.out.printf("%-20s%-10s%-20s\n","NAME","NUMBER","DESCRIPTION");
   //System.out.println("------------------------------------------------");
   //System.out.printf("%-20s$%-10.2f%-20s\n","Andrew",18.24,"Dinner");
   //System.out.printf("%-20s$%-10.2f%-20s\n","Bob",1450.997,"New Car");
   //System.out.println(Status.approved);
   //System.out.println(Status.valueOf("approved"));

   /*
   Console console = System.console();
   char[] passwordArray = console.readPassword("Enter your password: ");
   String password = new String(passwordArray);
   System.out.println(password);
    Expense exp = new Expense(35, "Marko", 29.99, "Computer mouse", "2026/07/02", Category.Supplies, Status.approved, null, null, null);
    String s = String.format("│ %-3d │ %-15s │ %-10.2f │ %-30s │ %-10s │ %-15s │ %-10s │ %-20s │ %-30s │ %-10s │",
    exp.getExpense_id(), exp.getEmp_name(), exp.getAmount(), exp.getDescription(), exp.getExp_date(), exp.getCategory().toString(), exp.getStatus().toString(), exp.getReviewer_name(), exp.getComment(), exp.getReview_date()
    );
    System.out.println(s);
    for(int k = 0; k < s.length(); k++){
        if(s.charAt(k) == '|')
            System.out.print('|');
        else
            System.out.print(' ');
    }
    String barHeader = "╭─────┬─────────────────┬────────────┬────────────────────────────────┬────────────┬─────────────────┬────────────┬──────────────────────┬────────────────────────────────┬────────────╮";
    String barLabels = "│EXPID│  EMPLOYEE NAME  │   AMOUNT   │          DESCRIPTION           │EXPENSE_DATE│     CATEGORY    │   STATUS   │     REVIEWER_NAME    │           COMMENT              │ REVIEW_DATE│";
    String barUnder  = "╞═════╪═════════════════╪════════════╪════════════════════════════════╪════════════╪═════════════════╪════════════╪══════════════════════╪════════════════════════════════╪════════════╡";
    String barCloser = "╰─────┴─────────────────┴────────────┴────────────────────────────────┴────────────┴─────────────────┴────────────┴──────────────────────┴────────────────────────────────┴────────────╯";
    System.out.println(barHeader);
    System.out.println(barLabels);
    System.out.println(barUnder);
    System.out.println(s);
    System.out.println(s);
    System.out.println(s);
    System.out.println(barCloser);
    */
}
}
