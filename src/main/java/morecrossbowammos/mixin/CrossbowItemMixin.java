package morecrossbowammos.mixin;

import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import morecrossbowammos.MoreCrossbowAmmos;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.LingeringPotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.SplashPotionEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.stat.Stats;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
	@Shadow
	protected abstract int getWeaponStackDamage(ItemStack projectile);

	@Unique
	private static final Predicate<ItemStack> newProjectiles() {
		Predicate<ItemStack> predicate = stack -> isExplosiveFireworkRocket(stack);
		predicate = predicate.or(stack -> stack.isOf(Items.SNOWBALL));
		predicate = predicate.or(stack -> stack.isIn(ItemTags.EGGS));
		predicate = predicate.or(stack -> stack.isOf(Items.WIND_CHARGE));
		predicate = predicate.or(stack -> stack.isOf(Items.FIRE_CHARGE));
		predicate = predicate.or(stack -> stack.isOf(Items.ENDER_PEARL));
		predicate = predicate.or(stack -> stack.isOf(Items.EXPERIENCE_BOTTLE));
		predicate = predicate.or(stack -> stack.isOf(Items.SPLASH_POTION));
		predicate = predicate.or(stack -> stack.isOf(Items.LINGERING_POTION));
		predicate = predicate.or(stack -> stack.isOf(Items.TNT));
		return predicate;
	}

	@Unique
	private static final Predicate<ItemStack> newHeldProjectiles() {
		Predicate<ItemStack> predicate = newProjectiles();
		predicate = predicate.or(stack -> stack.isOf(Items.TRIDENT));
		predicate = predicate.or(stack -> stack.isOf(Items.BLAZE_POWDER));
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
				windChargeEntity.setPosition(shooter.getX(), shooter.getEyeY() - 0.15F, shooter.getZ());
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
			fireballEntity.setPosition(shooter.getX(), shooter.getEyeY() - 0.15F, shooter.getZ());
			cir.setReturnValue(fireballEntity);
			return;
		}

		if (projectileStack.isOf(Items.ENDER_PEARL)) {
			EnderPearlEntity enderPearlEntity = new EnderPearlEntity(world, shooter, projectileStack);
			enderPearlEntity.setPosition(shooter.getX(), shooter.getEyeY() - 0.15F, shooter.getZ());
			cir.setReturnValue(enderPearlEntity);
			return;
		}

		if (projectileStack.isOf(Items.EXPERIENCE_BOTTLE)) {
			ExperienceBottleEntity experienceBottleEntity = new ExperienceBottleEntity(world, shooter.getX(),
					shooter.getEyeY() - 0.15F,
					shooter.getZ(), projectileStack);
			experienceBottleEntity.setOwner(shooter);
			cir.setReturnValue(experienceBottleEntity);
			return;
		}

		if (projectileStack.isOf(Items.SPLASH_POTION)) {
			SplashPotionEntity splashPotionEntity = new SplashPotionEntity(world, shooter.getX(),
					shooter.getEyeY() - 0.15F,
					shooter.getZ(), projectileStack);
			splashPotionEntity.setOwner(shooter);
			cir.setReturnValue(splashPotionEntity);
			return;
		}

		if (projectileStack.isOf(Items.LINGERING_POTION)) {
			LingeringPotionEntity lingeringPotionEntity = new LingeringPotionEntity(world, shooter.getX(),
					shooter.getEyeY() - 0.15F,
					shooter.getZ(), projectileStack);
			lingeringPotionEntity.setOwner(shooter);
			cir.setReturnValue(lingeringPotionEntity);
			return;
		}

		if (projectileStack.isOf(Items.TRIDENT)) {
			TridentEntity tridentEntity = new TridentEntity(world, shooter, projectileStack);
			tridentEntity.setPosition(shooter.getX(), shooter.getEyeY() - 0.15F, shooter.getZ());
			cir.setReturnValue(tridentEntity);
			return;
		}

		if (projectileStack.isOf(Items.BLAZE_POWDER)) {
			SmallFireballEntity smallFireballEntity = new SmallFireballEntity(world, shooter, Vec3d.ZERO);
			smallFireballEntity.setPosition(shooter.getX(), shooter.getEyeY() - 0.15F, shooter.getZ());
			cir.setReturnValue(smallFireballEntity);
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
			cir.setReturnValue(3.1F);
			return;
		}

		if (stack.contains(Items.FIRE_CHARGE)) {
			cir.setReturnValue(3.1F);
			return;
		}

		if (stack.contains(Items.ENDER_PEARL)) {
			cir.setReturnValue(2.6F);
			return;
		}

		if (stack.contains(Items.EXPERIENCE_BOTTLE)) {
			cir.setReturnValue(2.6F);
			return;
		}

		if (stack.contains(Items.SPLASH_POTION)) {
			cir.setReturnValue(2.6F);
			return;
		}

		if (stack.contains(Items.LINGERING_POTION)) {
			cir.setReturnValue(2.6F);
			return;
		}

		if (stack.contains(Items.TNT)) {
			cir.setReturnValue(1.6F);
			return;
		}

		if (stack.contains(Items.TRIDENT)) {
			cir.setReturnValue(3.1F);
			return;
		}

		if (stack.contains(Items.BLAZE_POWDER)) {
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

		if (ammo.isOf(Items.ENDER_PEARL)) {
			cir.setReturnValue(1);
			return;
		}

		if (ammo.isOf(Items.EXPERIENCE_BOTTLE)) {
			cir.setReturnValue(1);
			return;
		}

		if (ammo.isOf(Items.SPLASH_POTION)) {
			cir.setReturnValue(1);
			return;
		}

		if (ammo.isOf(Items.LINGERING_POTION)) {
			cir.setReturnValue(1);
			return;
		}

		if (ammo.isOf(Items.TNT)) {
			cir.setReturnValue(5);
			return;
		}

		if (ammo.isOf(Items.TRIDENT)) {
			cir.setReturnValue(3);
			return;
		}

		if (ammo.isOf(Items.BLAZE_POWDER)) {
			cir.setReturnValue(3);
			return;
		}
	}

	@Inject(at = @At("TAIL"), method = "shoot")
	protected void shootFireball(LivingEntity shooter, ProjectileEntity projectile, int index, float speed,
			float divergence,
			float yaw, @Nullable LivingEntity target, CallbackInfo ci) {
		if (projectile instanceof FireballEntity) {
			// Move the fireball a bit forward so it doesn't collide with each other in a
			// multi-shot scenario
			Vec3d velocity = projectile.getVelocity().normalize().multiply(2.0);
			Vec3d pos = projectile.getPos();
			projectile.setPosition(velocity.add(pos));
		}
	}

	@Unique
	private Entity createNonProjectileEntity(World world, ItemStack stack, LivingEntity shooter) {
		if (stack.isOf(Items.TNT)) {
			TntEntity tntEntity = new TntEntity(world, shooter.getX(), shooter.getEyeY() - 0.15F, shooter.getZ(),
					shooter);
			return tntEntity;
		}
		return null;
	}

	@Unique
	protected void shootNonProjectile(LivingEntity shooter, Entity entity, int index, float speed,
			float divergence, float yaw, @Nullable LivingEntity target) {
		Vector3f vector3f;
		if (target != null) {
			double d = target.getX() - shooter.getX();
			double e = target.getZ() - shooter.getZ();
			double f = Math.sqrt(d * d + e * e);
			double g = target.getBodyY(0.3333333333333333) - entity.getY() + f * 0.2F;
			vector3f = CrossbowItem.calcVelocity(shooter, new Vec3d(d, g, e), yaw);
		} else {
			Vec3d vec3d = shooter.getOppositeRotationVector(1.0F);
			Quaternionf quaternionf = new Quaternionf().setAngleAxis((double) (yaw * (float) (Math.PI / 180.0)),
					vec3d.x, vec3d.y, vec3d.z);
			Vec3d vec3d2 = shooter.getRotationVec(1.0F);
			vector3f = vec3d2.toVector3f().rotate(quaternionf);
		}

		vector3f = vector3f.normalize().mul(speed);
		entity.setVelocity(vector3f.x(), vector3f.y(), vector3f.z());
		float h = CrossbowItem.getSoundPitch(shooter.getRandom(), index);
		shooter.getWorld().playSound(null, shooter.getX(), shooter.getY(),
				shooter.getZ(),
				SoundEvents.ITEM_CROSSBOW_SHOOT, shooter.getSoundCategory(), 1.0F, h);
	}

	@Unique
	private void shootAllNonProjectiles(ServerWorld world, LivingEntity shooter, Hand hand, ItemStack stack,
			float speed,
			float divergence, List<ItemStack> projectiles, @Nullable LivingEntity target) {
		float f = EnchantmentHelper.getProjectileSpread(world, stack, shooter, 0.0F);
		float g = projectiles.size() == 1 ? 0.0F : 2.0F * f / (projectiles.size() - 1);
		float h = (projectiles.size() - 1) % 2 * g / 2.0F;
		float i = 1.0F;

		for (int j = 0; j < projectiles.size(); j++) {
			ItemStack itemStack = projectiles.get(j);
			if (!itemStack.isEmpty()) {
				float k = h + i * ((j + 1) / 2) * g;
				i = -i;
				Entity entity = createNonProjectileEntity(world, itemStack, shooter);
				if (entity == null) {
					continue;
				}
				shootNonProjectile(shooter, entity, j, speed, divergence, k, target);
				world.spawnEntity(entity);
				stack.damage(this.getWeaponStackDamage(itemStack), shooter, LivingEntity.getSlotForHand(hand));
				if (stack.isEmpty()) {
					break;
				}
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "shootAll", cancellable = true)
	public void shootAll(World world, LivingEntity shooter, Hand hand, ItemStack stack, float speed, float divergence,
			@Nullable LivingEntity target, CallbackInfo ci) {
		if (world instanceof ServerWorld serverWorld) {
			ChargedProjectilesComponent chargedProjectiles = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
			if (chargedProjectiles == null || chargedProjectiles.isEmpty()) {
				return;
			}

			Predicate<ItemStack> isNonProjectile = itemstack -> itemstack.isOf(Items.TNT);

			if (chargedProjectiles.getProjectiles().stream().anyMatch(isNonProjectile)) {
				shootAllNonProjectiles(serverWorld, shooter, hand, stack, speed, divergence,
						chargedProjectiles.getProjectiles(), target);
				stack.set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);
				if (shooter instanceof ServerPlayerEntity serverPlayer) {
					Criteria.SHOT_CROSSBOW.trigger(serverPlayer, stack);
					serverPlayer.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
				}
				ci.cancel();
				return;
			}
		}
	}
}
