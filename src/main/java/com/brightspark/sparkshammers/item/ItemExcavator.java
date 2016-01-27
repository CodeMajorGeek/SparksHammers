package com.brightspark.sparkshammers.item;

import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import java.util.Set;

public class ItemExcavator extends ItemAOE
{
    private static final Set<Block> ShovelBlocks = Sets.newHashSet(new Block[]{Blocks.clay, Blocks.dirt, Blocks.farmland, Blocks.grass, Blocks.gravel, Blocks.mycelium, Blocks.sand, Blocks.snow, Blocks.snow_layer, Blocks.soul_sand});
    //These are the material types which the hammer can mine in AOE:
    private static final Set<Material> ShovelMats = Sets.newHashSet(new Material[]{Material.grass, Material.ground, Material.sand, Material.snow, Material.craftedSnow, Material.clay});
    //private static final Material[] ShovelMats = {Material.grass, Material.ground, Material.sand, Material.snow, Material.craftedSnow, Material.clay};

    /**
     * Constructor which does not set anything other than the creative tab.
     * @param mat Tool Material
     */
    public ItemExcavator(ToolMaterial mat)
    {
        super(2.0f, mat, ShovelBlocks, ShovelMats);
    }

    public ItemExcavator(String name, ToolMaterial mat)
    {
        this(name, mat, null, false);
    }

    public ItemExcavator(String name, ToolMaterial mat, String modName)
    {
        this(name, mat, modName, false);
    }

    public ItemExcavator(String name, ToolMaterial mat, String modName, boolean hasInfiniteUse)
    {
        super(name, modName, 2.0f, mat, ShovelBlocks, ShovelMats);
        setInfinite(hasInfiniteUse);
    }
}