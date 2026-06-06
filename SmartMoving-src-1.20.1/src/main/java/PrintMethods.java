import net.minecraft.world.entity.Entity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class PrintMethods {
    public static void main(String[] args) {
        System.out.println("Entity methods:");
        for (Method m : Entity.class.getDeclaredMethods()) {
            if (m.getReturnType() == boolean.class) {
                System.out.println("Method: " + m.getName() + " returns boolean");
            }
        }
        System.out.println("\nPlayer methods:");
        for (Method m : Player.class.getDeclaredMethods()) {
            if (m.getName().contains("move")) {
                System.out.println("Method: " + m.getName() + " params: " + m.getParameterCount());
            }
        }
        System.out.println("\nLocalPlayer fields:");
        for (Field f : LocalPlayer.class.getDeclaredFields()) {
            System.out.println("Field: " + f.getName() + " type: " + f.getType().getName());
        }
    }
}
