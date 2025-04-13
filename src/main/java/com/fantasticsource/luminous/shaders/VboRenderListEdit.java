package com.fantasticsource.luminous.shaders;

import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL20;

@SideOnly(Side.CLIENT)
public class VboRenderListEdit extends ChunkRenderContainer
{
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        if (initialized)
        {
            GL20.glUseProgram(Shaders.LIGHTS);

            for (RenderChunk renderchunk : renderChunks)
            {
                VertexBuffer vertexbuffer = renderchunk.getVertexBufferByLayer(layer.ordinal());
                GlStateManager.pushMatrix();
                preRenderChunk(renderchunk);
                renderchunk.multModelviewMatrix();

                vertexbuffer.bindBuffer();
                GlStateManager.glVertexPointer(3, 5126, 28, 0);
                GlStateManager.glColorPointer(4, 5121, 28, 12);
                GlStateManager.glTexCoordPointer(2, 5126, 28, 16);
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
                GlStateManager.glTexCoordPointer(2, 5122, 28, 24);
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);

                vertexbuffer.drawArrays(7);
                GlStateManager.popMatrix();
            }

            OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
            GlStateManager.resetColor();
            renderChunks.clear();

            GL20.glUseProgram(0);
        }
    }
}