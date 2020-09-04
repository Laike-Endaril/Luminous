package net.minecraft.world;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.advancements.FunctionManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.pathfinding.PathWorldListener;
import net.minecraft.profiler.Profiler;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.village.VillageCollection;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraft.world.storage.loot.LootTableManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;

public abstract class World implements IBlockAccess, net.minecraftforge.common.capabilities.ICapabilityProvider
{
    public static double MAX_ENTITY_RADIUS = 2.0D;

    private int seaLevel = 63;
    protected boolean scheduledUpdatesAreImmediate;
    public final List<Entity> loadedEntityList = Lists.newArrayList();
    protected final List<Entity> unloadedEntityList = Lists.newArrayList();
    public final List<TileEntity> loadedTileEntityList = Lists.newArrayList();
    public final List<TileEntity> tickableTileEntities = Lists.newArrayList();
    private final List<TileEntity> addedTileEntityList = Lists.newArrayList();
    private final List<TileEntity> tileEntitiesToBeRemoved = Lists.newArrayList();
    public final List<EntityPlayer> playerEntities = Lists.newArrayList();
    public final List<Entity> weatherEffects = Lists.newArrayList();
    protected final IntHashMap<Entity> entitiesById = new IntHashMap<>();
    private int skylightSubtracted;
    protected int updateLCG = (new Random()).nextInt();
    public float prevRainingStrength;
    public float rainingStrength;
    public float prevThunderingStrength;
    public float thunderingStrength;
    private int lastLightningBolt;
    public final Random rand = new Random();
    public final WorldProvider provider;
    protected PathWorldListener pathListener = new PathWorldListener();
    protected List<IWorldEventListener> eventListeners;
    protected IChunkProvider chunkProvider;
    protected final ISaveHandler saveHandler;
    protected WorldInfo worldInfo;
    protected boolean findingSpawnPoint;
    protected MapStorage mapStorage;
    public VillageCollection villageCollection;
    protected LootTableManager lootTable;
    protected AdvancementManager advancementManager;
    protected FunctionManager functionManager;
    public final Profiler profiler;
    private final Calendar calendar;
    protected Scoreboard worldScoreboard;
    public final boolean isRemote;
    protected boolean spawnHostileMobs;
    protected boolean spawnPeacefulMobs;
    private boolean processingLoadedTiles;
    private final WorldBorder worldBorder;
    int[] lightUpdateBlockList;

    public boolean restoringBlockSnapshots = false;
    public boolean captureBlockSnapshots = false;
    public java.util.ArrayList<net.minecraftforge.common.util.BlockSnapshot> capturedBlockSnapshots = new java.util.ArrayList<net.minecraftforge.common.util.BlockSnapshot>();
    private net.minecraftforge.common.capabilities.CapabilityDispatcher capabilities;
    private net.minecraftforge.common.util.WorldCapabilityData capabilityData;

    protected World(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn, Profiler profilerIn, boolean client)
    {
        eventListeners = Lists.newArrayList(pathListener);
        calendar = Calendar.getInstance();
        worldScoreboard = new Scoreboard();
        spawnHostileMobs = true;
        spawnPeacefulMobs = true;
        lightUpdateBlockList = new int[32768];
        saveHandler = saveHandlerIn;
        profiler = profilerIn;
        worldInfo = info;
        provider = providerIn;
        isRemote = client;
        worldBorder = providerIn.createWorldBorder();
        perWorldStorage = new MapStorage(null);
    }

    public World init()
    {
        return this;
    }

    public Biome getBiome(final BlockPos pos)
    {
        return provider.getBiomeForCoords(pos);
    }

