package practice.week3;

import java.util.*;

public class TryExceptionPractice {

    public static Scanner sc = new Scanner(System.in);

    public static int inputNumber() {

        int num = 0;

        boolean validInput = false;

        do {
            try {

                System.out.print("Enter an integer: ");
                num = sc.nextInt();

                sc.nextLine();

                validInput = true;

                System.out.println("You entered: " + num);

            } catch (InputMismatchException e) {

                sc.nextLine();
                System.out.println("That's not a valid integer!");

            }

        } while (validInput == false);

        return num;

    }

    public static void main(String[] args) {
        inputNumber();
    }
}
