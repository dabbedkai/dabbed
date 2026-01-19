public class Laptop {
    String brand;
    int storage;
    int memory;
    int dimension;
    String model;
    String condition;
    int price;

    void printDetails(){
        System.out.printf("""
                %s %s %s
                %d %d %d %d
                """, model, condition, brand, memory, storage, dimension, price);
    }
}
