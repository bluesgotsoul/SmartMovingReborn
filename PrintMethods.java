import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.io.File;

public class PrintMethods {
    public static void main(String[] args) throws Exception {
        File f = new File("C:\\Users\\slava\\.gradle\\caches\\forge_gradle\\bundeled_repo\\net\\minecraftforge\\forge\\1.20.1-47.3.0_mapped_official_1.20.1\\forge-1.20.1-47.3.0_mapped_official_1.20.1.jar");
        URLClassLoader loader = new URLClassLoader(new URL[]{f.toURI().toURL()});
        
        System.out.println("== Entity methods ==");
        Class<?> entityClass = loader.loadClass("net.minecraft.world.entity.Entity");
        for (Method m : entityClass.getDeclaredMethods()) {
            if (m.getReturnType() == boolean.class) {
                System.out.println("Entity boolean method: " + m.getName());
            }
        }
        
        System.out.println("\n== LocalPlayer fields ==");
        Class<?> localPlayerClass = loader.loadClass("net.minecraft.client.player.LocalPlayer");
        for (Field field : localPlayerClass.getDeclaredFields()) {
            System.out.println("LocalPlayer field: " + field.getName() + " " + field.getType().getName());
        }
        
        System.out.println("\n== Player methods ==");
        Class<?> playerClass = loader.loadClass("net.minecraft.world.entity.player.Player");
        for (Method m : playerClass.getDeclaredMethods()) {
            if (m.getName().toLowerCase().contains("move") || m.getName().toLowerCase().contains("space")) {
                System.out.print("Player method: " + m.getName() + "(");
                for (Class<?> p : m.getParameterTypes()) {
                    System.out.print(p.getName() + ",");
                }
                System.out.println(")");
            }
        }
    }
}
