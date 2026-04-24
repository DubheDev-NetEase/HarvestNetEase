package dev.dubhe.netease.harvest.util;

import dev.dubhe.netease.harvest.Harvest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = Harvest.MODID)
public class HarvestUtil {
    public static boolean harvest(Player player, Level level, BlockPos pos, @NotNull BlockState state) {
        Block block = state.getBlock();
        BlockState newState = null;
        switch (block) {
            case CropBlock cropBlock when state.getValue(cropBlock.getAgeProperty()) == cropBlock.getMaxAge() -> {
                Block.dropResources(state, level, pos, null, player, ItemStack.EMPTY);
                newState = state.setValue(cropBlock.getAgeProperty(), 0);
            }
            case SweetBerryBushBlock ignored when state.getValue(SweetBerryBushBlock.AGE) >= 2 -> {
                int j = 1 + level.random.nextInt(2);
                boolean flag = state.getValue(SweetBerryBushBlock.AGE) == SweetBerryBushBlock.AGE.max;
                Block.popResource(level, pos, new ItemStack(Items.SWEET_BERRIES, j + (flag ? 1 : 0)));
                newState = state.setValue(SweetBerryBushBlock.AGE, 1);
            }
            case CocoaBlock ignored when state.getValue(CocoaBlock.AGE) == 2 -> {
                Block.dropResources(state, level, pos, null, player, ItemStack.EMPTY);
                newState = state.setValue(CocoaBlock.AGE, 1);
            }
            default -> {
            }
        }
        if (newState == null) return false;
        level.setBlockAndUpdate(pos, newState);
        return true;
    }

    private static final Direction[] ALL_DIRECTIONS = Direction.values();

    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.@NotNull RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        ItemStack stack = event.getItemStack();
        AtomicBoolean success = new AtomicBoolean(false);
        if (stack.is(ItemTags.HOES) && (
            block instanceof CropBlock || block instanceof SweetBerryBushBlock || block instanceof CocoaBlock
        )) {
            int max = 5;
            if (stack.getItem() instanceof TieredItem item) {
                max = (int) Math.floor(item.getTier().getUses() * 0.1d);
            }
            BlockPos.breadthFirstTraversal(
                pos, Integer.MAX_VALUE, max, (posx, consumer) -> {
                    for (Direction direction : ALL_DIRECTIONS) {
                        consumer.accept(posx.relative(direction));
                    }
                }, (checkedPos) -> {
                    BlockState checkedState = level.getBlockState(checkedPos);
                    Block checkedBlock = checkedState.getBlock();
                    if (checkedBlock instanceof CropBlock || checkedBlock instanceof SweetBerryBushBlock || checkedBlock instanceof CocoaBlock) {
                        if (HarvestUtil.harvest(player, level, checkedPos, checkedState)) {
                            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
                            success.set(true);
                            return true;
                        }
                    }
                    return false;
                }
            );
        } else if (block instanceof CropBlock || block instanceof CocoaBlock) {
            success.set(HarvestUtil.harvest(player, level, pos, state));
        }
        if (success.get()) {
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        }
    }
}
