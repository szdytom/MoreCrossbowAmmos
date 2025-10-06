package morecrossbowammos.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import morecrossbowammos.MoreCrossbowAmmos;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.EntityType;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
	@Unique
	private static final Predicate<ItemStack> newProjectiles() {
		Predicate<ItemStack> predicate = stack -> isExplosiveFireworkRocket(stack);
		predicate = predicate.or(stack -> stack.isOf(Items.SNOWBALL));
		predicate = predicate.or(stack -> stack.isIn(ItemTags.EGGS));
		predicate = predicate.or(stack -> stack.isOf(Items.WIND_CHARGE));
		predicate = predicate.or(stack -> stack.isOf(Items.FIRE_CHARGE));
		return predicate;
	}

	@Unique
	private static final Predicate<ItemStack> newHeldProjectiles() {
		Predicate<ItemStack> predicate = newProjectiles();
		predicate = predicate.or(stack -> stack.isOf(Items.TRIDENT));
		return predicate;
	}

	@Unique
	private static boolean isExplosiveFireworkRocket(ItemStack stack) {
		if (!stack.isOf(Items.FIREWORK_ROCKET)) {
			return false;
		}
		return stack.get(DataComponentTypes.FIREWORKS).explosions().size() > 0;
	}

	@Inject(at = @At("RETURN"), method = "getProjectiles", cancellable = true)
	private void getProjectiles(CallbackInfoReturnable<Predicate<ItemStack>> cir) {
		cir.setReturnValue(cir.getReturnValue().or(newProjectiles()));
	}

	@Inject(at = @At("RETURN"), method = "getHeldProjectiles", cancellable = true)
	private void getHeldProjectiles(CallbackInfoReturnable<Predicate<ItemStack>> cir) {
		cir.setReturnValue(cir.getReturnValue().or(newHeldProjectiles()));
	}

	@Inject(at = @At("HEAD"), method = "createArrowEntity", cancellable = true)
	private void createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack projectileStack,
			boolean critical, CallbackInfoReturnable<ProjectileEntity> cir) {
		if (projectileStack.isOf(Items.SNOWBALL)) {
			SnowballEntity snowballEntity = new SnowballEntity(world, shooter.getX(), shooter.getEyeY() - 0.15F,
					shooter.getZ(), projectileStack);
			snowballEntity.setOwner(shooter);
			cir.setReturnValue(snowballEntity);
			return;
		}

		if (projectileStack.isIn(ItemTags.EGGS)) {
			EggEntity eggEntity = new EggEntity(world, shooter.getX(), shooter.getEyeY() - 0.15F,
					shooter.getZ(), projectileStack);
			eggEntity.setOwner(shooter);
			cir.setReturnValue(eggEntity);
			return;
		}

		if (projectileStack.isOf(Items.WIND_CHARGE)) {
			if (shooter instanceof PlayerEntity player) {
				WindChargeEntity windChargeEntity = new WindChargeEntity(player, world, player.getX(),
						player.getEyeY() - 0.15F, player.getZ());
				cir.setReturnValue(windChargeEntity);
			} else {
				WindChargeEntity windChargeEntity = new WindChargeEntity(EntityType.WIND_CHARGE, world);
				windChargeEntity.setOwner(shooter);
				windChargeEntity.setPos(shooter.getX(), shooter.getEyeY() - 0.15F, shooter.getZ());
				cir.setReturnValue(windChargeEntity);
			}
			return;
		}

		if (projectileStack.isOf(Items.FIRE_CHARGE)) {
			int explosionPower = 2;
			if (world instanceof ServerWorld serverWorld) {
				explosionPower = serverWorld.getGameRules().getInt(MoreCrossbowAmmos.CROSSBOW_FIREBALL_POWER);
			}

			// velocity is set later in CrossbowItem#shootAll
			// use Vec3d.ZERO as a placeholder here
			FireballEntity fireballEntity = new FireballEntity(world, shooter, Vec3d.ZERO, explosionPower);
			fireballEntity.setPos(shooter.getX(), shooter.getEyeY() - 0.15F, shooter.getZ());
			cir.setReturnValue(fireballEntity);
			return;
		}

		if (projectileStack.isOf(Items.TRIDENT)) {
			TridentEntity tridentEntity = new TridentEntity(world, shooter, projectileStack);
			tridentEntity.setPos(shooter.getX(), shooter.getEyeY() - 0.15F, shooter.getZ());
			cir.setReturnValue(tridentEntity);
			return;
		}
	}

	@Inject(at = @At("HEAD"), method = "getSpeed", cancellable = true)
	private static void getSpeed(ChargedProjectilesComponent stack, CallbackInfoReturnable<Float> cir) {
		if (stack.contains(Items.SNOWBALL)) {
			cir.setReturnValue(2.6F);
			return;
		}

		if (stack.getProjectiles().stream().anyMatch(itemstack -> itemstack.isIn(ItemTags.EGGS))) {
			cir.setReturnValue(2.6F);
			return;
		}

		if (stack.contains(Items.WIND_CHARGE)) {
			cir.setReturnValue(2.2F);
			return;
		}

		if (stack.contains(Items.FIRE_CHARGE)) {
			cir.setReturnValue(2.2F);
			return;
		}

		if (stack.contains(Items.TRIDENT)) {
			cir.setReturnValue(3.1F);
			return;
		}
	}

	@Inject(at = @At("HEAD"), method = "getWeaponStackDamage", cancellable = true)
	private void getWeaponStackDamage(ItemStack ammo, CallbackInfoReturnable<Integer> cir) {
		if (ammo.isOf(Items.SNOWBALL)) {
			cir.setReturnValue(1);
			return;
		}

		if (ammo.isIn(ItemTags.EGGS)) {
			cir.setReturnValue(1);
			return;
		}

		if (ammo.isOf(Items.WIND_CHARGE)) {
			cir.setReturnValue(1);
			return;
		}

		if (ammo.isOf(Items.FIRE_CHARGE)) {
			cir.setReturnValue(5);
			return;
		}

		if (ammo.isOf(Items.TRIDENT)) {
			cir.setReturnValue(3);
			return;
		}
	}
}
