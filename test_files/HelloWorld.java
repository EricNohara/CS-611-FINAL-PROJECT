package test_files;

/**
 * CS611 - Spring 2025
 * Simple Hello World program
 */

 import java.util.Scanner;

 public class HelloWorld {
     public static void main(String[] args) {
         // Create a Scanner object for reading input
         Scanner scanner = new Scanner(System.in);
         
         // Print a welcome message
         System.out.println("Hello, World!");
         System.out.println("Welcome to CS611 Programming Assignment");
         
         // Ask for the user's name
         System.out.print("Please enter your name: ");
         String name = scanner.nextLine();
         
         // Greet the user
         System.out.println("Hello, " + name + "! Nice to meet you.");
         
         // Ask for a number
         System.out.print("Enter a number: ");
         int number = scanner.nextInt();
         
         // Display the square of the number
         System.out.println("The square of " + number + " is " + (number * number));
         
         // Close the scanner
         scanner.close();
         
         // Print a goodbye message
         System.out.println("Thank you for using this program. Goodbye!");
     }
 }