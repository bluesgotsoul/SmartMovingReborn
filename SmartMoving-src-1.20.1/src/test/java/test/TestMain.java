package net.smart.moving.config;

public class TestMain {
    public static void main(String[] args) {
        try {
            SmartMovingConfig config = new SmartMovingConfig();
            config.loadFromOptionsFile(new java.io.File("smart_moving_options.txt"));
            System.out.println("Success!");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