    public Biome getBiomeForCoordsBody(final BlockPos pos)
    {
        if (isBlockLoaded(pos))
        {
            Chunk chunk = getChunkFromBlockCoords(pos);

            try
            {
                return chunk.getBiome(pos, provider.getBiomeProvider());
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Getting biome");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Coordinates of biome request");
                crashreportcategory.addDetail("Location", () -> CrashReportCategory.getCoordinateInfo(pos));
                throw new ReportedException(crashreport);
            }
        }
        else return provider.getBiomeProvider().getBiome(pos, Biomes.PLAINS);
    }

    public BiomeProvider getBiomeProvider()
    {
        return provider.getBiomeProvider();
    }

    protected abstract IChunkProvider createChunkProvider();

    public void initialize(WorldSettings settings)
    {
        worldInfo.setServerInitialized(true);
    }

    @Nullable
    public MinecraftServer getMinecraftServer()
    {
        return null;
    }

    @SideOnly(Side.CLIENT)
    public void setInitialSpawnLocation()
    {
        setSpawnPoint(new BlockPos(8, 64, 8));
    }

    public IBlockState getGroundAboveSeaLevel(BlockPos pos)
    {
        BlockPos blockpos;

        for (blockpos = new BlockPos(pos.getX(), getSeaLevel(), pos.getZ()); !isAirBlock(blockpos.up()); blockpos = blockpos.up())
        {
        }

        return getBlockState(blockpos);
    }

    public boolean isValid(BlockPos pos)
    {
        return !isOutsideBuildHeight(pos) && pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000;
    }

    public boolean isOutsideBuildHeight(BlockPos pos)
    {
        return pos.getY() < 0 || pos.getY() >= 256;
    }

    public boolean isAirBlock(BlockPos pos)
    {
        return getBlockState(pos).getBlock().isAir(getBlockState(pos), this, pos);
    }

    public boolean isBlockLoaded(BlockPos pos)
    {
        return isBlockLoaded(pos, true);
    }

    public boolean isBlockLoaded(BlockPos pos, boolean allowEmpty)
    {
        return isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4, allowEmpty);
    }

    public boolean isAreaLoaded(BlockPos center, int radius)
    {
        return isAreaLoaded(center, radius, true);
    }

    public boolean isAreaLoaded(BlockPos center, int radius, boolean allowEmpty)
    {
        return isAreaLoaded(center.getX() - radius, center.getY() - radius, center.getZ() - radius, center.getX() + radius, center.getY() + radius, center.getZ() + radius, allowEmpty);
    }

    public boolean isAreaLoaded(BlockPos from, BlockPos to)
    {
        return isAreaLoaded(from, to, true);
    }

    public boolean isAreaLoaded(BlockPos from, BlockPos to, boolean allowEmpty)
    {
        return isAreaLoaded(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ(), allowEmpty);
    }

    public boolean isAreaLoaded(StructureBoundingBox box)
    {
        return isAreaLoaded(box, true);
    }

    public boolean isAreaLoaded(StructureBoundingBox box, boolean allowEmpty)
    {
        return isAreaLoaded(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, allowEmpty);
    }

    private boolean isAreaLoaded(int xStart, int yStart, int zStart, int xEnd, int yEnd, int zEnd, boolean allowEmpty)
    {
        if (yEnd < 0 || yStart > 255) return false;


        xStart = xStart >> 4;
        zStart = zStart >> 4;
        xEnd = xEnd >> 4;
        zEnd = zEnd >> 4;

        for (int i = xStart; i <= xEnd; ++i)
        {
            for (int j = zStart; j <= zEnd; ++j)
            {
                if (!isChunkLoaded(i, j, allowEmpty)) return false;
            }
        }

        return true;
    }

    protected abstract boolean isChunkLoaded(int x, int z, boolean allowEmpty);

    public Chunk getChunkFromBlockCoords(BlockPos pos)
    {
        return getChunkFromChunkCoords(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public Chunk getChunkFromChunkCoords(int chunkX, int chunkZ)
    {
        return chunkProvider.provideChunk(chunkX, chunkZ);
    }

    public boolean isChunkGeneratedAt(int x, int z)
    {
        return isChunkLoaded(x, z, false) ? true : chunkProvider.isChunkGeneratedAt(x, z);
    }

    public boolean setBlockState(BlockPos pos, IBlockState newState, int flags)
    {
        if (isOutsideBuildHeight(pos)) return false;
        if (!isRemote && worldInfo.getTerrainType() == WorldType.DEBUG_ALL_BLOCK_STATES) return false;


        Chunk chunk = getChunkFromBlockCoords(pos);

        pos = pos.toImmutable();
        net.minecraftforge.common.util.BlockSnapshot blockSnapshot = null;
        if (captureBlockSnapshots && !isRemote)
        {
            blockSnapshot = net.minecraftforge.common.util.BlockSnapshot.getBlockSnapshot(this, pos, flags);
            capturedBlockSnapshots.add(blockSnapshot);
        }
        IBlockState oldState = getBlockState(pos);
        int oldLight = oldState.getLightValue(this, pos);
        int oldOpacity = oldState.getLightOpacity(this, pos);

        IBlockState iblockstate = chunk.setBlockState(pos, newState);
        if (iblockstate == null)
        {
            if (blockSnapshot != null) capturedBlockSnapshots.remove(blockSnapshot);
            return false;
        }


        if (newState.getLightOpacity(this, pos) != oldOpacity || newState.getLightValue(this, pos) != oldLight)
        {
            profiler.startSection("checkLight");
            checkLight(pos);
            profiler.endSection();
        }

        if (blockSnapshot == null) markAndNotifyBlock(pos, chunk, iblockstate, newState, flags);
        return true;
    }

    public void markAndNotifyBlock(BlockPos pos, @Nullable Chunk chunk, IBlockState iblockstate, IBlockState newState, int flags)
    {
        Block block = newState.getBlock();
        if ((flags & 2) != 0 && (!isRemote || (flags & 4) == 0) && (chunk == null || chunk.isPopulated()))
        {
            notifyBlockUpdate(pos, iblockstate, newState, flags);
        }

        if (!isRemote && (flags & 1) != 0)
        {
            notifyNeighborsRespectDebug(pos, iblockstate.getBlock(), true);
            if (newState.hasComparatorInputOverride()) updateComparatorOutputLevel(pos, block);
        }
        else if (!isRemote && (flags & 16) == 0) updateObservingBlocksAt(pos, block);
    }

    public boolean setBlockToAir(BlockPos pos)
    {
        return setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
    }

    public boolean destroyBlock(BlockPos pos, boolean dropBlock)
    {
        IBlockState iblockstate = getBlockState(pos);
        Block block = iblockstate.getBlock();

        if (block.isAir(iblockstate, this, pos)) return false;


        playEvent(2001, pos, Block.getStateId(iblockstate));
        if (dropBlock) block.dropBlockAsItem(this, pos, iblockstate, 0);
        return setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
    }

    public boolean setBlockState(BlockPos pos, IBlockState state)
    {
        return setBlockState(pos, state, 3);
    }

    public void notifyBlockUpdate(BlockPos pos, IBlockState oldState, IBlockState newState, int flags)
    {
        for (int i = 0; i < eventListeners.size(); ++i)
        {
            eventListeners.get(i).notifyBlockUpdate(this, pos, oldState, newState, flags);
        }
    }

    public void notifyNeighborsRespectDebug(BlockPos pos, Block blockType, boolean p_175722_3_)
    {
        if (worldInfo.getTerrainType() != WorldType.DEBUG_ALL_BLOCK_STATES)
        {
            notifyNeighborsOfStateChange(pos, blockType, p_175722_3_);
        }
    }

    public void markBlocksDirtyVertical(int x, int z, int y1, int y2)
    {
        if (y1 > y2)
        {
            int i = y2;
            y2 = y1;
            y1 = i;
        }

        if (provider.hasSkyLight())
        {
            for (int j = y1; j <= y2; ++j) checkLightFor(EnumSkyBlock.SKY, new BlockPos(x, j, z));
        }

        markBlockRangeForRenderUpdate(x, y1, z, x, y2, z);
    }

    public void markBlockRangeForRenderUpdate(BlockPos rangeMin, BlockPos rangeMax)
    {
        markBlockRangeForRenderUpdate(rangeMin.getX(), rangeMin.getY(), rangeMin.getZ(), rangeMax.getX(), rangeMax.getY(), rangeMax.getZ());
    }

    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2)
    {
        for (int i = 0; i < eventListeners.size(); ++i)
        {
            eventListeners.get(i).markBlockRangeForRenderUpdate(x1, y1, z1, x2, y2, z2);
        }
    }

    public void updateObservingBlocksAt(BlockPos pos, Block blockType)
    {
        observedNeighborChanged(pos.west(), blockType, pos);
        observedNeighborChanged(pos.east(), blockType, pos);
        observedNeighborChanged(pos.down(), blockType, pos);
        observedNeighborChanged(pos.up(), blockType, pos);
        observedNeighborChanged(pos.north(), blockType, pos);
        observedNeighborChanged(pos.south(), blockType, pos);
    }

    public void notifyNeighborsOfStateChange(BlockPos pos, Block blockType, boolean updateObservers)
    {
        if (net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(this, pos, getBlockState(pos), java.util.EnumSet.allOf(EnumFacing.class), updateObservers).isCanceled()) return;


        neighborChanged(pos.west(), blockType, pos);
        neighborChanged(pos.east(), blockType, pos);
        neighborChanged(pos.down(), blockType, pos);
        neighborChanged(pos.up(), blockType, pos);
        neighborChanged(pos.north(), blockType, pos);
        neighborChanged(pos.south(), blockType, pos);

        if (updateObservers) updateObservingBlocksAt(pos, blockType);
    }

    public void notifyNeighborsOfStateExcept(BlockPos pos, Block blockType, EnumFacing skipSide)
    {
        java.util.EnumSet<EnumFacing> directions = java.util.EnumSet.allOf(EnumFacing.class);
        directions.remove(skipSide);
        if (net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(this, pos, getBlockState(pos), directions, false).isCanceled()) return;


        if (skipSide != EnumFacing.WEST) neighborChanged(pos.west(), blockType, pos);
        if (skipSide != EnumFacing.EAST) neighborChanged(pos.east(), blockType, pos);
        if (skipSide != EnumFacing.DOWN) neighborChanged(pos.down(), blockType, pos);
        if (skipSide != EnumFacing.UP) neighborChanged(pos.up(), blockType, pos);
        if (skipSide != EnumFacing.NORTH) neighborChanged(pos.north(), blockType, pos);
        if (skipSide != EnumFacing.SOUTH) neighborChanged(pos.south(), blockType, pos);
    }

    public void neighborChanged(BlockPos pos, final Block blockIn, BlockPos fromPos)
    {
        if (isRemote) return;


        IBlockState iblockstate = getBlockState(pos);
        try
        {
            iblockstate.neighborChanged(this, pos, blockIn, fromPos);
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception while updating neighbours");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being updated");
            crashreportcategory.addDetail("Source block type", () ->
            {
                try
                {
                    return String.format("ID #%d (%s // %s // %s)", Block.getIdFromBlock(blockIn), blockIn.getUnlocalizedName(), blockIn.getClass().getName(), blockIn.getRegistryName());
                }
                catch (Throwable var2)
                {
                    return "ID #" + Block.getIdFromBlock(blockIn);
                }
            });
            CrashReportCategory.addBlockInfo(crashreportcategory, pos, iblockstate);
            throw new ReportedException(crashreport);
        }
    }

    public void observedNeighborChanged(BlockPos pos, final Block p_190529_2_, BlockPos p_190529_3_)
    {
        if (isRemote) return;


        IBlockState iblockstate = getBlockState(pos);
        try
        {
            iblockstate.getBlock().observedNeighborChange(iblockstate, this, pos, p_190529_2_, p_190529_3_);
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception while updating neighbours");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being updated");
            crashreportcategory.addDetail("Source block type", () ->
            {
                try
                {
                    return String.format("ID #%d (%s // %s // %s)", Block.getIdFromBlock(p_190529_2_), p_190529_2_.getUnlocalizedName(), p_190529_2_.getClass().getName(), p_190529_2_.getRegistryName());
                }
                catch (Throwable var2)
                {
                    return "ID #" + Block.getIdFromBlock(p_190529_2_);
                }
            });
            CrashReportCategory.addBlockInfo(crashreportcategory, pos, iblockstate);
            throw new ReportedException(crashreport);
        }
    }

    public boolean isBlockTickPending(BlockPos pos, Block blockType)
    {
        return false;
    }

    public boolean canSeeSky(BlockPos pos)
    {
        return getChunkFromBlockCoords(pos).canSeeSky(pos);
    }

    public boolean canBlockSeeSky(BlockPos pos)
    {
        if (pos.getY() >= getSeaLevel()) return canSeeSky(pos);


        BlockPos blockpos = new BlockPos(pos.getX(), getSeaLevel(), pos.getZ());
        if (!canSeeSky(blockpos)) return false;


        for (BlockPos blockpos1 = blockpos.down(); blockpos1.getY() > pos.getY(); blockpos1 = blockpos1.down())
        {
            IBlockState iblockstate = getBlockState(blockpos1);
            if (iblockstate.getBlock().getLightOpacity(iblockstate, this, blockpos) > 0 && !iblockstate.getMaterial().isLiquid()) return false;
        }

        return true;
    }

    public int getLight(BlockPos pos)
    {
        if (pos.getY() < 0) return 0;


        if (pos.getY() >= 256) pos = new BlockPos(pos.getX(), 255, pos.getZ());
        return getChunkFromBlockCoords(pos).getLightSubtracted(pos, 0);
    }

    public int getLightFromNeighbors(BlockPos pos)
    {
        return getLight(pos, true);
    }

    public int getLight(BlockPos pos, boolean checkNeighbors)
    {
        if (pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000)
        {
            if (checkNeighbors && getBlockState(pos).useNeighborBrightness())
            {
                int up = getLight(pos.up(), false);
                int east = getLight(pos.east(), false);
                int west = getLight(pos.west(), false);
                int south = getLight(pos.south(), false);
                int north = getLight(pos.north(), false);

                if (east > up) up = east;
                if (west > up) up = west;
                if (south > up) up = south;
                if (north > up) up = north;

                return up;
            }


            if (pos.getY() < 0) return 0;


            if (pos.getY() >= 256) pos = new BlockPos(pos.getX(), 255, pos.getZ());


            //TODO Luminous Start
//            if (pos.equals(Luminous.pos)) return 15;
            //TODO Luminous End


            Chunk chunk = getChunkFromBlockCoords(pos);
            return chunk.getLightSubtracted(pos, skylightSubtracted);
        }


        return 15;
    }

    public BlockPos getHeight(BlockPos pos)
    {
        return new BlockPos(pos.getX(), getHeight(pos.getX(), pos.getZ()), pos.getZ());
    }

    public int getHeight(int x, int z)
    {
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000)
        {
            if (!isChunkLoaded(x >> 4, z >> 4, true)) return 0;

            return getChunkFromChunkCoords(x >> 4, z >> 4).getHeightValue(x & 15, z & 15);
        }

        return getSeaLevel() + 1;
    }

    @Deprecated
    public int getChunksLowestHorizon(int x, int z)
    {
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000)
        {
            if (!isChunkLoaded(x >> 4, z >> 4, true)) return 0;
            return getChunkFromChunkCoords(x >> 4, z >> 4).getLowestHeight();
        }
        return getSeaLevel() + 1;
    }

    @SideOnly(Side.CLIENT)
    public int getLightFromNeighborsFor(EnumSkyBlock type, BlockPos pos)
    {
        if (!provider.hasSkyLight() && type == EnumSkyBlock.SKY) return 0;
        else
        {
            if (pos.getY() < 0) pos = new BlockPos(pos.getX(), 0, pos.getZ());

            if (!isValid(pos)) return type.defaultLightValue;
            if (!isBlockLoaded(pos)) return type.defaultLightValue;


            if (getBlockState(pos).useNeighborBrightness())
            {
                int up = getLightFor(type, pos.up());
                int east = getLightFor(type, pos.east());
                int west = getLightFor(type, pos.west());
                int south = getLightFor(type, pos.south());
                int north = getLightFor(type, pos.north());

                if (east > up) up = east;
                if (west > up) up = west;
                if (south > up) up = south;
                if (north > up) up = north;

                return up;
            }

            Chunk chunk = getChunkFromBlockCoords(pos);
            return chunk.getLightFor(type, pos);
        }
    }

    public int getLightFor(EnumSkyBlock type, BlockPos pos)
    {
        if (pos.getY() < 0) pos = new BlockPos(pos.getX(), 0, pos.getZ());

        if (!isValid(pos)) return type.defaultLightValue;
        if (!isBlockLoaded(pos)) return type.defaultLightValue;


        Chunk chunk = getChunkFromBlockCoords(pos);
        return chunk.getLightFor(type, pos);
    }

    public void setLightFor(EnumSkyBlock type, BlockPos pos, int lightValue)
    {
        if (isValid(pos))
        {
            if (isBlockLoaded(pos))
            {
                Chunk chunk = getChunkFromBlockCoords(pos);
                chunk.setLightFor(type, pos, lightValue);
                notifyLightSet(pos);
            }
        }
    }

    public void notifyLightSet(BlockPos pos)
    {
        for (int i = 0; i < eventListeners.size(); ++i) eventListeners.get(i).notifyLightSet(pos);
    }

    @SideOnly(Side.CLIENT)
    public int getCombinedLight(BlockPos pos, int lightValue)
    {
        int i = getLightFromNeighborsFor(EnumSkyBlock.SKY, pos);
        int j = getLightFromNeighborsFor(EnumSkyBlock.BLOCK, pos);

        if (j < lightValue) j = lightValue;

        return i << 20 | j << 4;
    }

    public float getLightBrightness(BlockPos pos)
    {
        return provider.getLightBrightnessTable()[getLightFromNeighbors(pos)];
    }

    public IBlockState getBlockState(BlockPos pos)
    {
        if (isOutsideBuildHeight(pos)) return Blocks.AIR.getDefaultState();
        return getChunkFromBlockCoords(pos).getBlockState(pos);
    }

    public boolean isDaytime()
    {
        return provider.isDaytime();
    }

    @Nullable
    public RayTraceResult rayTraceBlocks(Vec3d start, Vec3d end)
    {
        return rayTraceBlocks(start, end, false, false, false);
    }

    @Nullable
    public RayTraceResult rayTraceBlocks(Vec3d start, Vec3d end, boolean stopOnLiquid)
    {
        return rayTraceBlocks(start, end, stopOnLiquid, false, false);
    }

    @Nullable
    public RayTraceResult rayTraceBlocks(Vec3d vec31, Vec3d vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock)
    {
        if (!Double.isNaN(vec31.x) && !Double.isNaN(vec31.y) && !Double.isNaN(vec31.z))
        {
            if (!Double.isNaN(vec32.x) && !Double.isNaN(vec32.y) && !Double.isNaN(vec32.z))
            {
                int i = MathHelper.floor(vec32.x);
                int j = MathHelper.floor(vec32.y);
                int k = MathHelper.floor(vec32.z);
                int l = MathHelper.floor(vec31.x);
                int i1 = MathHelper.floor(vec31.y);
                int j1 = MathHelper.floor(vec31.z);
                BlockPos blockpos = new BlockPos(l, i1, j1);
                IBlockState iblockstate = getBlockState(blockpos);
                Block block = iblockstate.getBlock();

                if ((!ignoreBlockWithoutBoundingBox || iblockstate.getCollisionBoundingBox(this, blockpos) != Block.NULL_AABB) && block.canCollideCheck(iblockstate, stopOnLiquid))
                {
                    RayTraceResult raytraceresult = iblockstate.collisionRayTrace(this, blockpos, vec31, vec32);
                    if (raytraceresult != null) return raytraceresult;
                }

                RayTraceResult raytraceresult2 = null;
                int k1 = 200;

                while (k1-- >= 0)
                {
                    if (Double.isNaN(vec31.x) || Double.isNaN(vec31.y) || Double.isNaN(vec31.z)) return null;

                    if (l == i && i1 == j && j1 == k) return returnLastUncollidableBlock ? raytraceresult2 : null;

                    boolean flag2 = true;
                    boolean flag = true;
                    boolean flag1 = true;
                    double d0 = 999.0D;
                    double d1 = 999.0D;
                    double d2 = 999.0D;

                    if (i > l) d0 = (double) l + 1.0D;
                    else if (i < l) d0 = (double) l + 0.0D;
                    else flag2 = false;

                    if (j > i1) d1 = (double) i1 + 1.0D;
                    else if (j < i1) d1 = (double) i1 + 0.0D;
                    else flag = false;

                    if (k > j1) d2 = (double) j1 + 1.0D;
                    else if (k < j1) d2 = (double) j1 + 0.0D;
                    else flag1 = false;

                    double d3 = 999.0D;
                    double d4 = 999.0D;
                    double d5 = 999.0D;
                    double d6 = vec32.x - vec31.x;
                    double d7 = vec32.y - vec31.y;
                    double d8 = vec32.z - vec31.z;

                    if (flag2) d3 = (d0 - vec31.x) / d6;
                    if (flag) d4 = (d1 - vec31.y) / d7;
                    if (flag1) d5 = (d2 - vec31.z) / d8;

                    if (d3 == -0.0D) d3 = -1.0E-4D;
                    if (d4 == -0.0D) d4 = -1.0E-4D;
                    if (d5 == -0.0D) d5 = -1.0E-4D;

                    EnumFacing enumfacing;

                    if (d3 < d4 && d3 < d5)
                    {
                        enumfacing = i > l ? EnumFacing.WEST : EnumFacing.EAST;
                        vec31 = new Vec3d(d0, vec31.y + d7 * d3, vec31.z + d8 * d3);
                    }
                    else if (d4 < d5)
                    {
                        enumfacing = j > i1 ? EnumFacing.DOWN : EnumFacing.UP;
                        vec31 = new Vec3d(vec31.x + d6 * d4, d1, vec31.z + d8 * d4);
                    }
                    else
                    {
                        enumfacing = k > j1 ? EnumFacing.NORTH : EnumFacing.SOUTH;
                        vec31 = new Vec3d(vec31.x + d6 * d5, vec31.y + d7 * d5, d2);
                    }

                    l = MathHelper.floor(vec31.x) - (enumfacing == EnumFacing.EAST ? 1 : 0);
                    i1 = MathHelper.floor(vec31.y) - (enumfacing == EnumFacing.UP ? 1 : 0);
                    j1 = MathHelper.floor(vec31.z) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
                    blockpos = new BlockPos(l, i1, j1);
                    IBlockState iblockstate1 = getBlockState(blockpos);
                    Block block1 = iblockstate1.getBlock();

                    if (!ignoreBlockWithoutBoundingBox || iblockstate1.getMaterial() == Material.PORTAL || iblockstate1.getCollisionBoundingBox(this, blockpos) != Block.NULL_AABB)
                    {
                        if (block1.canCollideCheck(iblockstate1, stopOnLiquid))
                        {
                            RayTraceResult raytraceresult1 = iblockstate1.collisionRayTrace(this, blockpos, vec31, vec32);

                            if (raytraceresult1 != null) return raytraceresult1;
                        }
                        else raytraceresult2 = new RayTraceResult(RayTraceResult.Type.MISS, vec31, enumfacing, blockpos);
                    }
                }

                return returnLastUncollidableBlock ? raytraceresult2 : null;
            }

            return null;
        }

        return null;
    }

    public void playSound(@Nullable EntityPlayer player, BlockPos pos, SoundEvent soundIn, SoundCategory category, float volume, float pitch)
    {
        playSound(player, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, soundIn, category, volume, pitch);
    }

    public void playSound(@Nullable EntityPlayer player, double x, double y, double z, SoundEvent soundIn, SoundCategory category, float volume, float pitch)
    {
        net.minecraftforge.event.entity.PlaySoundAtEntityEvent event = net.minecraftforge.event.ForgeEventFactory.onPlaySoundAtEntity(player, soundIn, category, volume, pitch);
        if (event.isCanceled() || event.getSound() == null) return;
        soundIn = event.getSound();
        category = event.getCategory();
        volume = event.getVolume();
        pitch = event.getPitch();

        for (int i = 0; i < eventListeners.size(); ++i)
        {
            eventListeners.get(i).playSoundToAllNearExcept(player, soundIn, category, x, y, z, volume, pitch);
        }
    }

    public void playSound(double x, double y, double z, SoundEvent soundIn, SoundCategory category, float volume, float pitch, boolean distanceDelay)
    {
    }

    public void playRecord(BlockPos blockPositionIn, @Nullable SoundEvent soundEventIn)
    {
        for (int i = 0; i < eventListeners.size(); ++i) eventListeners.get(i).playRecord(soundEventIn, blockPositionIn);
    }

    public void spawnParticle(EnumParticleTypes particleType, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters)
    {
        spawnParticle(particleType.getParticleID(), particleType.getShouldIgnoreRange(), xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, parameters);
    }

    public void spawnAlwaysVisibleParticle(int p_190523_1_, double p_190523_2_, double p_190523_4_, double p_190523_6_, double p_190523_8_, double p_190523_10_, double p_190523_12_, int... p_190523_14_)
    {
        for (int i = 0; i < eventListeners.size(); ++i)
        {
            eventListeners.get(i).spawnParticle(p_190523_1_, false, true, p_190523_2_, p_190523_4_, p_190523_6_, p_190523_8_, p_190523_10_, p_190523_12_, p_190523_14_);
        }
    }

    @SideOnly(Side.CLIENT)
    public void spawnParticle(EnumParticleTypes particleType, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters)
    {
        spawnParticle(particleType.getParticleID(), particleType.getShouldIgnoreRange() || ignoreRange, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, parameters);
    }

    private void spawnParticle(int particleID, boolean ignoreRange, double xCood, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters)
    {
        for (int i = 0; i < eventListeners.size(); ++i)
        {
            eventListeners.get(i).spawnParticle(particleID, ignoreRange, xCood, yCoord, zCoord, xSpeed, ySpeed, zSpeed, parameters);
        }
    }

    public boolean addWeatherEffect(Entity entityIn)
    {
        weatherEffects.add(entityIn);
        return true;
    }

    public boolean spawnEntity(Entity entityIn)
    {
        if (!isRemote && (entityIn == null || (entityIn instanceof net.minecraft.entity.item.EntityItem && restoringBlockSnapshots))) return false;

        int i = MathHelper.floor(entityIn.posX / 16.0D);
        int j = MathHelper.floor(entityIn.posZ / 16.0D);
        boolean flag = entityIn.forceSpawn;

        if (entityIn instanceof EntityPlayer) flag = true;

        if (!flag && !isChunkLoaded(i, j, false)) return false;


        if (entityIn instanceof EntityPlayer)
        {
            EntityPlayer entityplayer = (EntityPlayer) entityIn;
            playerEntities.add(entityplayer);
            updateAllPlayersSleepingFlag();
        }

        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.EntityJoinWorldEvent(entityIn, this)) && !flag) return false;


        getChunkFromChunkCoords(i, j).addEntity(entityIn);
        loadedEntityList.add(entityIn);
        onEntityAdded(entityIn);
        return true;
    }

    public void onEntityAdded(Entity entityIn)
    {
        for (int i = 0; i < eventListeners.size(); ++i) eventListeners.get(i).onEntityAdded(entityIn);
        entityIn.onAddedToWorld();
    }

    public void onEntityRemoved(Entity entityIn)
    {
        for (int i = 0; i < eventListeners.size(); ++i) eventListeners.get(i).onEntityRemoved(entityIn);
        entityIn.onRemovedFromWorld();
    }

    public void removeEntity(Entity entityIn)
    {
        if (entityIn.isBeingRidden()) entityIn.removePassengers();
        if (entityIn.isRiding()) entityIn.dismountRidingEntity();
        entityIn.setDead();

        if (entityIn instanceof EntityPlayer)
        {
            playerEntities.remove(entityIn);
            updateAllPlayersSleepingFlag();
            onEntityRemoved(entityIn);
        }
    }

    public void removeEntityDangerously(Entity entityIn)
    {
        entityIn.setDropItemsWhenDead(false);
        entityIn.setDead();

        if (entityIn instanceof EntityPlayer)
        {
            playerEntities.remove(entityIn);
            updateAllPlayersSleepingFlag();
        }

        int i = entityIn.chunkCoordX;
        int j = entityIn.chunkCoordZ;

        if (entityIn.addedToChunk && isChunkLoaded(i, j, true))
        {
            getChunkFromChunkCoords(i, j).removeEntity(entityIn);
        }

        loadedEntityList.remove(entityIn);
        onEntityRemoved(entityIn);
    }

    public void addEventListener(IWorldEventListener listener)
    {
        eventListeners.add(listener);
    }

    private boolean getCollisionBoxes(@Nullable Entity entityIn, AxisAlignedBB aabb, boolean p_191504_3_, @Nullable List<AxisAlignedBB> outList)
    {
        int i = MathHelper.floor(aabb.minX) - 1;
        int j = MathHelper.ceil(aabb.maxX) + 1;
        int k = MathHelper.floor(aabb.minY) - 1;
        int l = MathHelper.ceil(aabb.maxY) + 1;
        int i1 = MathHelper.floor(aabb.minZ) - 1;
        int j1 = MathHelper.ceil(aabb.maxZ) + 1;
        WorldBorder worldborder = getWorldBorder();
        boolean flag = entityIn != null && entityIn.isOutsideBorder();
        boolean flag1 = entityIn != null && isInsideWorldBorder(entityIn);
        IBlockState iblockstate = Blocks.STONE.getDefaultState();
        BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();

        if (p_191504_3_ && !net.minecraftforge.event.ForgeEventFactory.gatherCollisionBoxes(this, entityIn, aabb, outList)) return true;
        try
        {
            for (int k1 = i; k1 < j; ++k1)
            {
                for (int l1 = i1; l1 < j1; ++l1)
                {
                    boolean flag2 = k1 == i || k1 == j - 1;
                    boolean flag3 = l1 == i1 || l1 == j1 - 1;

                    if ((!flag2 || !flag3) && isBlockLoaded(blockpos$pooledmutableblockpos.setPos(k1, 64, l1)))
                    {
                        for (int i2 = k; i2 < l; ++i2)
                        {
                            if (!flag2 && !flag3 || i2 != l - 1)
                            {
                                if (p_191504_3_)
                                {
                                    if (k1 < -30000000 || k1 >= 30000000 || l1 < -30000000 || l1 >= 30000000) return true;
                                }
                                else if (entityIn != null && flag == flag1)
                                {
                                    entityIn.setOutsideBorder(!flag1);
                                }

                                blockpos$pooledmutableblockpos.setPos(k1, i2, l1);
                                IBlockState iblockstate1;

                                if (!p_191504_3_ && !worldborder.contains(blockpos$pooledmutableblockpos) && flag1) iblockstate1 = iblockstate;
                                else iblockstate1 = getBlockState(blockpos$pooledmutableblockpos);

                                iblockstate1.addCollisionBoxToList(this, blockpos$pooledmutableblockpos, aabb, outList, entityIn, false);

                                if (p_191504_3_ && !net.minecraftforge.event.ForgeEventFactory.gatherCollisionBoxes(this, entityIn, aabb, outList)) return true;
                            }
                        }
                    }
                }
            }
        }
        finally
        {
            blockpos$pooledmutableblockpos.release();
        }

        return !outList.isEmpty();
    }

    public List<AxisAlignedBB> getCollisionBoxes(@Nullable Entity entityIn, AxisAlignedBB aabb)
    {
        List<AxisAlignedBB> list = Lists.newArrayList();
        getCollisionBoxes(entityIn, aabb, false, list);

        if (entityIn != null)
        {
            List<Entity> list1 = getEntitiesWithinAABBExcludingEntity(entityIn, aabb.grow(0.25D));

            for (Entity entity : list1)
            {
                if (!entityIn.isRidingSameEntity(entity))
                {
                    AxisAlignedBB axisalignedbb = entity.getCollisionBoundingBox();

                    if (axisalignedbb != null && axisalignedbb.intersects(aabb))
                    {
                        list.add(axisalignedbb);
                    }

                    axisalignedbb = entityIn.getCollisionBox(entity);

                    if (axisalignedbb != null && axisalignedbb.intersects(aabb))
                    {
                        list.add(axisalignedbb);
                    }
                }
            }
        }
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.GetCollisionBoxesEvent(this, entityIn, aabb, list));
        return list;
    }

    public void removeEventListener(IWorldEventListener listener)
    {
        eventListeners.remove(listener);
    }

    public boolean isInsideWorldBorder(Entity p_191503_1_)
    {
        double d0 = worldBorder.minX();
        double d1 = worldBorder.minZ();
        double d2 = worldBorder.maxX();
        double d3 = worldBorder.maxZ();

        if (p_191503_1_.isOutsideBorder())
        {
            ++d0;
            ++d1;
            --d2;
            --d3;
        }
        else
        {
            --d0;
            --d1;
            ++d2;
            ++d3;
        }

        return p_191503_1_.posX > d0 && p_191503_1_.posX < d2 && p_191503_1_.posZ > d1 && p_191503_1_.posZ < d3;
    }

    public boolean collidesWithAnyBlock(AxisAlignedBB bbox)
    {
        return getCollisionBoxes(null, bbox, true, Lists.newArrayList());
    }

    public int calculateSkylightSubtracted(float partialTicks)
    {
        float f = provider.getSunBrightnessFactor(partialTicks);
        f = 1 - f;
        return (int) (f * 11);
    }

    public float getSunBrightnessFactor(float partialTicks)
    {
        float f = getCelestialAngle(partialTicks);
        float f1 = 1 - (MathHelper.cos(f * ((float) Math.PI * 2F)) * 2.0F + 0.5F);
        f1 = MathHelper.clamp(f1, 0.0F, 1.0F);
        f1 = 1 - f1;
        f1 = (float) ((double) f1 * (1.0D - (double) (getRainStrength(partialTicks) * 5.0F) / 16.0D));
        f1 = (float) ((double) f1 * (1.0D - (double) (getThunderStrength(partialTicks) * 5.0F) / 16.0D));
        return f1;
    }

    @SideOnly(Side.CLIENT)
    public float getSunBrightness(float partialTicks)
    {
        return provider.getSunBrightness(partialTicks);
    }

    @SideOnly(Side.CLIENT)
    public float getSunBrightnessBody(float partialTicks)
    {
        float f = getCelestialAngle(partialTicks);
        float f1 = 1 - (MathHelper.cos((float) (f * Math.PI * 2)) * 2 + 0.2f);
        f1 = MathHelper.clamp(f1, 0, 1);
        f1 = 1 - f1;
        f1 = f1 * (1 - getRainStrength(partialTicks) * 5 / 16);
        f1 = f1 * (1 - getThunderStrength(partialTicks) * 5 / 16);
        return 0.2f + f1 * 0.8f;
    }

    @SideOnly(Side.CLIENT)
    public Vec3d getSkyColor(Entity entityIn, float partialTicks)
    {
        return provider.getSkyColor(entityIn, partialTicks);
    }

    @SideOnly(Side.CLIENT)
    public Vec3d getSkyColorBody(Entity entityIn, float partialTicks)
    {
        float f = getCelestialAngle(partialTicks);
        float f1 = MathHelper.cos((float) (f * Math.PI * 2)) * 2 + 0.5f;
        f1 = MathHelper.clamp(f1, 0, 1);
        int i = MathHelper.floor(entityIn.posX);
        int j = MathHelper.floor(entityIn.posY);
        int k = MathHelper.floor(entityIn.posZ);
        BlockPos blockpos = new BlockPos(i, j, k);
        int l = net.minecraftforge.client.ForgeHooksClient.getSkyBlendColour(this, blockpos);
        float f3 = (float) (l >> 16 & 255) / 255.0F;
        float f4 = (float) (l >> 8 & 255) / 255.0F;
        float f5 = (float) (l & 255) / 255.0F;
        f3 = f3 * f1;
        f4 = f4 * f1;
        f5 = f5 * f1;
        float f6 = getRainStrength(partialTicks);

        if (f6 > 0.0F)
        {
            float f7 = (f3 * 0.3F + f4 * 0.59F + f5 * 0.11F) * 0.6F;
            float f8 = 1.0F - f6 * 0.75F;
            f3 = f3 * f8 + f7 * (1.0F - f8);
            f4 = f4 * f8 + f7 * (1.0F - f8);
            f5 = f5 * f8 + f7 * (1.0F - f8);
        }

        float f10 = getThunderStrength(partialTicks);

        if (f10 > 0.0F)
        {
            float f11 = (f3 * 0.3F + f4 * 0.59F + f5 * 0.11F) * 0.2F;
            float f9 = 1.0F - f10 * 0.75F;
            f3 = f3 * f9 + f11 * (1.0F - f9);
            f4 = f4 * f9 + f11 * (1.0F - f9);
            f5 = f5 * f9 + f11 * (1.0F - f9);
        }

        if (lastLightningBolt > 0)
        {
            float f12 = (float) lastLightningBolt - partialTicks;

            if (f12 > 1.0F) f12 = 1.0F;

            f12 = f12 * 0.45F;
            f3 = f3 * (1.0F - f12) + 0.8F * f12;
            f4 = f4 * (1.0F - f12) + 0.8F * f12;
            f5 = f5 * (1.0F - f12) + 1.0F * f12;
        }

        return new Vec3d((double) f3, (double) f4, (double) f5);
    }

    public float getCelestialAngle(float partialTicks)
    {
        return provider.calculateCelestialAngle(getWorldTime(), partialTicks);
    }

    @SideOnly(Side.CLIENT)
    public int getMoonPhase()
    {
        return provider.getMoonPhase(getWorldTime());
    }

    public float getCurrentMoonPhaseFactor()
    {
        return provider.getCurrentMoonPhaseFactor();
    }

    public float getCurrentMoonPhaseFactorBody()
    {
        return WorldProvider.MOON_PHASE_FACTORS[provider.getMoonPhase(getWorldTime())];
    }

    public float getCelestialAngleRadians(float partialTicks)
    {
        float f = getCelestialAngle(partialTicks);
        return f * ((float) Math.PI * 2F);
    }

    @SideOnly(Side.CLIENT)
    public Vec3d getCloudColour(float partialTicks)
    {
        return provider.getCloudColor(partialTicks);
    }

    @SideOnly(Side.CLIENT)
    public Vec3d getCloudColorBody(float partialTicks)
    {
        float f = getCelestialAngle(partialTicks);
        float f1 = MathHelper.cos(f * ((float) Math.PI * 2F)) * 2.0F + 0.5F;
        f1 = MathHelper.clamp(f1, 0, 1);
        float f2 = 1;
        float f3 = 1;
        float f4 = 1;
        float f5 = getRainStrength(partialTicks);

        if (f5 > 0.0F)
        {
            float f6 = (f2 * 0.3F + f3 * 0.59F + f4 * 0.11F) * 0.6F;
            float f7 = 1.0F - f5 * 0.95F;
            f2 = f2 * f7 + f6 * (1.0F - f7);
            f3 = f3 * f7 + f6 * (1.0F - f7);
            f4 = f4 * f7 + f6 * (1.0F - f7);
        }

        f2 = f2 * (f1 * 0.9F + 0.1F);
        f3 = f3 * (f1 * 0.9F + 0.1F);
        f4 = f4 * (f1 * 0.85F + 0.15F);
        float f9 = getThunderStrength(partialTicks);

        if (f9 > 0.0F)
        {
            float f10 = (f2 * 0.3F + f3 * 0.59F + f4 * 0.11F) * 0.2F;
            float f8 = 1.0F - f9 * 0.95F;
            f2 = f2 * f8 + f10 * (1.0F - f8);
            f3 = f3 * f8 + f10 * (1.0F - f8);
            f4 = f4 * f8 + f10 * (1.0F - f8);
        }

        return new Vec3d((double) f2, (double) f3, (double) f4);
    }

    @SideOnly(Side.CLIENT)
    public Vec3d getFogColor(float partialTicks)
    {
        float f = getCelestialAngle(partialTicks);
        return provider.getFogColor(f, partialTicks);
    }

    public BlockPos getPrecipitationHeight(BlockPos pos)
    {
        return getChunkFromBlockCoords(pos).getPrecipitationHeight(pos);
    }

    public BlockPos getTopSolidOrLiquidBlock(BlockPos pos)
    {
        Chunk chunk = getChunkFromBlockCoords(pos);
        BlockPos blockpos;
        BlockPos blockpos1;

        for (blockpos = new BlockPos(pos.getX(), chunk.getTopFilledSegment() + 16, pos.getZ()); blockpos.getY() >= 0; blockpos = blockpos1)
        {
            blockpos1 = blockpos.down();
            IBlockState state = chunk.getBlockState(blockpos1);

            if (state.getMaterial().blocksMovement() && !state.getBlock().isLeaves(state, this, blockpos1) && !state.getBlock().isFoliage(this, blockpos1))
            {
                break;
            }
        }

        return blockpos;
    }

    @SideOnly(Side.CLIENT)
    public float getStarBrightness(float partialTicks)
    {
        return provider.getStarBrightness(partialTicks);
    }

    @SideOnly(Side.CLIENT)
    public float getStarBrightnessBody(float partialTicks)
    {
        float f = getCelestialAngle(partialTicks);
        float f1 = 1.0F - (MathHelper.cos(f * ((float) Math.PI * 2F)) * 2.0F + 0.25F);
        f1 = MathHelper.clamp(f1, 0.0F, 1.0F);
        return f1 * f1 * 0.5F;
    }

    public boolean isUpdateScheduled(BlockPos pos, Block blk)
    {
        return true;
    }

    public void scheduleUpdate(BlockPos pos, Block blockIn, int delay)
    {
    }

    public void updateBlockTick(BlockPos pos, Block blockIn, int delay, int priority)
    {
    }

    public void scheduleBlockUpdate(BlockPos pos, Block blockIn, int delay, int priority)
    {
    }

    public void updateEntities()
    {
        profiler.startSection("entities");
        profiler.startSection("global");

        for (int i = 0; i < weatherEffects.size(); ++i)
        {
            Entity entity = weatherEffects.get(i);

            try
            {
                if (entity.updateBlocked) continue;
                ++entity.ticksExisted;
                entity.onUpdate();
            }
            catch (Throwable throwable2)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable2, "Ticking entity");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being ticked");

                if (entity == null)
                {
                    crashreportcategory.addCrashSection("Entity", "~~NULL~~");
                }
                else
                {
                    entity.addEntityCrashInfo(crashreportcategory);
                }

                if (net.minecraftforge.common.ForgeModContainer.removeErroringEntities)
                {
                    net.minecraftforge.fml.common.FMLLog.log.fatal("{}", crashreport.getCompleteReport());
                    removeEntity(entity);
                }
                else throw new ReportedException(crashreport);
            }

            if (entity.isDead) weatherEffects.remove(i--);
        }

        profiler.endStartSection("remove");
        loadedEntityList.removeAll(unloadedEntityList);

        for (Entity entity1 : unloadedEntityList)
        {
            int j = entity1.chunkCoordX;
            int k1 = entity1.chunkCoordZ;

            if (entity1.addedToChunk && isChunkLoaded(j, k1, true)) getChunkFromChunkCoords(j, k1).removeEntity(entity1);
        }

        for (Entity anUnloadedEntityList : unloadedEntityList) onEntityRemoved(anUnloadedEntityList);

        unloadedEntityList.clear();
        tickPlayers();
        profiler.endStartSection("regular");

        for (int i1 = 0; i1 < loadedEntityList.size(); ++i1)
        {
            Entity entity2 = loadedEntityList.get(i1);
            Entity entity3 = entity2.getRidingEntity();

            if (entity3 != null)
            {
                if (!entity3.isDead && entity3.isPassenger(entity2)) continue;
                entity2.dismountRidingEntity();
            }

            profiler.startSection("tick");

            if (!entity2.isDead && !(entity2 instanceof EntityPlayerMP))
            {
                try
                {
                    net.minecraftforge.server.timings.TimeTracker.ENTITY_UPDATE.trackStart(entity2);
                    updateEntity(entity2);
                    net.minecraftforge.server.timings.TimeTracker.ENTITY_UPDATE.trackEnd(entity2);
                }
                catch (Throwable throwable1)
                {
                    CrashReport crashreport1 = CrashReport.makeCrashReport(throwable1, "Ticking entity");
                    CrashReportCategory crashreportcategory1 = crashreport1.makeCategory("Entity being ticked");
                    entity2.addEntityCrashInfo(crashreportcategory1);
                    if (net.minecraftforge.common.ForgeModContainer.removeErroringEntities)
                    {
                        net.minecraftforge.fml.common.FMLLog.log.fatal("{}", crashreport1.getCompleteReport());
                        removeEntity(entity2);
                    }
                    else throw new ReportedException(crashreport1);
                }
            }

            profiler.endSection();
            profiler.startSection("remove");

            if (entity2.isDead)
            {
                int l1 = entity2.chunkCoordX;
                int i2 = entity2.chunkCoordZ;

                if (entity2.addedToChunk && isChunkLoaded(l1, i2, true))
                {
                    getChunkFromChunkCoords(l1, i2).removeEntity(entity2);
                }

                loadedEntityList.remove(i1--);
                onEntityRemoved(entity2);
            }

            profiler.endSection();
        }

        profiler.endStartSection("blockEntities");

        processingLoadedTiles = true;

        if (!tileEntitiesToBeRemoved.isEmpty())
        {
            for (Object tile : tileEntitiesToBeRemoved)
            {
                ((TileEntity) tile).onChunkUnload();
            }

            java.util.Set<TileEntity> remove = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            remove.addAll(tileEntitiesToBeRemoved);
            tickableTileEntities.removeAll(remove);
            loadedTileEntityList.removeAll(remove);
            tileEntitiesToBeRemoved.clear();
        }

        Iterator<TileEntity> iterator = tickableTileEntities.iterator();

        while (iterator.hasNext())
        {
            TileEntity tileentity = iterator.next();

            if (!tileentity.isInvalid() && tileentity.hasWorld())
            {
                BlockPos blockpos = tileentity.getPos();

                if (isBlockLoaded(blockpos, false) && worldBorder.contains(blockpos))
                {
                    try
                    {
                        profiler.func_194340_a(() -> String.valueOf((Object) TileEntity.getKey(tileentity.getClass())));
                        net.minecraftforge.server.timings.TimeTracker.TILE_ENTITY_UPDATE.trackStart(tileentity);
                        ((ITickable) tileentity).update();
                        net.minecraftforge.server.timings.TimeTracker.TILE_ENTITY_UPDATE.trackEnd(tileentity);
                        profiler.endSection();
                    }
                    catch (Throwable throwable)
                    {
                        CrashReport crashreport2 = CrashReport.makeCrashReport(throwable, "Ticking block entity");
                        CrashReportCategory crashreportcategory2 = crashreport2.makeCategory("Block entity being ticked");
                        tileentity.addInfoToCrashReport(crashreportcategory2);
                        if (net.minecraftforge.common.ForgeModContainer.removeErroringTileEntities)
                        {
                            net.minecraftforge.fml.common.FMLLog.log.fatal("{}", crashreport2.getCompleteReport());
                            tileentity.invalidate();
                            removeTileEntity(tileentity.getPos());
                        }
                        else throw new ReportedException(crashreport2);
                    }
                }
            }

            if (tileentity.isInvalid())
            {
                iterator.remove();
                loadedTileEntityList.remove(tileentity);

                if (isBlockLoaded(tileentity.getPos()))
                {
                    Chunk chunk = getChunkFromBlockCoords(tileentity.getPos());
                    if (chunk.getTileEntity(tileentity.getPos(), net.minecraft.world.chunk.Chunk.EnumCreateEntityType.CHECK) == tileentity)
                    {
                        chunk.removeTileEntity(tileentity.getPos());
                    }
                }
            }
        }

        processingLoadedTiles = false;
        profiler.endStartSection("pendingBlockEntities");

        if (!addedTileEntityList.isEmpty())
        {
            for (int j1 = 0; j1 < addedTileEntityList.size(); ++j1)
            {
                TileEntity tileentity1 = addedTileEntityList.get(j1);

                if (!tileentity1.isInvalid())
                {
                    if (!loadedTileEntityList.contains(tileentity1)) addTileEntity(tileentity1);

                    if (isBlockLoaded(tileentity1.getPos()))
                    {
                        Chunk chunk = getChunkFromBlockCoords(tileentity1.getPos());
                        IBlockState iblockstate = chunk.getBlockState(tileentity1.getPos());
                        chunk.addTileEntity(tileentity1.getPos(), tileentity1);
                        notifyBlockUpdate(tileentity1.getPos(), iblockstate, iblockstate, 3);
                    }
                }
            }

            addedTileEntityList.clear();
        }

        profiler.endSection();
        profiler.endSection();
    }

    protected void tickPlayers()
    {
    }

    public boolean addTileEntity(TileEntity tile)
    {
        if (tile.getWorld() != this) tile.setWorld(this);
        if (processingLoadedTiles) return addedTileEntityList.add(tile);

        boolean flag = loadedTileEntityList.add(tile);

        if (flag && tile instanceof ITickable)
        {
            tickableTileEntities.add(tile);
        }
        tile.onLoad();

        if (isRemote)
        {
            BlockPos blockpos1 = tile.getPos();
            IBlockState iblockstate1 = getBlockState(blockpos1);
            notifyBlockUpdate(blockpos1, iblockstate1, iblockstate1, 2);
        }

        return flag;
    }

    public void addTileEntities(Collection<TileEntity> tileEntityCollection)
    {
        if (processingLoadedTiles)
        {
            for (TileEntity te : tileEntityCollection)
            {
                if (te.getWorld() != this) te.setWorld(this);
            }
            addedTileEntityList.addAll(tileEntityCollection);
        }
        else
        {
            for (TileEntity tileentity2 : tileEntityCollection) addTileEntity(tileentity2);
        }
    }

    public void updateEntity(Entity ent)
    {
        updateEntityWithOptionalForce(ent, true);
    }

    public void updateEntityWithOptionalForce(Entity entityIn, boolean forceUpdate)
    {
        if (!(entityIn instanceof EntityPlayer))
        {
            int j2 = MathHelper.floor(entityIn.posX);
            int k2 = MathHelper.floor(entityIn.posZ);

            boolean isForced = getPersistentChunks().containsKey(new net.minecraft.util.math.ChunkPos(j2 >> 4, k2 >> 4));
            int range = isForced ? 0 : 32;
            boolean canUpdate = !forceUpdate || isAreaLoaded(j2 - range, 0, k2 - range, j2 + range, 0, k2 + range, true);
            if (!canUpdate) canUpdate = net.minecraftforge.event.ForgeEventFactory.canEntityUpdate(entityIn);
            if (!canUpdate) return;
        }

        entityIn.lastTickPosX = entityIn.posX;
        entityIn.lastTickPosY = entityIn.posY;
        entityIn.lastTickPosZ = entityIn.posZ;
        entityIn.prevRotationYaw = entityIn.rotationYaw;
        entityIn.prevRotationPitch = entityIn.rotationPitch;

        if (forceUpdate && entityIn.addedToChunk)
        {
            ++entityIn.ticksExisted;

            if (entityIn.isRiding()) entityIn.updateRidden();
            else if (!entityIn.updateBlocked) entityIn.onUpdate();
        }

        profiler.startSection("chunkCheck");

        if (Double.isNaN(entityIn.posX) || Double.isInfinite(entityIn.posX))
        {
            entityIn.posX = entityIn.lastTickPosX;
        }

        if (Double.isNaN(entityIn.posY) || Double.isInfinite(entityIn.posY))
        {
            entityIn.posY = entityIn.lastTickPosY;
        }

        if (Double.isNaN(entityIn.posZ) || Double.isInfinite(entityIn.posZ))
        {
            entityIn.posZ = entityIn.lastTickPosZ;
        }

        if (Double.isNaN((double) entityIn.rotationPitch) || Double.isInfinite((double) entityIn.rotationPitch))
        {
            entityIn.rotationPitch = entityIn.prevRotationPitch;
        }

        if (Double.isNaN((double) entityIn.rotationYaw) || Double.isInfinite((double) entityIn.rotationYaw))
        {
            entityIn.rotationYaw = entityIn.prevRotationYaw;
        }

        int i3 = MathHelper.floor(entityIn.posX / 16.0D);
        int j3 = MathHelper.floor(entityIn.posY / 16.0D);
        int k3 = MathHelper.floor(entityIn.posZ / 16.0D);

        if (!entityIn.addedToChunk || entityIn.chunkCoordX != i3 || entityIn.chunkCoordY != j3 || entityIn.chunkCoordZ != k3)
        {
            if (entityIn.addedToChunk && isChunkLoaded(entityIn.chunkCoordX, entityIn.chunkCoordZ, true))
            {
                getChunkFromChunkCoords(entityIn.chunkCoordX, entityIn.chunkCoordZ).removeEntityAtIndex(entityIn, entityIn.chunkCoordY);
            }

            if (!entityIn.setPositionNonDirty() && !isChunkLoaded(i3, k3, true))
            {
                entityIn.addedToChunk = false;
            }
            else
            {
                getChunkFromChunkCoords(i3, k3).addEntity(entityIn);
            }
        }

        profiler.endSection();

        if (forceUpdate && entityIn.addedToChunk)
        {
            for (Entity entity4 : entityIn.getPassengers())
            {
                if (!entity4.isDead && entity4.getRidingEntity() == entityIn)
                {
                    updateEntity(entity4);
                }
                else entity4.dismountRidingEntity();
            }
        }
    }

    public boolean checkNoEntityCollision(AxisAlignedBB bb)
    {
        return checkNoEntityCollision(bb, (Entity) null);
    }

    public boolean checkNoEntityCollision(AxisAlignedBB bb, @Nullable Entity entityIn)
    {
        List<Entity> list = getEntitiesWithinAABBExcludingEntity((Entity) null, bb);

        for (Entity entity4 : list)
        {
            if (!entity4.isDead && entity4.preventEntitySpawning && entity4 != entityIn && (entityIn == null || !entity4.isRidingSameEntity(entityIn))) // Forge: fix MC-103516
            {
                return false;
            }
        }

        return true;
    }

    public boolean checkBlockCollision(AxisAlignedBB bb)
    {
        int j2 = MathHelper.floor(bb.minX);
        int k2 = MathHelper.ceil(bb.maxX);
        int l2 = MathHelper.floor(bb.minY);
        int i3 = MathHelper.ceil(bb.maxY);
        int j3 = MathHelper.floor(bb.minZ);
        int k3 = MathHelper.ceil(bb.maxZ);
        BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();

        for (int l3 = j2; l3 < k2; ++l3)
        {
            for (int i4 = l2; i4 < i3; ++i4)
            {
                for (int j4 = j3; j4 < k3; ++j4)
                {
                    IBlockState iblockstate1 = getBlockState(blockpos$pooledmutableblockpos.setPos(l3, i4, j4));

                    if (iblockstate1.getMaterial() != Material.AIR)
                    {
                        blockpos$pooledmutableblockpos.release();
                        return true;
                    }
                }
            }
        }

        blockpos$pooledmutableblockpos.release();
        return false;
    }

    public boolean containsAnyLiquid(AxisAlignedBB bb)
    {
        int j2 = MathHelper.floor(bb.minX);
        int k2 = MathHelper.ceil(bb.maxX);
        int l2 = MathHelper.floor(bb.minY);
        int i3 = MathHelper.ceil(bb.maxY);
        int j3 = MathHelper.floor(bb.minZ);
        int k3 = MathHelper.ceil(bb.maxZ);
        BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();

        for (int l3 = j2; l3 < k2; ++l3)
        {
            for (int i4 = l2; i4 < i3; ++i4)
            {
                for (int j4 = j3; j4 < k3; ++j4)
                {
                    IBlockState iblockstate1 = getBlockState(blockpos$pooledmutableblockpos.setPos(l3, i4, j4));

                    Boolean result = iblockstate1.getBlock().isAABBInsideLiquid(this, blockpos$pooledmutableblockpos, bb);
                    if (result != null)
                    {
                        if (!result) continue;
                        blockpos$pooledmutableblockpos.release();
                        return true;
                    }
                    if (iblockstate1.getMaterial().isLiquid())
                    {
                        blockpos$pooledmutableblockpos.release();
                        return true;
                    }
                }
            }
        }

        blockpos$pooledmutableblockpos.release();
        return false;
    }

    public boolean isFlammableWithin(AxisAlignedBB bb)
    {
        int j2 = MathHelper.floor(bb.minX);
        int k2 = MathHelper.ceil(bb.maxX);
        int l2 = MathHelper.floor(bb.minY);
        int i3 = MathHelper.ceil(bb.maxY);
        int j3 = MathHelper.floor(bb.minZ);
        int k3 = MathHelper.ceil(bb.maxZ);

        if (isAreaLoaded(j2, l2, j3, k2, i3, k3, true))
        {
            BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();

            for (int l3 = j2; l3 < k2; ++l3)
            {
                for (int i4 = l2; i4 < i3; ++i4)
                {
                    for (int j4 = j3; j4 < k3; ++j4)
                    {
                        Block block = getBlockState(blockpos$pooledmutableblockpos.setPos(l3, i4, j4)).getBlock();

                        if (block == Blocks.FIRE || block == Blocks.FLOWING_LAVA || block == Blocks.LAVA)
                        {
                            blockpos$pooledmutableblockpos.release();
                            return true;
                        }
                        else if (block.isBurning(this, new BlockPos(l3, i4, j4)))
                        {
                            blockpos$pooledmutableblockpos.release();
                            return true;
                        }
                    }
                }
            }

            blockpos$pooledmutableblockpos.release();
        }

        return false;
    }

    public boolean handleMaterialAcceleration(AxisAlignedBB bb, Material materialIn, Entity entityIn)
    {
        int j2 = MathHelper.floor(bb.minX);
        int k2 = MathHelper.ceil(bb.maxX);
        int l2 = MathHelper.floor(bb.minY);
        int i3 = MathHelper.ceil(bb.maxY);
        int j3 = MathHelper.floor(bb.minZ);
        int k3 = MathHelper.ceil(bb.maxZ);

        if (!isAreaLoaded(j2, l2, j3, k2, i3, k3, true)) return false;


        boolean flag = false;
        Vec3d vec3d = Vec3d.ZERO;
        BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();

        for (int l3 = j2; l3 < k2; ++l3)
        {
            for (int i4 = l2; i4 < i3; ++i4)
            {
                for (int j4 = j3; j4 < k3; ++j4)
                {
                    blockpos$pooledmutableblockpos.setPos(l3, i4, j4);
                    IBlockState iblockstate1 = getBlockState(blockpos$pooledmutableblockpos);
                    Block block = iblockstate1.getBlock();

                    Boolean result = block.isEntityInsideMaterial(this, blockpos$pooledmutableblockpos, iblockstate1, entityIn, (double) i3, materialIn, false);
                    if (result != null && result)
                    {
                        // Forge: When requested call blocks modifyAcceleration method, and more importantly cause this method to return true, which results in an entity being "inWater"
                        flag = true;
                        vec3d = block.modifyAcceleration(this, blockpos$pooledmutableblockpos, entityIn, vec3d);
                        continue;
                    }
                    else if (result != null && !result) continue;

                    if (iblockstate1.getMaterial() == materialIn)
                    {
                        double d0 = (double) ((float) (i4 + 1) - BlockLiquid.getLiquidHeightPercent(iblockstate1.getValue(BlockLiquid.LEVEL)));

                        if ((double) i3 >= d0)
                        {
                            flag = true;
                            vec3d = block.modifyAcceleration(this, blockpos$pooledmutableblockpos, entityIn, vec3d);
                        }
                    }
                }
            }
        }

        blockpos$pooledmutableblockpos.release();

        if (vec3d.lengthVector() > 0 && entityIn.isPushedByWater())
        {
            vec3d = vec3d.normalize();
            entityIn.motionX += vec3d.x * 0.014;
            entityIn.motionY += vec3d.y * 0.014;
            entityIn.motionZ += vec3d.z * 0.014;
        }

        return flag;
    }

    public boolean isMaterialInBB(AxisAlignedBB bb, Material materialIn)
    {
        int j2 = MathHelper.floor(bb.minX);
        int k2 = MathHelper.ceil(bb.maxX);
        int l2 = MathHelper.floor(bb.minY);
        int i3 = MathHelper.ceil(bb.maxY);
        int j3 = MathHelper.floor(bb.minZ);
        int k3 = MathHelper.ceil(bb.maxZ);
        BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();

        for (int l3 = j2; l3 < k2; ++l3)
        {
            for (int i4 = l2; i4 < i3; ++i4)
            {
                for (int j4 = j3; j4 < k3; ++j4)
                {
                    IBlockState iblockstate1 = getBlockState(blockpos$pooledmutableblockpos.setPos(l3, i4, j4));
                    Boolean result = iblockstate1.getBlock().isAABBInsideMaterial(this, blockpos$pooledmutableblockpos, bb, materialIn);
                    if (result != null)
                    {
                        if (!result) continue;
                        blockpos$pooledmutableblockpos.release();
                        return true;
                    }
                    if (iblockstate1.getMaterial() == materialIn)
                    {
                        blockpos$pooledmutableblockpos.release();
                        return true;
                    }
                }
            }
        }

        blockpos$pooledmutableblockpos.release();
        return false;
    }

    public Explosion createExplosion(@Nullable Entity entityIn, double x, double y, double z, float strength, boolean isSmoking)
    {
        return newExplosion(entityIn, x, y, z, strength, false, isSmoking);
    }

    public Explosion newExplosion(@Nullable Entity entityIn, double x, double y, double z, float strength, boolean isFlaming, boolean isSmoking)
    {
        Explosion explosion = new Explosion(this, entityIn, x, y, z, strength, isFlaming, isSmoking);
        if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(this, explosion)) return explosion;
        explosion.doExplosionA();
        explosion.doExplosionB(true);
        return explosion;
    }

    public float getBlockDensity(Vec3d vec, AxisAlignedBB bb)
    {
        double d0 = 1.0D / ((bb.maxX - bb.minX) * 2.0D + 1.0D);
        double d1 = 1.0D / ((bb.maxY - bb.minY) * 2.0D + 1.0D);
        double d2 = 1.0D / ((bb.maxZ - bb.minZ) * 2.0D + 1.0D);
        double d3 = (1.0D - Math.floor(1.0D / d0) * d0) / 2.0D;
        double d4 = (1.0D - Math.floor(1.0D / d2) * d2) / 2.0D;

        if (d0 >= 0.0D && d1 >= 0.0D && d2 >= 0.0D)
        {
            int j2 = 0;
            int k2 = 0;

            for (float f = 0.0F; f <= 1.0F; f = (float) ((double) f + d0))
            {
                for (float f1 = 0.0F; f1 <= 1.0F; f1 = (float) ((double) f1 + d1))
                {
                    for (float f2 = 0.0F; f2 <= 1.0F; f2 = (float) ((double) f2 + d2))
                    {
                        double d5 = bb.minX + (bb.maxX - bb.minX) * (double) f;
                        double d6 = bb.minY + (bb.maxY - bb.minY) * (double) f1;
                        double d7 = bb.minZ + (bb.maxZ - bb.minZ) * (double) f2;

                        if (rayTraceBlocks(new Vec3d(d5 + d3, d6, d7 + d4), vec) == null) ++j2;

                        ++k2;
                    }
                }
            }

            return (float) j2 / (float) k2;
        }

        return 0;
    }

    public boolean extinguishFire(@Nullable EntityPlayer player, BlockPos pos, EnumFacing side)
    {
        pos = pos.offset(side);
        if (getBlockState(pos).getBlock() == Blocks.FIRE)
        {
            playEvent(player, 1009, pos, 0);
            setBlockToAir(pos);
            return true;
        }

        return false;
    }

    @SideOnly(Side.CLIENT)
    public String getDebugLoadedEntities()
    {
        return "All: " + loadedEntityList.size();
    }

    @SideOnly(Side.CLIENT)
    public String getProviderName()
    {
        return chunkProvider.makeString();
    }

    @Nullable
    public TileEntity getTileEntity(BlockPos pos)
    {
        if (isOutsideBuildHeight(pos)) return null;


        TileEntity tileentity2 = null;
        if (processingLoadedTiles) tileentity2 = getPendingTileEntityAt(pos);

        if (tileentity2 == null)
        {
            tileentity2 = getChunkFromBlockCoords(pos).getTileEntity(pos, Chunk.EnumCreateEntityType.IMMEDIATE);
        }

        if (tileentity2 == null)
        {
            tileentity2 = getPendingTileEntityAt(pos);
        }

        return tileentity2;
    }

    @Nullable
    private TileEntity getPendingTileEntityAt(BlockPos pos)
    {
        for (TileEntity tileentity2 : addedTileEntityList)
        {
            if (!tileentity2.isInvalid() && tileentity2.getPos().equals(pos)) return tileentity2;
        }

        return null;
    }

    public void setTileEntity(BlockPos pos, @Nullable TileEntity tileEntityIn)
    {
        pos = pos.toImmutable();
        if (!isOutsideBuildHeight(pos))
        {
            if (tileEntityIn != null && !tileEntityIn.isInvalid())
            {
                if (processingLoadedTiles)
                {
                    tileEntityIn.setPos(pos);
                    if (tileEntityIn.getWorld() != this) tileEntityIn.setWorld(this);
                    Iterator<TileEntity> iterator1 = addedTileEntityList.iterator();

                    while (iterator1.hasNext())
                    {
                        TileEntity tileentity2 = iterator1.next();

                        if (tileentity2.getPos().equals(pos))
                        {
                            tileentity2.invalidate();
                            iterator1.remove();
                        }
                    }

                    addedTileEntityList.add(tileEntityIn);
                }
                else
                {
                    Chunk chunk = getChunkFromBlockCoords(pos);
                    if (chunk != null) chunk.addTileEntity(pos, tileEntityIn);
                    addTileEntity(tileEntityIn);
                }
            }
        }
    }

    public void removeTileEntity(BlockPos pos)
    {
        TileEntity tileentity2 = getTileEntity(pos);

        if (tileentity2 != null && processingLoadedTiles)
        {
            tileentity2.invalidate();
            addedTileEntityList.remove(tileentity2);
            if (!(tileentity2 instanceof ITickable)) loadedTileEntityList.remove(tileentity2);
        }
        else
        {
            if (tileentity2 != null)
            {
                addedTileEntityList.remove(tileentity2);
                loadedTileEntityList.remove(tileentity2);
                tickableTileEntities.remove(tileentity2);
            }

            getChunkFromBlockCoords(pos).removeTileEntity(pos);
        }
        updateComparatorOutputLevel(pos, getBlockState(pos).getBlock());
    }

    public void markTileEntityForRemoval(TileEntity tileEntityIn)
    {
        tileEntitiesToBeRemoved.add(tileEntityIn);
    }

    public boolean isBlockFullCube(BlockPos pos)
    {
        AxisAlignedBB axisalignedbb = getBlockState(pos).getCollisionBoundingBox(this, pos);
        return axisalignedbb != Block.NULL_AABB && axisalignedbb.getAverageEdgeLength() >= 1.0D;
    }

    public boolean isBlockNormalCube(BlockPos pos, boolean _default)
    {
        if (isOutsideBuildHeight(pos)) return false;


        Chunk chunk1 = chunkProvider.getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk1 != null && !chunk1.isEmpty())
        {
            IBlockState iblockstate1 = getBlockState(pos);
            return iblockstate1.getBlock().isNormalCube(iblockstate1, this, pos);
        }

        return _default;
    }

    public void calculateInitialSkylight()
    {
        int j2 = calculateSkylightSubtracted(1.0F);
        if (j2 != skylightSubtracted) skylightSubtracted = j2;
    }

    public void setAllowedSpawnTypes(boolean hostile, boolean peaceful)
    {
        spawnHostileMobs = hostile;
        spawnPeacefulMobs = peaceful;
        provider.setAllowedSpawnTypes(hostile, peaceful);
    }

    public void tick()
    {
        updateWeather();
    }

    protected void calculateInitialWeather()
    {
        provider.calculateInitialWeather();
    }

    public void calculateInitialWeatherBody()
    {
        if (worldInfo.isRaining())
        {
            rainingStrength = 1.0F;
            if (worldInfo.isThundering()) thunderingStrength = 1.0F;
        }
    }

    protected void updateWeather()
    {
        provider.updateWeather();
    }

    public void updateWeatherBody()
    {
        if (provider.hasSkyLight())
        {
            if (!isRemote)
            {
                boolean flag = getGameRules().getBoolean("doWeatherCycle");

                if (flag)
                {
                    int j2 = worldInfo.getCleanWeatherTime();

                    if (j2 > 0)
                    {
                        --j2;
                        worldInfo.setCleanWeatherTime(j2);
                        worldInfo.setThunderTime(worldInfo.isThundering() ? 1 : 2);
                        worldInfo.setRainTime(worldInfo.isRaining() ? 1 : 2);
                    }

                    int k2 = worldInfo.getThunderTime();

                    if (k2 <= 0)
                    {
                        if (worldInfo.isThundering())
                        {
                            worldInfo.setThunderTime(rand.nextInt(12000) + 3600);
                        }
                        else
                        {
                            worldInfo.setThunderTime(rand.nextInt(168000) + 12000);
                        }
                    }
                    else
                    {
                        --k2;
                        worldInfo.setThunderTime(k2);

                        if (k2 <= 0)
                        {
                            worldInfo.setThundering(!worldInfo.isThundering());
                        }
                    }

                    int l2 = worldInfo.getRainTime();

                    if (l2 <= 0)
                    {
                        if (worldInfo.isRaining())
                        {
                            worldInfo.setRainTime(rand.nextInt(12000) + 12000);
                        }
                        else
                        {
                            worldInfo.setRainTime(rand.nextInt(168000) + 12000);
                        }
                    }
                    else
                    {
                        --l2;
                        worldInfo.setRainTime(l2);
                        if (l2 <= 0) worldInfo.setRaining(!worldInfo.isRaining());
                    }
                }

                prevThunderingStrength = thunderingStrength;

                if (worldInfo.isThundering())
                {
                    thunderingStrength = (float) ((double) thunderingStrength + 0.01D);
                }
                else
                {
                    thunderingStrength = (float) ((double) thunderingStrength - 0.01D);
                }

                thunderingStrength = MathHelper.clamp(thunderingStrength, 0.0F, 1.0F);
                prevRainingStrength = rainingStrength;

                if (worldInfo.isRaining())
                {
                    rainingStrength = (float) ((double) rainingStrength + 0.01D);
                }
                else
                {
                    rainingStrength = (float) ((double) rainingStrength - 0.01D);
                }

                rainingStrength = MathHelper.clamp(rainingStrength, 0.0F, 1.0F);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    protected void playMoodSoundAndCheckLight(int p_147467_1_, int p_147467_2_, Chunk chunkIn)
    {
        chunkIn.enqueueRelightChecks();
    }

    protected void updateBlocks()
    {
    }

    public void immediateBlockTick(BlockPos pos, IBlockState state, Random random)
    {
        scheduledUpdatesAreImmediate = true;
        state.getBlock().updateTick(this, pos, state, random);
        scheduledUpdatesAreImmediate = false;
    }

    public boolean canBlockFreezeWater(BlockPos pos)
    {
        return canBlockFreeze(pos, false);
    }

    public boolean canBlockFreezeNoWater(BlockPos pos)
    {
        return canBlockFreeze(pos, true);
    }

    public boolean canBlockFreeze(BlockPos pos, boolean noWaterAdj)
    {
        return provider.canBlockFreeze(pos, noWaterAdj);
    }

    public boolean canBlockFreezeBody(BlockPos pos, boolean noWaterAdj)
    {
        Biome biome = getBiome(pos);
        float f = biome.getTemperature(pos);

        if (f >= 0.15F) return false;

        if (pos.getY() >= 0 && pos.getY() < 256 && getLightFor(EnumSkyBlock.BLOCK, pos) < 10)
        {
            IBlockState iblockstate1 = getBlockState(pos);
            Block block = iblockstate1.getBlock();

            if ((block == Blocks.WATER || block == Blocks.FLOWING_WATER) && ((Integer) iblockstate1.getValue(BlockLiquid.LEVEL)).intValue() == 0)
            {
                if (!noWaterAdj) return true;

                boolean flag = isWater(pos.west()) && isWater(pos.east()) && isWater(pos.north()) && isWater(pos.south());
                return !flag;
            }
        }

        return false;
    }

    private boolean isWater(BlockPos pos)
    {
        return getBlockState(pos).getMaterial() == Material.WATER;
    }

    public boolean canSnowAt(BlockPos pos, boolean checkLight)
    {
        return provider.canSnowAt(pos, checkLight);
    }

    public boolean canSnowAtBody(BlockPos pos, boolean checkLight)
    {
        Biome biome = getBiome(pos);
        float f = biome.getTemperature(pos);

        if (f >= 0.15F) return false;
        if (!checkLight) return true;


        if (pos.getY() >= 0 && pos.getY() < 256 && getLightFor(EnumSkyBlock.BLOCK, pos) < 10)
        {
            IBlockState iblockstate1 = getBlockState(pos);
            return iblockstate1.getBlock().isAir(iblockstate1, this, pos) && Blocks.SNOW_LAYER.canPlaceBlockAt(this, pos);
        }

        return false;
    }

    public boolean checkLight(BlockPos pos)
    {
        boolean flag = false;
        if (provider.hasSkyLight()) flag = checkLightFor(EnumSkyBlock.SKY, pos);
        return flag | checkLightFor(EnumSkyBlock.BLOCK, pos);
    }

    private int getRawLight(BlockPos pos, EnumSkyBlock lightType)
    {
        if (lightType == EnumSkyBlock.SKY && canSeeSky(pos)) return 15;
        else
        {
            IBlockState iblockstate1 = getBlockState(pos);
            int j2 = lightType == EnumSkyBlock.SKY ? 0 : iblockstate1.getBlock().getLightValue(iblockstate1, this, pos);
            int k2 = iblockstate1.getBlock().getLightOpacity(iblockstate1, this, pos);

            if (k2 < 1) k2 = 1;

            if (k2 >= 15) return j2;
            if (j2 >= 14) return j2;


            BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();

            try
            {
                for (EnumFacing enumfacing : EnumFacing.values())
                {
                    blockpos$pooledmutableblockpos.setPos(pos).move(enumfacing);
                    int l2 = getLightFor(lightType, blockpos$pooledmutableblockpos) - k2;

                    if (l2 > j2) j2 = l2;

                    if (j2 >= 14) return j2;
                }

                return j2;
            }
            finally
            {
                blockpos$pooledmutableblockpos.release();
            }

        }
    }

    public boolean checkLightFor(EnumSkyBlock lightType, BlockPos pos)
    {
        if (!isAreaLoaded(pos, 16, false)) return false;


        int updateRange = isAreaLoaded(pos, 18, false) ? 17 : 15;
        int j2 = 0;
        int k2 = 0;
        profiler.startSection("getBrightness");
        int l2 = getLightFor(lightType, pos);
        int i3 = getRawLight(pos, lightType);
        int j3 = pos.getX();
        int k3 = pos.getY();
        int l3 = pos.getZ();

        if (i3 > l2)
        {
            lightUpdateBlockList[k2++] = 133152;
        }
        else if (i3 < l2)
        {
            lightUpdateBlockList[k2++] = 133152 | l2 << 18;

            while (j2 < k2)
            {
                int i4 = lightUpdateBlockList[j2++];
                int j4 = (i4 & 63) - 32 + j3;
                int k4 = (i4 >> 6 & 63) - 32 + k3;
                int l4 = (i4 >> 12 & 63) - 32 + l3;
                int i5 = i4 >> 18 & 15;
                BlockPos blockpos1 = new BlockPos(j4, k4, l4);
                int j5 = getLightFor(lightType, blockpos1);

                if (j5 == i5)
                {
                    setLightFor(lightType, blockpos1, 0);

                    if (i5 > 0)
                    {
                        int k5 = MathHelper.abs(j4 - j3);
                        int l5 = MathHelper.abs(k4 - k3);
                        int i6 = MathHelper.abs(l4 - l3);

                        if (k5 + l5 + i6 < updateRange)
                        {
                            BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();

                            for (EnumFacing enumfacing : EnumFacing.values())
                            {
                                int j6 = j4 + enumfacing.getFrontOffsetX();
                                int k6 = k4 + enumfacing.getFrontOffsetY();
                                int l6 = l4 + enumfacing.getFrontOffsetZ();
                                blockpos$pooledmutableblockpos.setPos(j6, k6, l6);
                                IBlockState bs = getBlockState(blockpos$pooledmutableblockpos);
                                int i7 = Math.max(1, bs.getBlock().getLightOpacity(bs, this, blockpos$pooledmutableblockpos));
                                j5 = getLightFor(lightType, blockpos$pooledmutableblockpos);

                                if (j5 == i5 - i7 && k2 < lightUpdateBlockList.length)
                                {
                                    lightUpdateBlockList[k2++] = j6 - j3 + 32 | k6 - k3 + 32 << 6 | l6 - l3 + 32 << 12 | i5 - i7 << 18;
                                }
                            }

                            blockpos$pooledmutableblockpos.release();
                        }
                    }
                }
            }

            j2 = 0;
        }

        profiler.endSection();
        profiler.startSection("checkedPosition < toCheckCount");

        while (j2 < k2)
        {
            int j7 = lightUpdateBlockList[j2++];
            int k7 = (j7 & 63) - 32 + j3;
            int l7 = (j7 >> 6 & 63) - 32 + k3;
            int i8 = (j7 >> 12 & 63) - 32 + l3;
            BlockPos blockpos2 = new BlockPos(k7, l7, i8);
            int j8 = getLightFor(lightType, blockpos2);
            int k8 = getRawLight(blockpos2, lightType);

            if (k8 != j8)
            {
                setLightFor(lightType, blockpos2, k8);

                if (k8 > j8)
                {
                    int l8 = Math.abs(k7 - j3);
                    int i9 = Math.abs(l7 - k3);
                    int j9 = Math.abs(i8 - l3);
                    boolean flag = k2 < lightUpdateBlockList.length - 6;

                    if (l8 + i9 + j9 < updateRange && flag)
                    {
                        if (getLightFor(lightType, blockpos2.west()) < k8)
                        {
                            lightUpdateBlockList[k2++] = k7 - 1 - j3 + 32 + (l7 - k3 + 32 << 6) + (i8 - l3 + 32 << 12);
                        }

                        if (getLightFor(lightType, blockpos2.east()) < k8)
                        {
                            lightUpdateBlockList[k2++] = k7 + 1 - j3 + 32 + (l7 - k3 + 32 << 6) + (i8 - l3 + 32 << 12);
                        }

                        if (getLightFor(lightType, blockpos2.down()) < k8)
                        {
                            lightUpdateBlockList[k2++] = k7 - j3 + 32 + (l7 - 1 - k3 + 32 << 6) + (i8 - l3 + 32 << 12);
                        }

                        if (getLightFor(lightType, blockpos2.up()) < k8)
                        {
                            lightUpdateBlockList[k2++] = k7 - j3 + 32 + (l7 + 1 - k3 + 32 << 6) + (i8 - l3 + 32 << 12);
                        }

                        if (getLightFor(lightType, blockpos2.north()) < k8)
                        {
                            lightUpdateBlockList[k2++] = k7 - j3 + 32 + (l7 - k3 + 32 << 6) + (i8 - 1 - l3 + 32 << 12);
                        }

                        if (getLightFor(lightType, blockpos2.south()) < k8)
                        {
                            lightUpdateBlockList[k2++] = k7 - j3 + 32 + (l7 - k3 + 32 << 6) + (i8 + 1 - l3 + 32 << 12);
                        }
                    }
                }
            }
        }

        profiler.endSection();
        return true;
    }

    public boolean tickUpdates(boolean runAllPending)
    {
        return false;
    }

    @Nullable
    public List<NextTickListEntry> getPendingBlockUpdates(Chunk chunkIn, boolean remove)
    {
        return null;
    }

    @Nullable
    public List<NextTickListEntry> getPendingBlockUpdates(StructureBoundingBox structureBB, boolean remove)
    {
        return null;
    }

    public List<Entity> getEntitiesWithinAABBExcludingEntity(@Nullable Entity entityIn, AxisAlignedBB bb)
    {
        return getEntitiesInAABBexcluding(entityIn, bb, EntitySelectors.NOT_SPECTATING);
    }

    public List<Entity> getEntitiesInAABBexcluding(@Nullable Entity entityIn, AxisAlignedBB boundingBox, @Nullable Predicate<? super Entity> predicate)
    {
        List<Entity> list = Lists.<Entity>newArrayList();
        int j2 = MathHelper.floor((boundingBox.minX - MAX_ENTITY_RADIUS) / 16.0D);
        int k2 = MathHelper.floor((boundingBox.maxX + MAX_ENTITY_RADIUS) / 16.0D);
        int l2 = MathHelper.floor((boundingBox.minZ - MAX_ENTITY_RADIUS) / 16.0D);
        int i3 = MathHelper.floor((boundingBox.maxZ + MAX_ENTITY_RADIUS) / 16.0D);

        for (int j3 = j2; j3 <= k2; ++j3)
        {
            for (int k3 = l2; k3 <= i3; ++k3)
            {
                if (isChunkLoaded(j3, k3, true))
                {
                    getChunkFromChunkCoords(j3, k3).getEntitiesWithinAABBForEntity(entityIn, boundingBox, list, predicate);
                }
            }
        }

        return list;
    }

    public <T extends Entity> List<T> getEntities(Class<? extends T> entityType, Predicate<? super T> filter)
    {
        List<T> list = Lists.newArrayList();

        for (Entity entity4 : loadedEntityList)
        {
            if (entityType.isAssignableFrom(entity4.getClass()) && filter.apply((T) entity4))
            {
                list.add((T) entity4);
            }
        }

        return list;
    }

    public <T extends Entity> List<T> getPlayers(Class<? extends T> playerType, Predicate<? super T> filter)
    {
        List<T> list = Lists.newArrayList();

        for (Entity entity4 : playerEntities)
        {
            if (playerType.isAssignableFrom(entity4.getClass()) && filter.apply((T) entity4))
            {
                list.add((T) entity4);
            }
        }

        return list;
    }

    public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> classEntity, AxisAlignedBB bb)
    {
        return getEntitiesWithinAABB(classEntity, bb, EntitySelectors.NOT_SPECTATING);
    }

    public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> clazz, AxisAlignedBB aabb, @Nullable Predicate<? super T> filter)
    {
        int j2 = MathHelper.floor((aabb.minX - MAX_ENTITY_RADIUS) / 16);
        int k2 = MathHelper.ceil((aabb.maxX + MAX_ENTITY_RADIUS) / 16);
        int l2 = MathHelper.floor((aabb.minZ - MAX_ENTITY_RADIUS) / 16);
        int i3 = MathHelper.ceil((aabb.maxZ + MAX_ENTITY_RADIUS) / 16);
        List<T> list = Lists.newArrayList();

        for (int j3 = j2; j3 < k2; ++j3)
        {
            for (int k3 = l2; k3 < i3; ++k3)
            {
                if (isChunkLoaded(j3, k3, true))
                {
                    getChunkFromChunkCoords(j3, k3).getEntitiesOfTypeWithinAABB(clazz, aabb, list, filter);
                }
            }
        }

        return list;
    }

    @Nullable
    public <T extends Entity> T findNearestEntityWithinAABB(Class<? extends T> entityType, AxisAlignedBB aabb, T closestTo)
    {
        List<T> list = getEntitiesWithinAABB(entityType, aabb);
        T t = null;
        double d0 = Double.MAX_VALUE;

        for (int j2 = 0; j2 < list.size(); ++j2)
        {
            T t1 = list.get(j2);

            if (t1 != closestTo && EntitySelectors.NOT_SPECTATING.apply(t1))
            {
                double d1 = closestTo.getDistanceSq(t1);

                if (d1 <= d0)
                {
                    t = t1;
                    d0 = d1;
                }
            }
        }

        return t;
    }

    @Nullable
    public Entity getEntityByID(int id)
    {
        return entitiesById.lookup(id);
    }

    @SideOnly(Side.CLIENT)
    public List<Entity> getLoadedEntityList()
    {
        return loadedEntityList;
    }

    public void markChunkDirty(BlockPos pos, TileEntity unusedTileEntity)
    {
        if (isBlockLoaded(pos)) getChunkFromBlockCoords(pos).markDirty();
    }

    public int countEntities(Class<?> entityType)
    {
        int j2 = 0;

        for (Entity entity4 : loadedEntityList)
        {
            if ((!(entity4 instanceof EntityLiving) || !((EntityLiving) entity4).isNoDespawnRequired()) && entityType.isAssignableFrom(entity4.getClass()))
            {
                ++j2;
            }
        }

        return j2;
    }

    public void loadEntities(Collection<Entity> entityCollection)
    {
        for (Entity entity4 : entityCollection)
        {
            if (!net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.EntityJoinWorldEvent(entity4, this)))
            {
                loadedEntityList.add(entity4);
                onEntityAdded(entity4);
            }
        }
    }

    public void unloadEntities(Collection<Entity> entityCollection)
    {
        unloadedEntityList.addAll(entityCollection);
    }

    public boolean mayPlace(Block blockIn, BlockPos pos, boolean skipCollisionCheck, EnumFacing sidePlacedOn, @Nullable Entity placer)
    {
        IBlockState iblockstate1 = getBlockState(pos);
        AxisAlignedBB axisalignedbb = skipCollisionCheck ? null : blockIn.getDefaultState().getCollisionBoundingBox(this, pos);

        if (axisalignedbb != Block.NULL_AABB && !checkNoEntityCollision(axisalignedbb.offset(pos), placer)) return false;
        if (iblockstate1.getMaterial() == Material.CIRCUITS && blockIn == Blocks.ANVIL) return true;
        return iblockstate1.getBlock().isReplaceable(this, pos) && blockIn.canPlaceBlockOnSide(this, pos, sidePlacedOn);
    }

    public int getSeaLevel()
    {
        return seaLevel;
    }

    public void setSeaLevel(int seaLevelIn)
    {
        seaLevel = seaLevelIn;
    }

    public int getStrongPower(BlockPos pos, EnumFacing direction)
    {
        return getBlockState(pos).getStrongPower(this, pos, direction);
    }

    public WorldType getWorldType()
    {
        return worldInfo.getTerrainType();
    }

    public int getStrongPower(BlockPos pos)
    {
        int j2 = 0;
        j2 = Math.max(j2, getStrongPower(pos.down(), EnumFacing.DOWN));

        if (j2 >= 15)
        {
            return j2;
        }
        else
        {
            j2 = Math.max(j2, getStrongPower(pos.up(), EnumFacing.UP));

            if (j2 >= 15)
            {
                return j2;
            }
            else
            {
                j2 = Math.max(j2, getStrongPower(pos.north(), EnumFacing.NORTH));

                if (j2 >= 15)
                {
                    return j2;
                }
                else
                {
                    j2 = Math.max(j2, getStrongPower(pos.south(), EnumFacing.SOUTH));

                    if (j2 >= 15)
                    {
                        return j2;
                    }
                    else
                    {
                        j2 = Math.max(j2, getStrongPower(pos.west(), EnumFacing.WEST));

                        if (j2 >= 15)
                        {
                            return j2;
                        }
                        else
                        {
                            j2 = Math.max(j2, getStrongPower(pos.east(), EnumFacing.EAST));
                            return j2 >= 15 ? j2 : j2;
                        }
                    }
                }
            }
        }
    }

    public boolean isSidePowered(BlockPos pos, EnumFacing side)
    {
        return getRedstonePower(pos, side) > 0;
    }

    public int getRedstonePower(BlockPos pos, EnumFacing facing)
    {
        IBlockState iblockstate1 = getBlockState(pos);
        return iblockstate1.getBlock().shouldCheckWeakPower(iblockstate1, this, pos, facing) ? getStrongPower(pos) : iblockstate1.getWeakPower(this, pos, facing);
    }

    public boolean isBlockPowered(BlockPos pos)
    {
        if (getRedstonePower(pos.down(), EnumFacing.DOWN) > 0) return true;
        if (getRedstonePower(pos.up(), EnumFacing.UP) > 0) return true;
        if (getRedstonePower(pos.north(), EnumFacing.NORTH) > 0) return true;
        if (getRedstonePower(pos.south(), EnumFacing.SOUTH) > 0) return true;
        if (getRedstonePower(pos.west(), EnumFacing.WEST) > 0) return true;
        return getRedstonePower(pos.east(), EnumFacing.EAST) > 0;
    }

    public int isBlockIndirectlyGettingPowered(BlockPos pos)
    {
        int maxPower = 0;

        for (EnumFacing enumfacing : EnumFacing.values())
        {
            int power = getRedstonePower(pos.offset(enumfacing), enumfacing);
            if (power >= 15) return 15;

            if (power > maxPower) maxPower = power;
        }

        return maxPower;
    }

    @Nullable
    public EntityPlayer getClosestPlayerToEntity(Entity entityIn, double distance)
    {
        return getClosestPlayer(entityIn.posX, entityIn.posY, entityIn.posZ, distance, false);
    }

    @Nullable
    public EntityPlayer getNearestPlayerNotCreative(Entity entityIn, double distance)
    {
        return getClosestPlayer(entityIn.posX, entityIn.posY, entityIn.posZ, distance, true);
    }

    @Nullable
    public EntityPlayer getClosestPlayer(double posX, double posY, double posZ, double distance, boolean spectator)
    {
        return getClosestPlayer(posX, posY, posZ, distance, spectator ? EntitySelectors.CAN_AI_TARGET : EntitySelectors.NOT_SPECTATING);
    }

    @Nullable
    public EntityPlayer getClosestPlayer(double x, double y, double z, double p_190525_7_, Predicate<Entity> predicate)
    {
        double d0 = -1.0D;
        EntityPlayer entityplayer = null;

        for (EntityPlayer entityplayer1 : playerEntities)
        {
            if (predicate.apply(entityplayer1))
            {
                double d1 = entityplayer1.getDistanceSq(x, y, z);

                if ((p_190525_7_ < 0.0D || d1 < p_190525_7_ * p_190525_7_) && (d0 == -1.0D || d1 < d0))
                {
                    d0 = d1;
                    entityplayer = entityplayer1;
                }
            }
        }

        return entityplayer;
    }

    public boolean isAnyPlayerWithinRangeAt(double x, double y, double z, double range)
    {
        for (EntityPlayer entityplayer : playerEntities)
        {
            if (EntitySelectors.NOT_SPECTATING.apply(entityplayer))
            {
                double d0 = entityplayer.getDistanceSq(x, y, z);
                if (range < 0.0D || d0 < range * range) return true;
            }
        }

        return false;
    }

    @Nullable
    public EntityPlayer getNearestAttackablePlayer(Entity entityIn, double maxXZDistance, double maxYDistance)
    {
        return getNearestAttackablePlayer(entityIn.posX, entityIn.posY, entityIn.posZ, maxXZDistance, maxYDistance, null, null);
    }

    @Nullable
    public EntityPlayer getNearestAttackablePlayer(BlockPos pos, double maxXZDistance, double maxYDistance)
    {
        return getNearestAttackablePlayer((double) ((float) pos.getX() + 0.5F), (double) ((float) pos.getY() + 0.5F), (double) ((float) pos.getZ() + 0.5F), maxXZDistance, maxYDistance, null, null);
    }

    @Nullable
    public EntityPlayer getNearestAttackablePlayer(double posX, double posY, double posZ, double maxXZDistance, double maxYDistance, @Nullable Function<EntityPlayer, Double> function, @Nullable Predicate<EntityPlayer> predicate)
    {
        double d0 = -1;
        EntityPlayer entityplayer = null;

        for (int j2 = 0; j2 < playerEntities.size(); ++j2)
        {
            EntityPlayer entityplayer1 = playerEntities.get(j2);

            if (!entityplayer1.capabilities.disableDamage && entityplayer1.isEntityAlive() && !entityplayer1.isSpectator() && (predicate == null || predicate.apply(entityplayer1)))
            {
                double d1 = entityplayer1.getDistanceSq(posX, entityplayer1.posY, posZ);
                double d2 = maxXZDistance;

                if (entityplayer1.isSneaking())
                {
                    d2 = maxXZDistance * 0.800000011920929D;
                }

                if (entityplayer1.isInvisible())
                {
                    float f = entityplayer1.getArmorVisibility();

                    if (f < 0.1F)
                    {
                        f = 0.1F;
                    }

                    d2 *= (double) (0.7F * f);
                }

                if (function != null)
                {
                    d2 *= MoreObjects.firstNonNull(function.apply(entityplayer1), 1).doubleValue();
                }

                d2 = net.minecraftforge.common.ForgeHooks.getPlayerVisibilityDistance(entityplayer1, d2, maxYDistance);

                if ((maxYDistance < 0.0D || Math.abs(entityplayer1.posY - posY) < maxYDistance * maxYDistance) && (maxXZDistance < 0.0D || d1 < d2 * d2) && (d0 == -1.0D || d1 < d0))
                {
                    d0 = d1;
                    entityplayer = entityplayer1;
                }
            }
        }

        return entityplayer;
    }

    @Nullable
    public EntityPlayer getPlayerEntityByName(String name)
    {
        for (EntityPlayer entityplayer : playerEntities)
        {
            if (name.equals(entityplayer.getName())) return entityplayer;
        }

        return null;
    }

    @Nullable
    public EntityPlayer getPlayerEntityByUUID(UUID uuid)
    {
        for (EntityPlayer entityplayer : playerEntities)
        {
            if (uuid.equals(entityplayer.getUniqueID())) return entityplayer;
        }

        return null;
    }

    @SideOnly(Side.CLIENT)
    public void sendQuittingDisconnectingPacket()
    {
    }

    public void checkSessionLock() throws MinecraftException
    {
        saveHandler.checkSessionLock();
    }

    @SideOnly(Side.CLIENT)
    public void setTotalWorldTime(long worldTime)
    {
        worldInfo.setWorldTotalTime(worldTime);
    }

    public long getSeed()
    {
        return provider.getSeed();
    }

    public long getTotalWorldTime()
    {
        return worldInfo.getWorldTotalTime();
    }

    public long getWorldTime()
    {
        return provider.getWorldTime();
    }

    public void setWorldTime(long time)
    {
        provider.setWorldTime(time);
    }

    public BlockPos getSpawnPoint()
    {
        BlockPos blockpos1 = provider.getSpawnPoint();

        if (!getWorldBorder().contains(blockpos1))
        {
            blockpos1 = getHeight(new BlockPos(getWorldBorder().getCenterX(), 0.0D, getWorldBorder().getCenterZ()));
        }

        return blockpos1;
    }

    public void setSpawnPoint(BlockPos pos)
    {
        provider.setSpawnPoint(pos);
    }

    @SideOnly(Side.CLIENT)
    public void joinEntityInSurroundings(Entity entityIn)
    {
        int j2 = MathHelper.floor(entityIn.posX / 16);
        int k2 = MathHelper.floor(entityIn.posZ / 16);

        for (int i3 = -2; i3 <= 2; ++i3)
        {
            for (int j3 = -2; j3 <= 2; ++j3)
            {
                getChunkFromChunkCoords(j2 + i3, k2 + j3);
            }
        }

        if (!loadedEntityList.contains(entityIn))
        {
            if (!net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.EntityJoinWorldEvent(entityIn, this)))
            {
                loadedEntityList.add(entityIn);
            }
        }
    }

    public boolean isBlockModifiable(EntityPlayer player, BlockPos pos)
    {
        return provider.canMineBlock(player, pos);
    }

    public boolean canMineBlockBody(EntityPlayer player, BlockPos pos)
    {
        return true;
    }

    public void setEntityState(Entity entityIn, byte state)
    {
    }

    public IChunkProvider getChunkProvider()
    {
        return chunkProvider;
    }

    public void addBlockEvent(BlockPos pos, Block blockIn, int eventID, int eventParam)
    {
        getBlockState(pos).onBlockEventReceived(this, pos, eventID, eventParam);
    }

    public ISaveHandler getSaveHandler()
    {
        return saveHandler;
    }

    public WorldInfo getWorldInfo()
    {
        return worldInfo;
    }

    public GameRules getGameRules()
    {
        return worldInfo.getGameRulesInstance();
    }

    public void updateAllPlayersSleepingFlag()
    {
    }

    public float getThunderStrength(float delta)
    {
        return (prevThunderingStrength + (thunderingStrength - prevThunderingStrength) * delta) * getRainStrength(delta);
    }

    @SideOnly(Side.CLIENT)
    public void setThunderStrength(float strength)
    {
        prevThunderingStrength = strength;
        thunderingStrength = strength;
    }

    public float getRainStrength(float delta)
    {
        return prevRainingStrength + (rainingStrength - prevRainingStrength) * delta;
    }

    @SideOnly(Side.CLIENT)
    public void setRainStrength(float strength)
    {
        prevRainingStrength = strength;
        rainingStrength = strength;
    }

    public boolean isThundering()
    {
        return getThunderStrength(1) > 0.9;
    }

    public boolean isRaining()
    {
        return getRainStrength(1) > 0.2;
    }

    public boolean isRainingAt(BlockPos position)
    {
        if (!isRaining()) return false;
        if (!canSeeSky(position)) return false;
        if (getPrecipitationHeight(position).getY() > position.getY()) return false;

        Biome biome = getBiome(position);
        if (biome.getEnableSnow()) return false;
        return !canSnowAt(position, false) && biome.canRain();
    }

    public boolean isBlockinHighHumidity(BlockPos pos)
    {
        return provider.isBlockHighHumidity(pos);
    }

    @Nullable
    public MapStorage getMapStorage()
    {
        return mapStorage;
    }

    public void setData(String dataID, WorldSavedData worldSavedDataIn)
    {
        mapStorage.setData(dataID, worldSavedDataIn);
    }

    @Nullable
    public WorldSavedData loadData(Class<? extends WorldSavedData> clazz, String dataID)
    {
        return mapStorage.getOrLoadData(clazz, dataID);
    }

    public int getUniqueDataId(String key)
    {
        return mapStorage.getUniqueDataId(key);
    }

    public void playBroadcastSound(int id, BlockPos pos, int data)
    {
        for (IWorldEventListener eventListener : eventListeners) eventListener.broadcastSound(id, pos, data);
    }

    public void playEvent(int type, BlockPos pos, int data)
    {
        playEvent(null, type, pos, data);
    }

    public void playEvent(@Nullable EntityPlayer player, int type, BlockPos pos, int data)
    {
        try
        {
            for (IWorldEventListener eventListener : eventListeners) eventListener.playEvent(player, type, pos, data);
        }
        catch (Throwable throwable3)
        {
            CrashReport crashreport3 = CrashReport.makeCrashReport(throwable3, "Playing level event");
            CrashReportCategory crashreportcategory3 = crashreport3.makeCategory("Level event being played");
            crashreportcategory3.addCrashSection("Block coordinates", CrashReportCategory.getCoordinateInfo(pos));
            crashreportcategory3.addCrashSection("Event source", player);
            crashreportcategory3.addCrashSection("Event type", type);
            crashreportcategory3.addCrashSection("Event data", data);
            throw new ReportedException(crashreport3);
        }
    }

    public int getHeight()
    {
        return provider.getHeight();
    }

    public int getActualHeight()
    {
        return provider.getActualHeight();
    }

    public Random setRandomSeed(int p_72843_1_, int p_72843_2_, int p_72843_3_)
    {
        long j2 = (long) p_72843_1_ * 341873128712L + (long) p_72843_2_ * 132897987541L + getWorldInfo().getSeed() + (long) p_72843_3_;
        rand.setSeed(j2);
        return rand;
    }

    public CrashReportCategory addWorldInfoToCrashReport(CrashReport report)
    {
        CrashReportCategory crashreportcategory3 = report.makeCategoryDepth("Affected level", 1);
        crashreportcategory3.addCrashSection("Level name", worldInfo == null ? "????" : worldInfo.getWorldName());
        crashreportcategory3.addDetail("All players", () -> playerEntities.size() + " total; " + playerEntities);
        crashreportcategory3.addDetail("Chunk stats", () -> chunkProvider.makeString());

        try
        {
            worldInfo.addToCrashReport(crashreportcategory3);
        }
        catch (Throwable throwable3)
        {
            crashreportcategory3.addCrashSectionThrowable("Level Data Unobtainable", throwable3);
        }

        return crashreportcategory3;
    }

    @SideOnly(Side.CLIENT)
    public double getHorizon()
    {
        return provider.getHorizon();
    }

    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress)
    {
        for (IWorldEventListener iworldeventlistener : eventListeners) iworldeventlistener.sendBlockBreakProgress(breakerId, pos, progress);
    }

    public Calendar getCurrentDate()
    {
        if (getTotalWorldTime() % 600L == 0L) calendar.setTimeInMillis(MinecraftServer.getCurrentTimeMillis());
        return calendar;
    }

    @SideOnly(Side.CLIENT)
    public void makeFireworks(double x, double y, double z, double motionX, double motionY, double motionZ, @Nullable NBTTagCompound compound)
    {
    }

    public Scoreboard getScoreboard()
    {
        return worldScoreboard;
    }

    public void updateComparatorOutputLevel(BlockPos pos, Block blockIn)
    {
        for (EnumFacing enumfacing : EnumFacing.VALUES)
        {
            BlockPos blockpos1 = pos.offset(enumfacing);

            if (isBlockLoaded(blockpos1))
            {
                IBlockState iblockstate1 = getBlockState(blockpos1);

                iblockstate1.getBlock().onNeighborChange(this, blockpos1, pos);
                if (iblockstate1.getBlock().isNormalCube(iblockstate1, this, blockpos1))
                {
                    blockpos1 = blockpos1.offset(enumfacing);
                    iblockstate1 = getBlockState(blockpos1);

                    if (iblockstate1.getBlock().getWeakChanges(this, blockpos1))
                    {
                        iblockstate1.getBlock().onNeighborChange(this, blockpos1, pos);
                    }
                }
            }
        }
    }

    public DifficultyInstance getDifficultyForLocation(BlockPos pos)
    {
        long j2 = 0;
        float f = 0;

        if (isBlockLoaded(pos))
        {
            f = getCurrentMoonPhaseFactor();
            j2 = getChunkFromBlockCoords(pos).getInhabitedTime();
        }

        return new DifficultyInstance(getDifficulty(), getWorldTime(), j2, f);
    }

    public EnumDifficulty getDifficulty()
    {
        return getWorldInfo().getDifficulty();
    }

    public int getSkylightSubtracted()
    {
        return skylightSubtracted;
    }

    public void setSkylightSubtracted(int newSkylightSubtracted)
    {
        skylightSubtracted = newSkylightSubtracted;
    }

    @SideOnly(Side.CLIENT)
    public int getLastLightningBolt()
    {
        return lastLightningBolt;
    }

    public void setLastLightningBolt(int lastLightningBoltIn)
    {
        lastLightningBolt = lastLightningBoltIn;
    }

    public VillageCollection getVillageCollection()
    {
        return villageCollection;
    }

    public WorldBorder getWorldBorder()
    {
        return worldBorder;
    }

    public boolean isSpawnChunk(int x, int z)
    {
        BlockPos blockpos1 = getSpawnPoint();
        int j2 = x * 16 + 8 - blockpos1.getX();
        int k2 = z * 16 + 8 - blockpos1.getZ();
        return j2 >= -128 && j2 <= 128 && k2 >= -128 && k2 <= 128;
    }

    public boolean isSideSolid(BlockPos pos, EnumFacing side)
    {
        return isSideSolid(pos, side, false);
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default)
    {
        if (!isValid(pos)) return _default;

        Chunk chunk = getChunkFromBlockCoords(pos);
        if (chunk == null || chunk.isEmpty()) return _default;
        return getBlockState(pos).isSideSolid(this, pos, side);
    }

    public com.google.common.collect.ImmutableSetMultimap<net.minecraft.util.math.ChunkPos, net.minecraftforge.common.ForgeChunkManager.Ticket> getPersistentChunks()
    {
        return net.minecraftforge.common.ForgeChunkManager.getPersistentChunksFor(this);
    }

    public Iterator<Chunk> getPersistentChunkIterable(Iterator<Chunk> chunkIterator)
    {
        return net.minecraftforge.common.ForgeChunkManager.getPersistentChunksIterableFor(this, chunkIterator);
    }

    public int getBlockLightOpacity(BlockPos pos)
    {
        if (!isValid(pos)) return 0;
        return getChunkFromBlockCoords(pos).getBlockLightOpacity(pos);
    }

    public int countEntities(net.minecraft.entity.EnumCreatureType type, boolean forSpawnCount)
    {
        int count = 0;
        for (Entity aLoadedEntityList : loadedEntityList)
        {
            if (aLoadedEntityList.isCreatureType(type, forSpawnCount)) count++;
        }
        return count;
    }

    public void markTileEntitiesInChunkForRemoval(Chunk chunk)
    {
        for (TileEntity tileentity : chunk.getTileEntityMap().values()) markTileEntityForRemoval(tileentity);
    }

    protected void initCapabilities()
    {
        net.minecraftforge.common.capabilities.ICapabilityProvider parent = provider.initCapabilities();
        capabilities = net.minecraftforge.event.ForgeEventFactory.gatherCapabilities(this, parent);
        net.minecraftforge.common.util.WorldCapabilityData data = (net.minecraftforge.common.util.WorldCapabilityData) perWorldStorage.getOrLoadData(net.minecraftforge.common.util.WorldCapabilityData.class, net.minecraftforge.common.util.WorldCapabilityData.ID);
        if (data == null)
        {
            capabilityData = new net.minecraftforge.common.util.WorldCapabilityData(capabilities);
            perWorldStorage.setData(capabilityData.mapName, capabilityData);
        }
        else
        {
            capabilityData = data;
            capabilityData.setCapabilities(provider, capabilities);
        }
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

    protected MapStorage perWorldStorage; //Moved to a getter to simulate final without being final so we can load in subclasses.

    public MapStorage getPerWorldStorage()
    {
        return perWorldStorage;
    }

    public void sendPacketToServer(Packet<?> packetIn)
    {
        throw new UnsupportedOperationException("Can't send packets to server unless you're on the client.");
    }

    public LootTableManager getLootTableManager()
    {
        return lootTable;
    }

    @Nullable
    public BlockPos findNearestStructure(String p_190528_1_, BlockPos p_190528_2_, boolean p_190528_3_)
    {
        return null;
    }
}