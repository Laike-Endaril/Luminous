package net.minecraft.world.chunk;

import com.fantasticsource.luminous.Luminous;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkGeneratorDebug;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Chunk implements net.minecraftforge.common.capabilities.ICapabilityProvider
{
    private static final Logger LOGGER = LogManager.getLogger();
    public static final ExtendedBlockStorage NULL_BLOCK_STORAGE = null;
    private final ExtendedBlockStorage[] storageArrays;
    private final byte[] blockBiomeArray;
    private final int[] precipitationHeightMap;
    private final boolean[] updateSkylightColumns;
    private boolean loaded;
    private final World world;
    private final int[] heightMap;
    public final int x;
    public final int z;
    private boolean isGapLightingUpdated;
    private final Map<BlockPos, TileEntity> tileEntities;
    private final ClassInheritanceMultiMap<Entity>[] entityLists;
    private boolean isTerrainPopulated;
    private boolean isLightPopulated;
    private boolean ticked;
    private boolean dirty;
    private boolean hasEntities;
    private long lastSaveTime;
    private int heightMapMinimum;
    private long inhabitedTime;
    private int queuedLightChecks;
    private final ConcurrentLinkedQueue<BlockPos> tileEntityPosQueue;
    public boolean unloadQueued;

    public Chunk(World worldIn, int x, int z)
    {
        storageArrays = new ExtendedBlockStorage[16];
        blockBiomeArray = new byte[256];
        precipitationHeightMap = new int[256];
        updateSkylightColumns = new boolean[256];
        tileEntities = Maps.newHashMap();
        queuedLightChecks = 4096;
        tileEntityPosQueue = Queues.newConcurrentLinkedQueue();
        entityLists = (ClassInheritanceMultiMap[]) (new ClassInheritanceMultiMap[16]);
        world = worldIn;
        this.x = x;
        this.z = z;
        heightMap = new int[256];

        for (int i = 0; i < entityLists.length; ++i)
        {
            entityLists[i] = new ClassInheritanceMultiMap(Entity.class);
        }

        Arrays.fill(precipitationHeightMap, -999);
        Arrays.fill(blockBiomeArray, (byte) -1);
        capabilities = net.minecraftforge.event.ForgeEventFactory.gatherCapabilities(this);
    }

    public Chunk(World worldIn, ChunkPrimer primer, int x, int z)
    {
        this(worldIn, x, z);
        boolean flag = worldIn.provider.hasSkyLight();

        for (int j = 0; j < 16; ++j)
        {
            for (int k = 0; k < 16; ++k)
            {
                for (int l = 0; l < 256; ++l)
                {
                    IBlockState iblockstate = primer.getBlockState(j, l, k);

                    if (iblockstate.getMaterial() != Material.AIR)
                    {
                        int i1 = l >> 4;

                        if (storageArrays[i1] == NULL_BLOCK_STORAGE)
                        {
                            storageArrays[i1] = new ExtendedBlockStorage(i1 << 4, flag);
                        }

                        storageArrays[i1].set(j, l & 15, k, iblockstate);
                    }
                }
            }
        }
    }

    public boolean isAtLocation(int x, int z)
    {
        return x == this.x && z == this.z;
    }

    public int getHeight(BlockPos pos)
    {
        return getHeightValue(pos.getX() & 15, pos.getZ() & 15);
    }

    public int getHeightValue(int x, int z)
    {
        return heightMap[z << 4 | x];
    }

    @Nullable
    private ExtendedBlockStorage getLastExtendedBlockStorage()
    {
        for (int i = storageArrays.length - 1; i >= 0; --i)
        {
            if (storageArrays[i] != NULL_BLOCK_STORAGE) return storageArrays[i];
        }
        return null;
    }

    public int getTopFilledSegment()
    {
        ExtendedBlockStorage extendedblockstorage = getLastExtendedBlockStorage();
        return extendedblockstorage == null ? 0 : extendedblockstorage.getYLocation();
    }

    public ExtendedBlockStorage[] getBlockStorageArray()
    {
        return storageArrays;
    }

    @SideOnly(Side.CLIENT)
    protected void generateHeightMap()
    {
        int i = getTopFilledSegment();
        heightMapMinimum = Integer.MAX_VALUE;

        for (int j = 0; j < 16; ++j)
        {
            for (int k = 0; k < 16; ++k)
            {
                precipitationHeightMap[j + (k << 4)] = -999;

                for (int l = i + 16; l > 0; --l)
                {
                    if (getBlockLightOpacity(j, l - 1, k) != 0)
                    {
                        heightMap[k << 4 | j] = l;
                        if (l < heightMapMinimum) heightMapMinimum = l;
                        break;
                    }
                }
            }
        }

        dirty = true;
    }

    public void generateSkylightMap()
    {
        int i = getTopFilledSegment();
        heightMapMinimum = Integer.MAX_VALUE;

        for (int j = 0; j < 16; ++j)
        {
            for (int k = 0; k < 16; ++k)
            {
                precipitationHeightMap[j + (k << 4)] = -999;

                for (int l = i + 16; l > 0; --l)
                {
                    if (getBlockLightOpacity(j, l - 1, k) != 0)
                    {
                        heightMap[k << 4 | j] = l;

                        if (l < heightMapMinimum)
                        {
                            heightMapMinimum = l;
                        }

                        break;
                    }
                }

                if (world.provider.hasSkyLight())
                {
                    int k1 = 15;
                    int i1 = i + 16 - 1;

                    do
                    {
                        int j1 = getBlockLightOpacity(j, i1, k);

                        if (j1 == 0 && k1 != 15) j1 = 1;
                        k1 -= j1;

                        if (k1 > 0)
                        {
                            ExtendedBlockStorage extendedblockstorage = storageArrays[i1 >> 4];

                            if (extendedblockstorage != NULL_BLOCK_STORAGE)
                            {
                                extendedblockstorage.setSkyLight(j, i1 & 15, k, k1);
                                world.notifyLightSet(new BlockPos((this.x << 4) + j, i1, (this.z << 4) + k));
                            }
                        }

                        --i1;
                    }
                    while (i1 > 0 && k1 > 0);
                }
            }
        }

        dirty = true;
    }

    private void propagateSkylightOcclusion(int x, int z)
    {
        updateSkylightColumns[x + z * 16] = true;
        isGapLightingUpdated = true;
    }

    private void recheckGaps(boolean onlyOne)
    {
        world.profiler.startSection("recheckGaps");

        if (world.isAreaLoaded(new BlockPos(this.x * 16 + 8, 0, this.z * 16 + 8), 16))
        {
            for (int i = 0; i < 16; ++i)
            {
                for (int j = 0; j < 16; ++j)
                {
                    if (updateSkylightColumns[i + j * 16])
                    {
                        updateSkylightColumns[i + j * 16] = false;
                        int k = getHeightValue(i, j);
                        int l = this.x * 16 + i;
                        int i1 = this.z * 16 + j;
                        int j1 = Integer.MAX_VALUE;

                        for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL)
                        {
                            j1 = Math.min(j1, world.getChunksLowestHorizon(l + enumfacing.getFrontOffsetX(), i1 + enumfacing.getFrontOffsetZ()));
                        }

                        checkSkylightNeighborHeight(l, i1, j1);

                        for (EnumFacing enumfacing1 : EnumFacing.Plane.HORIZONTAL)
                        {
                            checkSkylightNeighborHeight(l + enumfacing1.getFrontOffsetX(), i1 + enumfacing1.getFrontOffsetZ(), k);
                        }

                        if (onlyOne)
                        {
                            world.profiler.endSection();
                            return;
                        }
                    }
                }
            }

            isGapLightingUpdated = false;
        }

        world.profiler.endSection();
    }

    private void checkSkylightNeighborHeight(int x, int z, int maxValue)
    {
        int i = world.getHeight(new BlockPos(x, 0, z)).getY();

        if (i > maxValue)
        {
            updateSkylightNeighborHeight(x, z, maxValue, i + 1);
        }
        else if (i < maxValue)
        {
            updateSkylightNeighborHeight(x, z, i, maxValue + 1);
        }
    }

    private void updateSkylightNeighborHeight(int x, int z, int startY, int endY)
    {
        if (endY > startY && world.isAreaLoaded(new BlockPos(x, 0, z), 16))
        {
            for (int i = startY; i < endY; ++i)
            {
                world.checkLightFor(EnumSkyBlock.SKY, new BlockPos(x, i, z));
            }

            dirty = true;
        }
    }

    private void relightBlock(int x, int y, int z)
    {
        int i = heightMap[z << 4 | x] & 255;
        int j = i;

        if (y > i) j = y;

        while (j > 0 && getBlockLightOpacity(x, j - 1, z) == 0) --j;

        if (j != i)
        {
            world.markBlocksDirtyVertical(x + this.x * 16, z + this.z * 16, j, i);
            heightMap[z << 4 | x] = j;
            int k = this.x * 16 + x;
            int l = this.z * 16 + z;

            if (world.provider.hasSkyLight())
            {
                if (j < i)
                {
                    for (int j1 = j; j1 < i; ++j1)
                    {
                        ExtendedBlockStorage extendedblockstorage2 = storageArrays[j1 >> 4];

                        if (extendedblockstorage2 != NULL_BLOCK_STORAGE)
                        {
                            extendedblockstorage2.setSkyLight(x, j1 & 15, z, 15);
                            world.notifyLightSet(new BlockPos((this.x << 4) + x, j1, (this.z << 4) + z));
                        }
                    }
                }
                else
                {
                    for (int i1 = i; i1 < j; ++i1)
                    {
                        ExtendedBlockStorage extendedblockstorage = storageArrays[i1 >> 4];

                        if (extendedblockstorage != NULL_BLOCK_STORAGE)
                        {
                            extendedblockstorage.setSkyLight(x, i1 & 15, z, 0);
                            world.notifyLightSet(new BlockPos((this.x << 4) + x, i1, (this.z << 4) + z));
                        }
                    }
                }

                int k1 = 15;

                while (j > 0 && k1 > 0)
                {
                    --j;
                    int i2 = getBlockLightOpacity(x, j, z);

                    if (i2 == 0)
                    {
                        i2 = 1;
                    }

                    k1 -= i2;

                    if (k1 < 0)
                    {
                        k1 = 0;
                    }

                    ExtendedBlockStorage extendedblockstorage1 = storageArrays[j >> 4];

                    if (extendedblockstorage1 != NULL_BLOCK_STORAGE)
                    {
                        extendedblockstorage1.setSkyLight(x, j & 15, z, k1);
                    }
                }
            }

            int l1 = heightMap[z << 4 | x];
            int j2 = i;
            int k2 = l1;

            if (l1 < i)
            {
                j2 = l1;
                k2 = i;
            }

            if (l1 < heightMapMinimum)
            {
                heightMapMinimum = l1;
            }

            if (world.provider.hasSkyLight())
            {
                for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL)
                {
                    updateSkylightNeighborHeight(k + enumfacing.getFrontOffsetX(), l + enumfacing.getFrontOffsetZ(), j2, k2);
                }

                updateSkylightNeighborHeight(k, l, j2, k2);
            }

            dirty = true;
        }
    }

    public int getBlockLightOpacity(BlockPos pos)
    {
        return getBlockState(pos).getLightOpacity(world, pos);
    }

    private int getBlockLightOpacity(int x, int y, int z)
    {
        IBlockState state = getBlockState(x, y, z);
        return !loaded ? state.getLightOpacity() : state.getLightOpacity(world, new BlockPos(this.x << 4 | x & 15, y, this.z << 4 | z & 15));
    }

    public IBlockState getBlockState(BlockPos pos)
    {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    public IBlockState getBlockState(final int x, final int y, final int z)
    {
        if (world.getWorldType() == WorldType.DEBUG_ALL_BLOCK_STATES)
        {
            IBlockState iblockstate = null;

            if (y == 60)
            {
                iblockstate = Blocks.BARRIER.getDefaultState();
            }

            if (y == 70)
            {
                iblockstate = ChunkGeneratorDebug.getBlockStateFor(x, z);
            }

            return iblockstate == null ? Blocks.AIR.getDefaultState() : iblockstate;
        }
        else
        {
            try
            {
                if (y >= 0 && y >> 4 < storageArrays.length)
                {
                    ExtendedBlockStorage extendedblockstorage = storageArrays[y >> 4];

                    if (extendedblockstorage != NULL_BLOCK_STORAGE)
                    {
                        return extendedblockstorage.get(x & 15, y & 15, z & 15);
                    }
                }

                return Blocks.AIR.getDefaultState();
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Getting block state");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being got");
                crashreportcategory.addDetail("Location", () -> CrashReportCategory.getCoordinateInfo(x, y, z));
                throw new ReportedException(crashreport);
            }
        }
    }

    @Nullable
    public IBlockState setBlockState(BlockPos pos, IBlockState state)
    {
        int i = pos.getX() & 15;
        int j = pos.getY();
        int k = pos.getZ() & 15;
        int l = k << 4 | i;

        if (j >= precipitationHeightMap[l] - 1)
        {
            precipitationHeightMap[l] = -999;
        }

        int i1 = heightMap[l];
        IBlockState iblockstate = getBlockState(pos);

        if (iblockstate == state) return null;


        Block block = state.getBlock();
        Block block1 = iblockstate.getBlock();
        int k1 = iblockstate.getLightOpacity(world, pos);
        ExtendedBlockStorage extendedblockstorage = storageArrays[j >> 4];
        boolean flag = false;

        if (extendedblockstorage == NULL_BLOCK_STORAGE)
        {
            if (block == Blocks.AIR) return null;

            extendedblockstorage = new ExtendedBlockStorage(j >> 4 << 4, world.provider.hasSkyLight());
            storageArrays[j >> 4] = extendedblockstorage;
            flag = j >= i1;
        }

        extendedblockstorage.set(i, j & 15, k, state);

        if (!world.isRemote)
        {
            if (block1 != block) block1.breakBlock(world, pos, iblockstate);
            TileEntity te = getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
            if (te != null && te.shouldRefresh(world, pos, iblockstate, state)) world.removeTileEntity(pos);
        }
        else if (block1.hasTileEntity(iblockstate))
        {
            TileEntity te = getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
            if (te != null && te.shouldRefresh(world, pos, iblockstate, state)) world.removeTileEntity(pos);
        }

        if (extendedblockstorage.get(i, j & 15, k).getBlock() != block) return null;


        if (flag) generateSkylightMap();
        else
        {
            int j1 = state.getLightOpacity(world, pos);

            if (j1 > 0)
            {
                if (j >= i1) relightBlock(i, j + 1, k);
            }
            else if (j == i1 - 1) relightBlock(i, j, k);

            if (j1 != k1 && (j1 < k1 || getLightFor(EnumSkyBlock.SKY, pos) > 0 || getLightFor(EnumSkyBlock.BLOCK, pos) > 0))
            {
                propagateSkylightOcclusion(i, k);
            }
        }

        if (!world.isRemote && block1 != block && (!world.captureBlockSnapshots || block.hasTileEntity(state)))
        {
            block.onBlockAdded(world, pos, state);
        }

        if (block.hasTileEntity(state))
        {
            TileEntity tileentity1 = getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);

            if (tileentity1 == null)
            {
                tileentity1 = block.createTileEntity(world, state);
                world.setTileEntity(pos, tileentity1);
            }

            if (tileentity1 != null)
            {
                tileentity1.updateContainingBlockInfo();
            }
        }

        dirty = true;
        return iblockstate;
    }

    public int getLightFor(EnumSkyBlock type, BlockPos pos)
    {
        int i = pos.getX() & 15;
        int j = pos.getY();
        int k = pos.getZ() & 15;
        ExtendedBlockStorage extendedblockstorage = storageArrays[j >> 4];

        int result;
        if (extendedblockstorage == NULL_BLOCK_STORAGE) result = canSeeSky(pos) ? type.defaultLightValue : 0;
        else if (type == EnumSkyBlock.SKY) result = !world.provider.hasSkyLight() ? 0 : extendedblockstorage.getSkyLight(i, j & 15, k);
        else result = type == EnumSkyBlock.BLOCK ? extendedblockstorage.getBlockLight(i, j & 15, k) : type.defaultLightValue;

        return Luminous.getLightLevel(pos, type, result);
    }

    public void setLightFor(EnumSkyBlock type, BlockPos pos, int value)
    {
        int i = pos.getX() & 15;
        int j = pos.getY();
        int k = pos.getZ() & 15;
        ExtendedBlockStorage extendedblockstorage = storageArrays[j >> 4];

        if (extendedblockstorage == NULL_BLOCK_STORAGE)
        {
            extendedblockstorage = new ExtendedBlockStorage(j >> 4 << 4, world.provider.hasSkyLight());
            storageArrays[j >> 4] = extendedblockstorage;
            generateSkylightMap();
        }

        dirty = true;

        if (type == EnumSkyBlock.SKY)
        {
            if (world.provider.hasSkyLight())
            {
                extendedblockstorage.setSkyLight(i, j & 15, k, value);
            }
        }
        else if (type == EnumSkyBlock.BLOCK)
        {
            extendedblockstorage.setBlockLight(i, j & 15, k, value);
        }
    }

    public int getLightSubtracted(BlockPos pos, int amount)
    {
        int i = pos.getX() & 15;
        int j = pos.getY();
        int k = pos.getZ() & 15;
        ExtendedBlockStorage extendedblockstorage = storageArrays[j >> 4];

        if (extendedblockstorage == NULL_BLOCK_STORAGE)
        {
            return world.provider.hasSkyLight() && amount < EnumSkyBlock.SKY.defaultLightValue ? EnumSkyBlock.SKY.defaultLightValue - amount : 0;
        }

        int blockLight = extendedblockstorage.getBlockLight(i, j & 15, k);
        int skyLight = !world.provider.hasSkyLight() ? 0 : extendedblockstorage.getSkyLight(i, j & 15, k) - amount;

        blockLight = Luminous.getLightLevel(pos, EnumSkyBlock.BLOCK, blockLight);
        skyLight = Luminous.getLightLevel(pos, EnumSkyBlock.SKY, skyLight);

        return blockLight > skyLight ? blockLight : skyLight;
    }

    public void addEntity(Entity entityIn)
    {
        hasEntities = true;
        int i = MathHelper.floor(entityIn.posX / 16);
        int j = MathHelper.floor(entityIn.posZ / 16);

        if (i != this.x || j != this.z)
        {
            LOGGER.warn("Wrong location! ({}, {}) should be ({}, {}), {}", i, j, this.x, this.z, entityIn);
            entityIn.setDead();
        }

        int k = MathHelper.floor(entityIn.posY / 16);
        if (k < 0) k = 0;
        if (k >= entityLists.length) k = entityLists.length - 1;

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.EntityEvent.EnteringChunk(entityIn, this.x, this.z, entityIn.chunkCoordX, entityIn.chunkCoordZ));
        entityIn.addedToChunk = true;
        entityIn.chunkCoordX = this.x;
        entityIn.chunkCoordY = k;
        entityIn.chunkCoordZ = this.z;
        entityLists[k].add(entityIn);
        markDirty(); // Forge - ensure chunks are marked to save after an entity add
    }

    public void removeEntity(Entity entityIn)
    {
        removeEntityAtIndex(entityIn, entityIn.chunkCoordY);
    }

    public void removeEntityAtIndex(Entity entityIn, int index)
    {
        if (index < 0) index = 0;
        if (index >= entityLists.length) index = entityLists.length - 1;

        entityLists[index].remove(entityIn);
        markDirty();
    }

    public boolean canSeeSky(BlockPos pos)
    {
        int i = pos.getX() & 15;
        int j = pos.getY();
        int k = pos.getZ() & 15;
        return j >= heightMap[k << 4 | i];
    }

    @Nullable
    private TileEntity createNewTileEntity(BlockPos pos)
    {
        IBlockState iblockstate = getBlockState(pos);
        Block block = iblockstate.getBlock();
        return !block.hasTileEntity(iblockstate) ? null : block.createTileEntity(world, iblockstate);
    }

    @Nullable
    public TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType p_177424_2_)
    {
        TileEntity tileentity = tileEntities.get(pos);

        if (tileentity != null && tileentity.isInvalid())
        {
            tileEntities.remove(pos);
            tileentity = null;
        }

        if (tileentity == null)
        {
            if (p_177424_2_ == Chunk.EnumCreateEntityType.IMMEDIATE)
            {
                tileentity = createNewTileEntity(pos);
                world.setTileEntity(pos, tileentity);
            }
            else if (p_177424_2_ == Chunk.EnumCreateEntityType.QUEUED)
            {
                tileEntityPosQueue.add(pos.toImmutable());
            }
        }

        return tileentity;
    }

    public void addTileEntity(TileEntity tileEntityIn)
    {
        addTileEntity(tileEntityIn.getPos(), tileEntityIn);
        if (loaded) world.addTileEntity(tileEntityIn);
    }

    public void addTileEntity(BlockPos pos, TileEntity tileEntityIn)
    {
        if (tileEntityIn.getWorld() != world) tileEntityIn.setWorld(world);
        tileEntityIn.setPos(pos);

        if (getBlockState(pos).getBlock().hasTileEntity(getBlockState(pos)))
        {
            if (tileEntities.containsKey(pos)) tileEntities.get(pos).invalidate();

            tileEntityIn.validate();
            tileEntities.put(pos, tileEntityIn);
        }
    }

    public void removeTileEntity(BlockPos pos)
    {
        if (loaded)
        {
            TileEntity tileentity = tileEntities.remove(pos);
            if (tileentity != null) tileentity.invalidate();
        }
    }

    public void onLoad()
    {
        loaded = true;
        world.addTileEntities(tileEntities.values());

        for (ClassInheritanceMultiMap<Entity> classinheritancemultimap : entityLists)
        {
            world.loadEntities(com.google.common.collect.ImmutableList.copyOf(classinheritancemultimap));
        }
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(this));
    }

    public void onUnload()
    {
        java.util.Arrays.stream(entityLists).forEach(multimap -> com.google.common.collect.Lists.newArrayList(multimap.getByClass(net.minecraft.entity.player.EntityPlayer.class)).forEach(player -> world.updateEntityWithOptionalForce(player, false)));
        loaded = false;

        for (TileEntity tileentity : tileEntities.values())
        {
            world.markTileEntityForRemoval(tileentity);
        }

        for (ClassInheritanceMultiMap<Entity> classinheritancemultimap : entityLists)
        {
            world.unloadEntities(classinheritancemultimap);
        }
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Unload(this));
    }

    public void markDirty()
    {
        dirty = true;
    }

    public void getEntitiesWithinAABBForEntity(@Nullable Entity entityIn, AxisAlignedBB aabb, List<Entity> listToFill, Predicate<? super Entity> filter)
    {
        int i = MathHelper.floor((aabb.minY - World.MAX_ENTITY_RADIUS) / 16);
        int j = MathHelper.floor((aabb.maxY + World.MAX_ENTITY_RADIUS) / 16);
        i = MathHelper.clamp(i, 0, entityLists.length - 1);
        j = MathHelper.clamp(j, 0, entityLists.length - 1);

        for (int k = i; k <= j; ++k)
        {
            if (!entityLists[k].isEmpty())
            {
                for (Entity entity : entityLists[k])
                {
                    if (entity.getEntityBoundingBox().intersects(aabb) && entity != entityIn)
                    {
                        if (filter == null || filter.apply(entity)) listToFill.add(entity);

                        Entity[] entities = entity.getParts();
                        if (entities != null)
                        {
                            for (Entity entity1 : entities)
                            {
                                if (entity1 != entityIn && entity1.getEntityBoundingBox().intersects(aabb) && (filter == null || filter.apply(entity1)))
                                {
                                    listToFill.add(entity1);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public <T extends Entity> void getEntitiesOfTypeWithinAABB(Class<? extends T> entityClass, AxisAlignedBB aabb, List<T> listToFill, Predicate<? super T> filter)
    {
        int i = MathHelper.floor((aabb.minY - World.MAX_ENTITY_RADIUS) / 16.0D);
        int j = MathHelper.floor((aabb.maxY + World.MAX_ENTITY_RADIUS) / 16.0D);
        i = MathHelper.clamp(i, 0, entityLists.length - 1);
        j = MathHelper.clamp(j, 0, entityLists.length - 1);

        for (int k = i; k <= j; ++k)
        {
            for (T t : entityLists[k].getByClass(entityClass))
            {
                if (t.getEntityBoundingBox().intersects(aabb) && (filter == null || filter.apply(t))) listToFill.add(t);
            }
        }
    }

    public boolean needsSaving(boolean p_76601_1_)
    {
        if (p_76601_1_)
        {
            if (hasEntities && world.getTotalWorldTime() != lastSaveTime || dirty) return true;
        }
        else if (hasEntities && world.getTotalWorldTime() >= lastSaveTime + 600) return true;

        return dirty;
    }

    public Random getRandomWithSeed(long seed)
    {
        return new Random(world.getSeed() + (long) (this.x * this.x * 4987142) + (long) (this.x * 5947611) + (long) (this.z * this.z) * 4392871L + (long) (this.z * 389711) ^ seed);
    }

    public boolean isEmpty()
    {
        return false;
    }

    public void populate(IChunkProvider chunkProvider, IChunkGenerator chunkGenrator)
    {
        Chunk chunk = chunkProvider.getLoadedChunk(this.x, this.z - 1);
        Chunk chunk1 = chunkProvider.getLoadedChunk(this.x + 1, this.z);
        Chunk chunk2 = chunkProvider.getLoadedChunk(this.x, this.z + 1);
        Chunk chunk3 = chunkProvider.getLoadedChunk(this.x - 1, this.z);

        if (chunk1 != null && chunk2 != null && chunkProvider.getLoadedChunk(this.x + 1, this.z + 1) != null)
        {
            populate(chunkGenrator);
        }

        if (chunk3 != null && chunk2 != null && chunkProvider.getLoadedChunk(this.x - 1, this.z + 1) != null)
        {
            chunk3.populate(chunkGenrator);
        }

        if (chunk != null && chunk1 != null && chunkProvider.getLoadedChunk(this.x + 1, this.z - 1) != null)
        {
            chunk.populate(chunkGenrator);
        }

        if (chunk != null && chunk3 != null)
        {
            Chunk chunk4 = chunkProvider.getLoadedChunk(this.x - 1, this.z - 1);
            if (chunk4 != null) chunk4.populate(chunkGenrator);
        }
    }

    protected void populate(IChunkGenerator generator)
    {
        if (populating != null && net.minecraftforge.common.ForgeModContainer.logCascadingWorldGeneration) logCascadingWorldGeneration();
        ChunkPos prev = populating;
        populating = getPos();
        if (isTerrainPopulated())
        {
            if (generator.generateStructures(this, this.x, this.z)) markDirty();
        }
        else
        {
            checkLight();
            generator.populate(this.x, this.z);
            net.minecraftforge.fml.common.registry.GameRegistry.generateWorld(this.x, this.z, world, generator, world.getChunkProvider());
            markDirty();
        }
        populating = prev;
    }

    public BlockPos getPrecipitationHeight(BlockPos pos)
    {
        int i = pos.getX() & 15;
        int j = pos.getZ() & 15;
        int k = i | j << 4;
        BlockPos blockpos = new BlockPos(pos.getX(), precipitationHeightMap[k], pos.getZ());

        if (blockpos.getY() == -999)
        {
            int l = getTopFilledSegment() + 15;
            blockpos = new BlockPos(pos.getX(), l, pos.getZ());
            int i1 = -1;

            while (blockpos.getY() > 0 && i1 == -1)
            {
                IBlockState iblockstate = getBlockState(blockpos);
                Material material = iblockstate.getMaterial();

                if (!material.blocksMovement() && !material.isLiquid()) blockpos = blockpos.down();
                else i1 = blockpos.getY() + 1;
            }

            precipitationHeightMap[k] = i1;
        }

        return new BlockPos(pos.getX(), precipitationHeightMap[k], pos.getZ());
    }

    public void onTick(boolean skipRecheckGaps)
    {
        if (isGapLightingUpdated && world.provider.hasSkyLight() && !skipRecheckGaps)
        {
            recheckGaps(world.isRemote);
        }

        ticked = true;

        if (!isLightPopulated && isTerrainPopulated) checkLight();

        while (!tileEntityPosQueue.isEmpty())
        {
            BlockPos blockpos = tileEntityPosQueue.poll();

            if (getTileEntity(blockpos, Chunk.EnumCreateEntityType.CHECK) == null && getBlockState(blockpos).getBlock().hasTileEntity(getBlockState(blockpos)))
            {
                TileEntity tileentity = createNewTileEntity(blockpos);
                world.setTileEntity(blockpos, tileentity);
                world.markBlockRangeForRenderUpdate(blockpos, blockpos);
            }
        }
    }

    public boolean isPopulated()
    {
        return ticked && isTerrainPopulated && isLightPopulated;
    }

    public boolean wasTicked()
    {
        return ticked;
    }

    public ChunkPos getPos()
    {
        return new ChunkPos(this.x, this.z);
    }

    public boolean isEmptyBetween(int startY, int endY)
    {
        if (startY < 0) startY = 0;

        if (endY >= 256) endY = 255;

        for (int i = startY; i <= endY; i += 16)
        {
            ExtendedBlockStorage extendedblockstorage = storageArrays[i >> 4];
            if (extendedblockstorage != NULL_BLOCK_STORAGE && !extendedblockstorage.isEmpty()) return false;
        }

        return true;
    }

    public void setStorageArrays(ExtendedBlockStorage[] newStorageArrays)
    {
        if (storageArrays.length != newStorageArrays.length)
        {
            LOGGER.warn("Could not set level chunk sections, array length is {} instead of {}", newStorageArrays.length, storageArrays.length);
        }
        else
        {
            System.arraycopy(newStorageArrays, 0, storageArrays, 0, storageArrays.length);
        }
    }

    @SideOnly(Side.CLIENT)
    public void read(PacketBuffer buf, int availableSections, boolean groundUpContinuous)
    {
        for (TileEntity tileEntity : tileEntities.values())
        {
            tileEntity.updateContainingBlockInfo();
            tileEntity.getBlockMetadata();
            tileEntity.getBlockType();
        }

        boolean flag = world.provider.hasSkyLight();

        for (int i = 0; i < storageArrays.length; ++i)
        {
            ExtendedBlockStorage extendedblockstorage = storageArrays[i];

            if ((availableSections & 1 << i) == 0)
            {
                if (groundUpContinuous && extendedblockstorage != NULL_BLOCK_STORAGE)
                {
                    storageArrays[i] = NULL_BLOCK_STORAGE;
                }
            }
            else
            {
                if (extendedblockstorage == NULL_BLOCK_STORAGE)
                {
                    extendedblockstorage = new ExtendedBlockStorage(i << 4, flag);
                    storageArrays[i] = extendedblockstorage;
                }

                extendedblockstorage.getData().read(buf);
                buf.readBytes(extendedblockstorage.getBlockLight().getData());

                if (flag)
                {
                    buf.readBytes(extendedblockstorage.getSkyLight().getData());
                }
            }
        }

        if (groundUpContinuous)
        {
            buf.readBytes(blockBiomeArray);
        }

        for (int j = 0; j < storageArrays.length; ++j)
        {
            if (storageArrays[j] != NULL_BLOCK_STORAGE && (availableSections & 1 << j) != 0)
            {
                storageArrays[j].recalculateRefCounts();
            }
        }

        isLightPopulated = true;
        isTerrainPopulated = true;
        generateHeightMap();

        List<TileEntity> invalidList = new java.util.ArrayList<>();

        for (TileEntity tileentity : tileEntities.values())
        {
            if (tileentity.shouldRefresh(world, tileentity.getPos(), tileentity.getBlockType().getStateFromMeta(tileentity.getBlockMetadata()), getBlockState(tileentity.getPos())))
            {
                invalidList.add(tileentity);
            }

            tileentity.updateContainingBlockInfo();
        }

        for (TileEntity te : invalidList) te.invalidate();
    }

    public Biome getBiome(BlockPos pos, BiomeProvider provider)
    {
        int i = pos.getX() & 15;
        int j = pos.getZ() & 15;
        int k = blockBiomeArray[j << 4 | i] & 255;

        if (k == 255)
        {
            Biome biome = provider.getBiome(pos, Biomes.PLAINS);
            k = Biome.getIdForBiome(biome);
            blockBiomeArray[j << 4 | i] = (byte) (k & 255);
        }

        Biome biome1 = Biome.getBiome(k);
        return biome1 == null ? Biomes.PLAINS : biome1;
    }

    public byte[] getBiomeArray()
    {
        return blockBiomeArray;
    }

    public void setBiomeArray(byte[] biomeArray)
    {
        if (blockBiomeArray.length != biomeArray.length)
        {
            LOGGER.warn("Could not set level chunk biomes, array length is {} instead of {}", biomeArray.length, blockBiomeArray.length);
        }
        else
        {
            System.arraycopy(biomeArray, 0, blockBiomeArray, 0, blockBiomeArray.length);
        }
    }

    public void resetRelightChecks()
    {
        queuedLightChecks = 0;
    }

    public void enqueueRelightChecks()
    {
        if (queuedLightChecks < 4096)
        {
            BlockPos blockpos = new BlockPos(this.x << 4, 0, this.z << 4);

            for (int i = 0; i < 8; ++i)
            {
                if (queuedLightChecks >= 4096) return;


                int j = queuedLightChecks % 16;
                int k = queuedLightChecks / 16 % 16;
                int l = queuedLightChecks / 256;
                ++queuedLightChecks;

                for (int i1 = 0; i1 < 16; ++i1)
                {
                    BlockPos blockpos1 = blockpos.add(k, (j << 4) + i1, l);
                    boolean flag = i1 == 0 || i1 == 15 || k == 0 || k == 15 || l == 0 || l == 15;

                    if (storageArrays[j] == NULL_BLOCK_STORAGE && flag || storageArrays[j] != NULL_BLOCK_STORAGE && storageArrays[j].get(k, i1, l).getBlock().isAir(storageArrays[j].get(k, i1, l), world, blockpos1))
                    {
                        for (EnumFacing enumfacing : EnumFacing.values())
                        {
                            BlockPos blockpos2 = blockpos1.offset(enumfacing);

                            if (world.getBlockState(blockpos2).getLightValue(world, blockpos2) > 0)
                            {
                                world.checkLight(blockpos2);
                            }
                        }

                        world.checkLight(blockpos1);
                    }
                }
            }
        }
    }

    public void checkLight()
    {
        isTerrainPopulated = true;
        isLightPopulated = true;
        BlockPos blockpos = new BlockPos(this.x << 4, 0, this.z << 4);

        if (world.provider.hasSkyLight())
        {
            if (world.isAreaLoaded(blockpos.add(-1, 0, -1), blockpos.add(16, world.getSeaLevel(), 16)))
            {
                label44:

                for (int i = 0; i < 16; ++i)
                {
                    for (int j = 0; j < 16; ++j)
                    {
                        if (!checkLight(i, j))
                        {
                            isLightPopulated = false;
                            break label44;
                        }
                    }
                }

                if (isLightPopulated)
                {
                    for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL)
                    {
                        int k = enumfacing.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? 16 : 1;
                        world.getChunkFromBlockCoords(blockpos.offset(enumfacing, k)).checkLightSide(enumfacing.getOpposite());
                    }

                    setSkylightUpdated();
                }
            }
            else isLightPopulated = false;
        }
    }

    private void setSkylightUpdated()
    {
        for (int i = 0; i < updateSkylightColumns.length; ++i) updateSkylightColumns[i] = true;
        recheckGaps(false);
    }

    private void checkLightSide(EnumFacing facing)
    {
        if (isTerrainPopulated)
        {
            if (facing == EnumFacing.EAST)
            {
                for (int i = 0; i < 16; ++i) checkLight(15, i);
            }
            else if (facing == EnumFacing.WEST)
            {
                for (int j = 0; j < 16; ++j) checkLight(0, j);
            }
            else if (facing == EnumFacing.SOUTH)
            {
                for (int k = 0; k < 16; ++k) checkLight(k, 15);
            }
            else if (facing == EnumFacing.NORTH)
            {
                for (int l = 0; l < 16; ++l) checkLight(l, 0);
            }
        }
    }

    private boolean checkLight(int x, int z)
    {
        int i = getTopFilledSegment();
        boolean flag = false;
        boolean flag1 = false;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos((this.x << 4) + x, 0, (this.z << 4) + z);

        for (int j = i + 16 - 1; j > world.getSeaLevel() || j > 0 && !flag1; --j)
        {
            blockpos$mutableblockpos.setPos(blockpos$mutableblockpos.getX(), j, blockpos$mutableblockpos.getZ());
            int k = getBlockLightOpacity(blockpos$mutableblockpos);

            if (k == 255 && blockpos$mutableblockpos.getY() < world.getSeaLevel()) flag1 = true;

            if (!flag && k > 0) flag = true;
            else if (flag && k == 0 && !world.checkLight(blockpos$mutableblockpos)) return false;
        }

        for (int l = blockpos$mutableblockpos.getY(); l > 0; --l)
        {
            blockpos$mutableblockpos.setPos(blockpos$mutableblockpos.getX(), l, blockpos$mutableblockpos.getZ());

            if (getBlockState(blockpos$mutableblockpos).getLightValue(world, blockpos$mutableblockpos) > 0)
            {
                world.checkLight(blockpos$mutableblockpos);
            }
        }

        return true;
    }

    public boolean isLoaded()
    {
        return loaded;
    }

    @SideOnly(Side.CLIENT)
    public void markLoaded(boolean loaded)
    {
        this.loaded = loaded;
    }

    public World getWorld()
    {
        return world;
    }

    public int[] getHeightMap()
    {
        return heightMap;
    }

    public void setHeightMap(int[] newHeightMap)
    {
        if (heightMap.length != newHeightMap.length)
        {
            LOGGER.warn("Could not set level chunk heightmap, array length is {} instead of {}", Integer.valueOf(newHeightMap.length), Integer.valueOf(heightMap.length));
        }
        else
        {
            System.arraycopy(newHeightMap, 0, heightMap, 0, heightMap.length);
            heightMapMinimum = com.google.common.primitives.Ints.min(heightMap); // Forge: fix MC-117412
        }
    }

    public Map<BlockPos, TileEntity> getTileEntityMap()
    {
        return tileEntities;
    }

    public ClassInheritanceMultiMap<Entity>[] getEntityLists()
    {
        return entityLists;
    }

    public boolean isTerrainPopulated()
    {
        return isTerrainPopulated;
    }

    public void setTerrainPopulated(boolean terrainPopulated)
    {
        isTerrainPopulated = terrainPopulated;
    }

    public boolean isLightPopulated()
    {
        return isLightPopulated;
    }

    public void setLightPopulated(boolean lightPopulated)
    {
        isLightPopulated = lightPopulated;
    }

    public void setModified(boolean modified)
    {
        dirty = modified;
    }

    public void setHasEntities(boolean hasEntitiesIn)
    {
        hasEntities = hasEntitiesIn;
    }

    public void setLastSaveTime(long saveTime)
    {
        lastSaveTime = saveTime;
    }

    public int getLowestHeight()
    {
        return heightMapMinimum;
    }

    public long getInhabitedTime()
    {
        return inhabitedTime;
    }

    public void setInhabitedTime(long newInhabitedTime)
    {
        inhabitedTime = newInhabitedTime;
    }

    public static enum EnumCreateEntityType
    {
        IMMEDIATE,
        QUEUED,
        CHECK
    }

    public void removeInvalidTileEntity(BlockPos pos)
    {
        if (loaded)
        {
            TileEntity entity = tileEntities.get(pos);
            if (entity != null && entity.isInvalid()) tileEntities.remove(pos);
        }
    }

    private static ChunkPos populating = null;

    private void logCascadingWorldGeneration()
    {
        net.minecraftforge.fml.common.ModContainer activeModContainer = net.minecraftforge.fml.common.Loader.instance().activeModContainer();
        String format = "{} loaded a new chunk {} in dimension {} ({}) while populating chunk {}, causing cascading worldgen lag.";

        if (activeModContainer == null)
        {
            net.minecraftforge.fml.common.FMLLog.log.debug(format, "Minecraft", getPos(), world.provider.getDimension(), world.provider.getDimensionType().getName(), populating);
            net.minecraftforge.fml.common.FMLLog.log.debug("Consider setting 'fixVanillaCascading' to 'true' in the Forge config to fix many cases where this occurs in the base game.");
        }
        else
        {
            net.minecraftforge.fml.common.FMLLog.log.warn(format, activeModContainer.getName(), getPos(), world.provider.getDimension(), world.provider.getDimensionType().getName(), populating);
            net.minecraftforge.fml.common.FMLLog.log.warn("Please report this to the mod's issue tracker. This log can be disabled in the Forge config.");
        }
    }

    private final net.minecraftforge.common.capabilities.CapabilityDispatcher capabilities;

    @Nullable
    public net.minecraftforge.common.capabilities.CapabilityDispatcher getCapabilities()
    {
        return capabilities;
    }

    @Override
    public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, @Nullable EnumFacing facing)
    {
        return capabilities != null && capabilities.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable EnumFacing facing)
    {
        return capabilities == null ? null : capabilities.getCapability(capability, facing);
    }
}