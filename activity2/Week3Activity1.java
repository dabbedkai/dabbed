package activity2;

import java.util.*;

public class Week3Activity1 {

    public static double sumColumn(double[][] m, int columnIndex) {
        double sum = 0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i][columnIndex];
        }
        return sum;
    }

    public static double sumMajorDiagonal(double[][] m) {
        double sum = 0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i][i];
        }

        return sum;
    }

    public static double reverseSumMajorDiagonal(double[][] m) {
        double sum = 0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i][m.length - 1 - i];
        }

        return sum;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        double[][] matrix3x4 = new double[3][4];
        double[][] matrix4x4 = new double[4][4];

        System.out.println("Enter a 3-by-4 matrix row by row:");

        for (int i = 0; i < matrix3x4.length; i++) {
            for (int j = 0; j < matrix3x4[i].length; j++) {
                matrix3x4[i][j] = sc.nextDouble();
            }
        }

        for (int i = 0; i <= matrix3x4.length; i++) {
            System.out.println("Sum of column " + (i + 1) + " is " + sumColumn(matrix3x4, i));
        }

        System.out.println("Enter a 4-by-4 matrix row by row:");

        for (int i = 0; i < matrix4x4.length; i++) {
            for (int j = 0; j < matrix4x4[i].length; j++) {
                matrix4x4[i][j] = sc.nextDouble();
            }
        }

        System.out.println("Sum of the major diag is " + sumMajorDiagonal(matrix4x4));
        System.out.println("Sum of the reverse major diag is " + reverseSumMajorDiagonal(matrix4x4));
    }
}
