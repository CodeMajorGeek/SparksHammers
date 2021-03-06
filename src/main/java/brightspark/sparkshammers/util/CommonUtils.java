package brightspark.sparkshammers.util;

import brightspark.sparkshammers.SparksHammers;
import brightspark.sparkshammers.item.ItemAOE;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import org.lwjgl.input.Keyboard;

public class CommonUtils
{
    public static boolean isCtrlKeyDown()
    {
        // prioritize CONTROL, but allow OPTION as well on Mac (note: GuiScreen's isCtrlKeyDown only checks for the OPTION key on Mac)
        boolean isCtrlKeyDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        if (!isCtrlKeyDown && Minecraft.IS_RUNNING_ON_MAC)
            isCtrlKeyDown = Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA);

        return isCtrlKeyDown;
    }

    public static boolean isShiftKeyDown()
    {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    /**
     * Capitalises the first letter of every word in the string
     */
    public static String capitaliseAllFirstLetters(String text)
    {
        String[] textArray = text.split("\\s");
        String output = "";
        for(String t : textArray)
        {
            String space = output.equals("") ? "" : " ";
            output += space + capitaliseFirstLetter(t);
        }
        return output;
    }

    /**
     * Capitalises the first letter of the string
     */
    public static String capitaliseFirstLetter(String text)
    {
        if(text == null || text.length() <= 0)
            return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    public static Item getRegisteredItem(String itemId)
    {
        return Item.REGISTRY.getObject(new ResourceLocation(itemId));
    }

    public static String getRegisteredId(Item item)
    {
        ResourceLocation id = Item.REGISTRY.getNameForObject(item);
        return id == null ? null : id.toString();
    }

    /**
     * Open a GUI for a block (Uses a guiID of -1).
     */
    public static void openGui(EntityPlayer player, World world, BlockPos pos)
    {
        player.openGui(SparksHammers.instance, -1, world, pos.getX(), pos.getY(), pos.getZ());
    }

    public static ItemStack findItemInPlayerInv(EntityPlayer player, Item item)
    {
        for(EnumHand hand : EnumHand.values())
        {
            ItemStack heldStack = player.getHeldItem(hand);
            if(heldStack.getItem() == item)
                return heldStack;
        }

        for(ItemStack stack : player.inventory.mainInventory)
            if(stack.getItem() == item)
                return stack;

        return null;
    }



    // <<<< AOE STUFF >>>>

    public static BlockPos[] getBreakArea(ItemStack hammerStack, BlockPos pos, EnumFacing sideHit, EntityPlayer player)
    {
        ItemAOE hammerItem = (ItemAOE) hammerStack.getItem();
        //Rotate if player is holding shift
        if(hammerItem.getShiftRotating() && player.isSneaking())
        {
            int tempW = hammerItem.getMineWidth(hammerStack);
            hammerItem.setMineWidth(hammerItem.getMineHeight(hammerStack));
            hammerItem.setMineHeight(tempW);
        }

        int mineWidth = hammerItem.getMineWidth(hammerStack);
        int mineHeight = hammerItem.getMineHeight(hammerStack);

        BlockPos start = pos.offset(sideHit, hammerItem.getMineDepth());
        BlockPos end = pos.offset(sideHit, hammerItem.getMineDepth());

        //Offset destroyed area if standing on ground and mining horizontally
        if(!player.capabilities.isFlying && sideHit != EnumFacing.UP && sideHit != EnumFacing.DOWN && mineHeight > 1)
        {
            start = start.up(mineHeight - 1);
            end = end.up(mineHeight - 1);
        }

        //Block destroyed, now for AOE
        switch (sideHit) {
            case DOWN:
            case UP:
                EnumFacing facing = EnumFacing.fromAngle(player.getRotationYawHead());
                switch(facing)
                {
                    case WEST:
                    case EAST:
                        start = start.add(-mineHeight, 0, -mineWidth);
                        end = end.add(mineHeight, 0, mineWidth);
                        break;
                    case NORTH:
                    case SOUTH:
                    default:
                        start = start.add(-mineWidth, 0, -mineHeight);
                        end = end.add(mineWidth, 0, mineHeight);
                        break;
                }
                break;
            case NORTH:
            case SOUTH:
                //Z axis
                start = start.add(-mineWidth, -mineHeight, 0);
                end = end.add(mineWidth, mineHeight, 0);
                break;
            case WEST:
            case EAST:
                //X axis
                start = start.add(0, -mineHeight, -mineWidth);
                end = end.add(0, mineHeight, mineWidth);
                break;
        }

        //Rotate back to normal if player is holding shift
        if(hammerItem.getShiftRotating() && player.isSneaking())
        {
            int tempW = hammerItem.getMineWidth(hammerStack);
            hammerItem.setMineWidth(hammerItem.getMineHeight(hammerStack));
            hammerItem.setMineHeight(tempW);
        }

        return new BlockPos[] {start, end};
    }

    /**
     * Used for Nether Star Hammer
     */
    public static void breakArea(ItemStack stack, World world, EntityPlayer player, float blockStrength, BlockPos posStart, BlockPos center, BlockPos posEnd)
    {
        for (int xPos = posStart.getX(); xPos <= posEnd.getX(); xPos++)
            for (int yPos = posStart.getY(); yPos <= posEnd.getY(); yPos++)
                for (int zPos = posStart.getZ(); zPos <= posEnd.getZ(); zPos++)
                {
                    BlockPos pos = new BlockPos(xPos, yPos, zPos);
                    if(breakBlock(stack, world, player, pos, blockStrength) && pos.equals(center))
                    {
                        //Play break sound at center only once
                        world.playEvent(2001, center, Block.getStateId(Blocks.STONE.getDefaultState()));
                    }
                }
    }

    /**
     * Used for the Nether Star hammer so I can pass a previously saved reference block strength to the method
     */
    public static boolean breakBlock(ItemStack stack, World world, EntityPlayer player, BlockPos blockPos, float refBlockStrength)
    {
        IBlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();

        if(!breakBlockChecks(stack, world, blockPos, blockState))
            return false;

        float strength = ForgeHooks.blockStrength(blockState, player, world, blockPos);

        // only harvestable blocks that aren't impossibly slow to harvest
        if(!ForgeHooks.canHarvestBlock(block, player, world, blockPos) || refBlockStrength / strength > 10f) return false;

        breakBlockAction(stack, world, player, blockPos, block, blockState);
        return true;
    }

    public static void breakBlock(ItemStack stack, World world, EntityPlayer player, BlockPos blockPos, BlockPos refBlockPos)
    {
        IBlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();

        if(!breakBlockChecks(stack, world, blockPos, blockState))
            return;

        IBlockState refBlockState = world.getBlockState(refBlockPos);
        float refStrength = ForgeHooks.blockStrength(refBlockState, player, world, refBlockPos);
        float strength = ForgeHooks.blockStrength(blockState, player, world, blockPos);

        // only harvestable blocks that aren't impossibly slow to harvest
        if(!ForgeHooks.canHarvestBlock(block, player, world, blockPos) || refStrength / strength > 10f) return;

        breakBlockAction(stack, world, player, blockPos, block, blockState);
    }

    private static boolean breakBlockChecks(ItemStack stack, World world, BlockPos blockPos, IBlockState block)
    {
        // prevent calling that stuff for air blocks, could lead to unexpected behaviour since it fires events
        if(world.isAirBlock(blockPos)) return false;

        // check if the block can be broken, since extra block breaks shouldn't instantly break stuff like obsidian
        // or precious ores you can't harvest while mining stone
        // only effective materials
        if(!((ItemAOE)stack.getItem()).isEffective(stack, block)) return false;

        return true;
    }

    private static void breakBlockAction(ItemStack stack, World world, EntityPlayer player, BlockPos blockPos, Block block, IBlockState blockState)
    {
        // From this point on it's clear that the player CAN break the block

        if(player.capabilities.isCreativeMode)
        {
            block.onBlockHarvested(world, blockPos, blockState, player);
            if(block.removedByPlayer(blockState, world, blockPos, player, false))
                block.onBlockDestroyedByPlayer(world, blockPos, blockState);

            // send update to client
            if(!world.isRemote)
                ((EntityPlayerMP)player).connection.sendPacket(new SPacketBlockChange(world, blockPos));
            return;
        }

        // callback to the tool the player uses. Called on both sides. This damages the tool n stuff.
        stack.onBlockDestroyed(world, blockState, blockPos, player);

        // server sided handling
        if(!world.isRemote)
        {
            // send the blockbreak event
            int xp = ForgeHooks.onBlockBreakEvent(world, ((EntityPlayerMP) player).interactionManager.getGameType(), (EntityPlayerMP) player, blockPos);
            if(xp == -1) {
                return;
            }

            // serverside we reproduce ItemInWorldManager.tryHarvestBlock

            TileEntity te = world.getTileEntity(blockPos);
            // ItemInWorldManager.removeBlock
            if(block.removedByPlayer(blockState, world, blockPos, player, true)) // boolean is if block can be harvested, checked above
            {
                block.onBlockDestroyedByPlayer(world, blockPos, blockState);
                block.harvestBlock(world, player, blockPos, blockState, te, stack);
                block.dropXpOnBlockBreak(world, blockPos, xp);
            }

            // always send block update to client
            ((EntityPlayerMP)player).connection.sendPacket(new SPacketBlockChange(world, blockPos));
        }
        // client sided handling
        else
        {
            // clientside we do a "this block has been clicked on long enough to be broken" call. This should not send any new packets
            // the code above, executed on the server, sends a block-updates that give us the correct state of the block we destroy.

            // following code can be found in PlayerControllerMP.onPlayerDestroyBlock
            //world.playEvent(2001, blockPos, Block.getStateId(blockState));
            if(block.removedByPlayer(blockState, world, blockPos, player, true))
                block.onBlockDestroyedByPlayer(world, blockPos, blockState);
            // callback to the tool
            stack.onBlockDestroyed(world, blockState, blockPos, player);

            if(stack.getCount() == 0 && stack == player.getHeldItemMainhand())
            {
                ForgeEventFactory.onPlayerDestroyItem(player, stack, EnumHand.MAIN_HAND);
                player.setHeldItem(EnumHand.MAIN_HAND, null);
            }

            // send an update to the server, so we get an update back
            Minecraft.getMinecraft().getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, blockPos, Minecraft.getMinecraft().objectMouseOver.sideHit));
        }
    }
}
