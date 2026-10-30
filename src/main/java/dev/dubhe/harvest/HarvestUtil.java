package dev.dubhe.harvest;

import dev.dubhe.harvest.mixin.CropBlockAccessor;
import dev.dubhe.harvest.mixin.IntegerPropertyAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@EventBusSubscriber(modid = Harvest.MOD_ID)
public class HarvestUtil {
    public static boolean harvest(Player player, Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        BlockState newState = null;
        switch (block) {
            case CropBlock cropBlock when state.getValue(((CropBlockAccessor) cropBlock).invokeGetAgeProperty()) == cropBlock.getMaxAge() -> {
                Block.dropResources(state, level, pos, null, player, ItemStack.EMPTY);
                newState = state.setValue(((CropBlockAccessor) cropBlock).invokeGetAgeProperty(), 0);
            }
            case SweetBerryBushBlock ignored when state.getValue(SweetBerryBushBlock.AGE) >= 2 -> {
                int j = 1 + level.random.nextInt(2);
                boolean flag = state.getValue(SweetBerryBushBlock.AGE) == ((IntegerPropertyAccessor) (Object) SweetBerryBushBlock.AGE).getMax();
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
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
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
            if (stack.has(DataComponents.MAX_DAMAGE)) {
                int maxDamage = Objects.requireNonNull(stack.get(DataComponents.MAX_DAMAGE));
                max = (int) Math.floor(maxDamage * 0.1d);
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
                            stack.hurtAndBreak(1, player, event.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                            success.set(true);
                            return BlockPos.TraversalNodeStatus.ACCEPT;
                        }
                    }
                    return BlockPos.TraversalNodeStatus.SKIP;
                }
            );
        } else if (block instanceof CropBlock || block instanceof CocoaBlock) {
            success.set(HarvestUtil.harvest(player, level, pos, state));
        }
        if (success.get()) {
            if (level.isClientSide()) {
                event.setCancellationResult(InteractionResult.SUCCESS);
            } else {
                event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
            }
        }
    }
}
