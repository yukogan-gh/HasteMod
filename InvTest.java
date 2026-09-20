import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

public class InvTest {
    public static void test(InventoryMenu menu) {
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            System.out.println("Slot " + i + " -> inv index " + slot.getContainerSlot());
        }
    }
}
