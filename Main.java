public class Main{
    
    public static void main(String[] args) {
        Laptop myLaptop = new Laptop();
        myLaptop.brand = "Rog";
        myLaptop.storage = 444;
        myLaptop.memory = 222;
        myLaptop.dimension = 20;
        myLaptop.model = "ROG STRIX";
        myLaptop.condition = 3;
        myLaptop.price = 70000;

        myLaptop.printDetails();
    }
}