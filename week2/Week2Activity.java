package week2;

public class Theater {
    public static void main(String[] args) {
        int[] theaterRow = new int[8];
        theaterRow[3] = 1;
        int availableSeats = 0;
        for (int j = 0; j < theaterRow.length; j++) {
            if (theaterRow[j] == 1){
                availableSeats++;
                System.out.println("Seat " + (j+1) + " Available");
            }
                System.out.println("Seat " + (j+1) + " Unavailable");
            }
            System.out.println("Number of seats available: " + availableSeats);
    }
}
