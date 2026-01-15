public class Laptop {
    String brand;
    double storage;
    double memory;
    double dimension;
    String model;
    double condition;
    double price;

    void printDetails(){
        System.out.printf("""
                %s %s %s
                %d %d %d %d
                """, model, condition, brand, memory, storage, dimension, price);
    }
}
