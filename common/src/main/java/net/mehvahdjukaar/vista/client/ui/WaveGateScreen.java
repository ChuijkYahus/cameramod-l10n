package net.mehvahdjukaar.vista.client.ui;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.textures.WebTexturesManager;
import net.mehvahdjukaar.vista.common.wave_gate.WaveGateBlockEntity;
import net.mehvahdjukaar.vista.network.ServerBoundSyncWaveGatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class WaveGateScreen extends Screen {
    private static final Component EDIT = Component.translatable("gui.vista.wave_gate.edit");

    private EditBox editBox;
    private final WaveGateBlockEntity tile;

    public WaveGateScreen(WaveGateBlockEntity te) {
        super(EDIT);
        this.tile = te;
    }

    public static void open(WaveGateBlockEntity te) {
        Minecraft.getInstance().setScreen(new WaveGateScreen(te));
    }


    @Override
    public void init() {
        assert this.minecraft != null;

        int buttonSize = 20;
        int spacing = 4;
        int boxWidth = 360 - buttonSize - spacing;
        int totalWidth = boxWidth + spacing + buttonSize;
        int left = (this.width - totalWidth) / 2;
        int top = this.height / 4 + 10;

        this.editBox = new EditBox(this.font, left, top, boxWidth, 20, this.title) {
            protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage();
            }
        };
        this.editBox.setMaxLength(3000);
        this.editBox.setValue(tile.getUrl());
        this.addRenderableWidget(this.editBox);
        this.setInitialFocus(this.editBox);
        this.editBox.setFocused(true);

        // Square refresh button with a simple glyph icon, next to the text box
        Button refreshButton = SpriteIconButton.builder(Component.translatable("gui.vista.refresh"), (button) -> {
                    String url = tile.getUrl();
                    if (url != null && !url.isBlank()) {
                        WebTexturesManager.invalidateUrl(url);
                    }
                }, true)
                .size(buttonSize, buttonSize)
                .sprite(VistaModClient.REFRESH_ICON, 16, 16)
                .build();
        refreshButton.setX(left + boxWidth + spacing);
        refreshButton.setY(top);
        refreshButton.setTooltip(Tooltip.create(Component.translatable("gui.vista.refresh")));
        this.addRenderableWidget(refreshButton);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> this.onDone()).bounds(this.width / 2 - 100, this.height / 4 + 120, 200, 20).build());
    }

    @Override
    public void removed() {
        //update this client immediately
        String str = this.editBox.getValue();
        //  this.tile.setUrl(str); updated by packet layer
        NetworkHelper.sendToServer(new ServerBoundSyncWaveGatePacket(this.tile.getBlockPos(), str));
    }

    private void onDone() {
        this.tile.setChanged();
        this.minecraft.setScreen(null);
    }

    @Override
    public void onClose() {
        this.onDone();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode != 257 && keyCode != 335) {
            return false;
        } else {
            this.onDone();
            return true;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 40, 16777215);
    }
}