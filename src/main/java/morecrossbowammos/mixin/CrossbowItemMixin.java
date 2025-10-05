package morecrossbowammos.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
	@Inject(at = @At("RETURN"), method = "getHeldProjectiles", cancellable = true)
	private void getHeldProjectiles(CallbackInfoReturnable<Predicate<ItemStack>> cir) {
		Predicate<ItemStack> original = cir.getReturnValue();
		cir.setReturnValue(original.or(stack -> stack.isOf(Items.SNOWBALL)));
	}

	@Inject(at = @At("HEAD"), method = "createArrowEntity", cancellable = true)
	private void createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack projectileStack,
			boolean critical, CallbackInfoReturnable<ProjectileEntity> cir) {
		if (projectileStack.isOf(Items.SNOWBALL)) {
			SnowballEntity snowballEntity = new SnowballEntity(world, shooter.getX(), shooter.getEyeY() - 0.15F,
					shooter.getZ(), projectileStack);
			snowballEntity.setOwner(shooter);
			cir.setReturnValue(snowballEntity);
		}
	}

	@Inject(at = @At("HEAD"), method = "getSpeed", cancellable = true)
	private static void getSpeed(ChargedProjectilesComponent stack, CallbackInfoReturnable<Float> cir) {
		if (stack.contains(Items.SNOWBALL)) {
			cir.setReturnValue(2.6F);
		}
	}

	@Inject(at = @At("HEAD"), method = "getWeaponStackDamage", cancellable = true)
	private void getWeaponStackDamage(ItemStack ammo, CallbackInfoReturnable<Integer> cir) {
		if (ammo.isOf(Items.SNOWBALL)) {
			cir.setReturnValue(1);
		}
	}
}
