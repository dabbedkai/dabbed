public class Store {
    public static void main(String[] args) {
        Shirt shirt1 = new Shirt();
        shirt1.design = "Deftones (Around the Fur)";
        shirt1.color = "Black";
        shirt1.condition = "Used and Vintage";
        shirt1.shirtTag = "Giant Tag";
        shirt1.length = 22;
        shirt1.width = 19;

        Shirt shirt2 = new Shirt();
        shirt2.design = "Metallica (And Justice for All)";
        shirt2.color = "Black";
        shirt2.condition = "Vintage";
        shirt2.shirtTag = "Official Metallica Tag";
        shirt2.length = 21;
        shirt2.width = 20;

        shirt1.printDimes();
        shirt2.printDimes();
    }
}
