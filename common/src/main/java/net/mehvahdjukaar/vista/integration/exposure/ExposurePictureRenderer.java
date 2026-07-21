package net.mehvahdjukaar.vista.integration.exposure;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.world.item.AlbumItem;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.component.album.AlbumPage;
import net.mehvahdjukaar.vista.client.ui.TapeEntryRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;

public class ExposurePictureRenderer implements TapeEntryRenderer {

    @Override
    public boolean matches(ItemStack stack) {
        return ExposureCompat.isExposurePicture(stack);
    }

    @Override
    public void render(GuiGraphics graphics, ItemStack stack, int x, int y, int size) {
        ItemStack toRender = stack;
        if (stack.getItem() instanceof AlbumItem album) {
            toRender = firstPhoto(album, stack);
        }

        if (!toRender.isEmpty()) {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(x, y, 1.0);
            pose.scale(size, size, 1f);
            MultiBufferSource.BufferSource buffer = graphics.bufferSource();
            boolean rendered = ExposureClient.photographRenderer()
                    .render(toRender, false, false, pose, buffer, LightTexture.FULL_BRIGHT);
            graphics.flush();
            pose.popPose();
            if (rendered) return;
        }

        // fallback: neutral background with the item icon centered
        graphics.fill(x, y, x + size, y + size, 0xFF888888);
        graphics.renderItem(stack, x + size / 2 - 8, y + size / 2 - 8);
    }

    private static ItemStack firstPhoto(AlbumItem album, ItemStack stack) {
        for (AlbumPage page : album.getContent(stack).pages()) {
            ItemStack photo = page.photograph();
            if (photo.getItem() instanceof PhotographItem) return photo;
        }
        return ItemStack.EMPTY;
    }
}
