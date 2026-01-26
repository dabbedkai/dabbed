package activity2;

import java.util.*;

public class Week3Activity2 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        double[][] arrGrades = new double[3][3];

        System.out.println("Enter a 3-by-4 matrix row by row:");

        for (int i = 0; i < arrGrades.length; i++) {
            for (int j = 0; j < arrGrades[i].length; j++) {
                arrGrades[i][j] = sc.nextDouble();
            }
        }
    }
}
