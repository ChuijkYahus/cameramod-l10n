package net.mehvahdjukaar.vista.client.renderer;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.network.ServerBoundCameraChunkRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CameraChunkLoadingManager {
    
    private static final Map<BlockPos, CameraChunkRequest> ACTIVE_CAMERAS = new Object2ObjectOpenHashMap<>();
    private static final int UPDATE_INTERVAL = 40;
    private static final int CHUNK_RADIUS = 4;
    
    private static int tickCounter = 0;
    
    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        
        tickCounter++;
        
        if (tickCounter % UPDATE_INTERVAL == 0) {
            for (CameraChunkRequest request : ACTIVE_CAMERAS.values()) {
                if (request.isValid(level)) {
                    Vec3 cameraPos = request.getCameraPosition();
                    int currentSectionX = SectionPos.posToSectionCoord(cameraPos.x);
                    int currentSectionZ = SectionPos.posToSectionCoord(cameraPos.z);
                    
                    if (currentSectionX != request.lastSectionX || 
                        currentSectionZ != request.lastSectionZ) {
                        
                        request.lastSectionX = currentSectionX;
                        request.lastSectionZ = currentSectionZ;
                        
                        sendChunkLoadRequest(request.camera.getBlockPos(), true);
                    }
                }
            }
        }
        
        ACTIVE_CAMERAS.values().removeIf(request -> !request.isValid(level));
    }
    
    public static void registerCamera(ViewFinderBlockEntity camera) {
        BlockPos pos = camera.getBlockPos();
        if (!ACTIVE_CAMERAS.containsKey(pos)) {
            ACTIVE_CAMERAS.put(pos, new CameraChunkRequest(camera));
            sendChunkLoadRequest(pos, true);
        }
    }
    
    public static void unregisterCamera(BlockPos pos) {
        if (ACTIVE_CAMERAS.remove(pos) != null) {
            sendChunkLoadRequest(pos, false);
        }
    }
    
    public static void clear() {
        for (BlockPos pos : ACTIVE_CAMERAS.keySet()) {
            sendChunkLoadRequest(pos, false);
        }
        ACTIVE_CAMERAS.clear();
        tickCounter = 0;
    }
    
    private static void sendChunkLoadRequest(BlockPos cameraPos, boolean register) {
        NetworkHelper.sendToServer(new ServerBoundCameraChunkRequestPacket(cameraPos, register, (byte) CHUNK_RADIUS));
    }
    
    public static Set<ChunkPos> getDesiredChunks() {
        Set<ChunkPos> chunks = new HashSet<>();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return chunks;
        
        for (CameraChunkRequest request : ACTIVE_CAMERAS.values()) {
            if (request.isValid(level)) {
                Vec3 pos = request.getCameraPosition();
                int chunkX = SectionPos.posToSectionCoord(pos.x);
                int chunkZ = SectionPos.posToSectionCoord(pos.z);
                
                for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                    for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                        chunks.add(new ChunkPos(chunkX + dx, chunkZ + dz));
                    }
                }
            }
        }
        
        return chunks;
    }
    
    public static boolean isCameraChunk(ChunkPos chunkPos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return false;
        
        for (CameraChunkRequest request : ACTIVE_CAMERAS.values()) {
            if (request.isValid(level)) {
                Vec3 pos = request.getCameraPosition();
                int chunkX = SectionPos.posToSectionCoord(pos.x);
                int chunkZ = SectionPos.posToSectionCoord(pos.z);
                
                int dx = Math.abs(chunkPos.x - chunkX);
                int dz = Math.abs(chunkPos.z - chunkZ);
                
                if (dx <= CHUNK_RADIUS && dz <= CHUNK_RADIUS) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private static class CameraChunkRequest {
        private final ViewFinderBlockEntity camera;
        private int lastSectionX = Integer.MIN_VALUE;
        private int lastSectionZ = Integer.MIN_VALUE;
        
        CameraChunkRequest(ViewFinderBlockEntity camera) {
            this.camera = camera;
        }
        
        boolean isValid(ClientLevel level) {
            return camera.getLevel() == level && !camera.isRemoved();
        }
        
        Vec3 getCameraPosition() {
            return camera.getBlockPos().getCenter();
        }
    }
}
