package activity3;

import java.util.*;

public class GRADES {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        int[][] grades = new int[3][3];

        String[] gradeCategory = {"PRELIMS", "MIDTERMS", "FINALS"};

        System.out.println("""
                [1] Enter Grades
                [2] Display Grades
                [3] Exit
                """);

        System.out.println("Choose an option: ");
        int choice = sc.nextInt();

        switch (choice) {
            case 1 -> {
                System.out.println("""
                [1] COMPRO2
                [2] DSA
                [3] OOP
                [4] EXIT
                """);
                int gradeChoice = sc.nextInt();

                switch (gradeChoice) {
                    case 1 -> {

                        for (int i = 0; i < grades.length; i++) {
                            System.out.print(gradeCategory[i] + ": ");
                            grades[0][i] = sc.nextInt();
                        }

                        for (int i = 0; i < grades.length; i++) {
                            System.out.print(grades[0][i] + " ");
                        }

                    }
                    case 2 -> {

                        for (int i = 0; i < grades.length; i++) {
                            System.out.print(gradeCategory[i] + ": ");
                            grades[1][i] = sc.nextInt();
                        }

                        for (int i = 0; i < grades.length; i++) {
                            System.out.print(grades[1][i] + " ");
                        }

                    }
                    case 3 -> {

                        for (int i = 0; i < grades.length; i++) {
                            System.out.print(gradeCategory[i] + ": ");
                            grades[2][i] = sc.nextInt();
                        }

                        for (int i = 0; i < grades.length; i++) {
                            System.out.print(grades[2][i] + " ");
                        }

                    }
                }
            }
        }

        

    }
}

// for (int i = 0; i < grades.length; i++) {
//                     System.out.print("Element " + (i + 1) + ": ");
//                     grades[i][] = sc.nextInt();
//                 }
