package net.remmintan.panama;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;

import java.util.concurrent.CompletableFuture;

public class RenderHelper {

    public static CompletableFuture<Void> scheduleUpload(
            net.minecraft.client.render.BuiltBuffer builtBuffer,
            VertexBuffer vertexBuffer
    ) {
        Runnable runnable = () -> {
            if (builtBuffer != null && !vertexBuffer.isClosed()) { // 1.21+: an empty layer has no built buffer
                vertexBuffer.bind();
                vertexBuffer.upload(builtBuffer);
                VertexBuffer.unbind();
            }
        };
        return CompletableFuture.runAsync(runnable, MinecraftClient.getInstance());
    }



}
