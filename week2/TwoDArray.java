package week2;

public class TwoDArray {
    public static void main(String[] args) {
        // initialize 2d array
        String[] clothTypes = {"Shirt", "Pants"};
        String[][] clothColors = {
            {"red", "blue", "green"},
            {"orange", "yellow", "violet"}
        };

        // print the 2D array
        for (int i = 0; i < clothColors.length; i++) {
            System.out.println(clothTypes[i]);
            for (int j = 0; j < clothColors[i].length; j++) {
                System.out.printf("%-10s", clothColors[i][j]);
            }
            System.out.println();
        }
    }
}