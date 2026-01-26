package activities.activity1;

import java.util.*;

public class Week2Activity2 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        int randomRow = (int) (Math.random() * ((5 - 1) + 1)) + 1;
        int randomColumn = (int) (Math.random() * ((8 - 1) + 1)) + 1;

        String[][] theaterSeats = new String[5][8];
        String seat = "|-|";

        for (String[] theaterSeat : theaterSeats) {
            Arrays.fill(theaterSeat, seat);
        }

        String choice;
        String anotherChoice;

        theaterSeats[2][5] = "|x|";
        theaterSeats[0][0] = "|x|";

        System.out.print("Book Seat? (Y/N): ");
        choice = sc.nextLine().toLowerCase();

        do {
            int availableSeats = 0;

            if ("y".equals(choice) || "Y".equals(choice)) {
                theaterSeats[randomRow - 1][randomColumn - 1] = "|x|";
            }

            for (int i = 0; i < theaterSeats.length; i++) {

                for (int j = 0; j < theaterSeats[i].length; j++) {
                    if ("|x|".equals(theaterSeats[i][j])) {
                        availableSeats++;
                    }
                }

                for (int j = 0; j < theaterSeats[i].length; j++) {
                    System.out.printf("%-5s", theaterSeats[i][j]);
                }
                System.out.println("\n");
            }
            System.out.println("Number of seats taken: " + availableSeats);

            System.out.print("Book Another Seat? (Y/N): ");
            anotherChoice = sc.nextLine().toLowerCase();

        } while ("y".equals(anotherChoice) || "Y".equals(anotherChoice));
    }
}
