package net.remmintan.panama;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-render-layer vertex buffers for Panama's model builders (Minecraft 1.21+).
 * <p>
 * Up to 1.20.6 Panama kept reusable {@code BufferBuilder}s (a {@code BlockBufferBuilderStorage} plus a map of line
 * builders) and started each one itself with {@code begin(mode, format)}. In 1.21 a {@code BufferBuilder} is created
 * already started, on top of a {@link BufferAllocator}, and is finished with {@code endNullable()}. This class keeps
 * the old shape: {@link #get(RenderLayer)} returns the layer's started builder (using the layer's own draw mode and
 * vertex format, which is what Panama used to pass to {@code begin}) and {@link #end(RenderLayer)} finishes it.
 * <p>
 * Client render thread only.
 */
public final class PanamaBufferStorage implements AutoCloseable {

    private static final int INITIAL_ALLOCATOR_SIZE = 256;

    private final Map<RenderLayer, BufferAllocator> allocators = new HashMap<>();
    private final Map<RenderLayer, BufferBuilder> started = new HashMap<>();

    /** The started builder for {@code layer}; created on first use since the last {@link #end}. */
    public BufferBuilder get(RenderLayer layer) {
        return started.computeIfAbsent(layer, l -> new BufferBuilder(
                allocators.computeIfAbsent(l, x -> new BufferAllocator(INITIAL_ALLOCATOR_SIZE)),
                l.getDrawMode(),
                l.getVertexFormat()));
    }

    /** Finishes the layer's builder; {@code null} when nothing was drawn into it. */
    public BuiltBuffer end(RenderLayer layer) {
        final BufferBuilder builder = started.remove(layer);
        return builder == null ? null : builder.endNullable();
    }

    @Override
    public void close() {
        started.clear();
        allocators.values().forEach(BufferAllocator::close);
        allocators.clear();
    }
}
