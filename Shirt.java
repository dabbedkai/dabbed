public class Shirt {
    String design;
    String color;
    String condition;
    String shirtTag;
    int length;
    int width;

    void printDimes(){
        System.out.printf("""
                \n%s %s %s %s
                %d %d
                """, design, color, condition, shirtTag, length, width);
    }
}
