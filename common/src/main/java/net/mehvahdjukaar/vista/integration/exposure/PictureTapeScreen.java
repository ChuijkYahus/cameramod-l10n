package net.mehvahdjukaar.vista.integration.exposure;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.client.gui.screen.album.AlbumGUI;
import io.github.mortuusars.exposure.client.gui.screen.album.PhotographSlotWidget;
import io.github.mortuusars.exposure.client.gui.screen.element.Pager;
import io.github.mortuusars.exposure.client.gui.screen.element.textbox.TextBox;
import io.github.mortuusars.exposure.client.input.Key;
import io.github.mortuusars.exposure.client.input.KeyBindings;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.util.PagingDirection;
import io.github.mortuusars.exposure.world.inventory.slot.AlbumPlayerInventorySlot;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.sound.SoundEffect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PictureTapeScreen extends AbstractContainerScreen<PictureTapeMenu> {

    //scrollable vertical tape feed with non discrete scroll which allows to add entries wherever;
    protected TapeScrollingEntriesListWidget tapeScrollWidget;

    //remove
    protected final Pager pager = new Pager()
            .setChangeSound(new SoundEffect(() -> SoundEvents.BOOK_PAGE_TURN))
            .onPageChanged((oldPage, newPage) -> clickButton(PagingDirection.fromChange(oldPage, newPage).ordinal()));

    protected final KeyBindings keyBindings = KeyBindings.of(
            Key.press(Minecrft.options().keyInventory).executes(this::onClose),
            Key.press(InputConstants.KEY_LEFT).or(Key.press(InputConstants.KEY_A)).executes(pager::previousPage),
            Key.press(InputConstants.KEY_RIGHT).or(Key.press(InputConstants.KEY_D)).executes(pager::nextPage),
            Key.release(InputConstants.KEY_LEFT).or(Key.press(InputConstants.KEY_A)).executes(pager::resetCooldown),
            Key.release(InputConstants.KEY_RIGHT).or(Key.press(InputConstants.KEY_D)).executes(pager::resetCooldown)
    );

    protected final List<Page> pages = new ArrayList<>();

    public PictureTapeScreen(PictureTapeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        this.imageWidth = 298;
        this.imageHeight = 188;
        super.init();

        titleLabelY = -999;
        inventoryLabelX = 69;
        inventoryLabelY = -999; // Inventory label will be moved into position when inventory is shown

        this.tapeScrollWidget = null;// this.addRenderableWidget(new TapeScrollingEntriesListWidget());

        pages.clear();

        // LEFT:
        Page leftPage = createPage(0);
        pages.add(leftPage);

        ImageButton previousPageButton = new ImageButton(leftPos + 12, topPos + 164, 13, 15,
                AlbumGUI.PREVIOUS_PAGE_BUTTON_SPRITES, button -> pager.changePage(PagingDirection.PREVIOUS), Component.translatable("gui.exposure.previous_page"));
        previousPageButton.setTooltip(Tooltip.create(Component.translatable("gui.exposure.previous_page")));
        addRenderableWidget(previousPageButton);

        ImageButton nextPageButton = new ImageButton(leftPos + 273, topPos + 164, 13, 15,
                AlbumGUI.NEXT_PAGE_BUTTON_SPRITES, button -> pager.changePage(PagingDirection.NEXT), Component.translatable("gui.exposure.next_page"));
        nextPageButton.setTooltip(Tooltip.create(Component.translatable("gui.exposure.next_page")));
        addRenderableWidget(nextPageButton);


        int spreadsCount = (int) Math.ceil(getMenu().getTapeContent().size() / 2f);
        pager.setPagesCount(spreadsCount)
                .setPreviousPageButton(previousPageButton)
                .setNextPageButton(nextPageButton);
    }

    @Override
    protected void containerTick() {

    }

    protected Page createPage(int xOffset) {
        int x = leftPos + xOffset;
        int y = topPos;

        Rect2i page = new Rect2i(x, y, 149, 188);
        Rect2i photo = new Rect2i(x + 25, y + 21, 108, 108);

        PhotographSlotWidget photographWidget = new PhotographSlotWidget(this, photo.getX(), photo.getY(),
                photo.getWidth(), photo.getHeight(), () -> getMenu().getPhotograph()) {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return !isInAddingMode() && super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean isHovered() {
                return !isInAddingMode() && super.isHovered();
            }
        };

        photographWidget
                .editable(true)
                .primaryAction(widget -> {
                    if (!widget.inspectPhotograph() && widget.getPhotograph().isEmpty() && widget.isEditable()) {
                        clickButton(PictureTapeMenu.PAGE_PHOTO_BUTTON);
                        Minecrft.get().getSoundManager().play(SimpleSoundInstance.forUI(
                                SoundEvents.UI_BUTTON_CLICK, 1f));
                    }
                })
                .secondaryAction(widget -> {
                    if (widget.isEditable() && !widget.getPhotograph().isEmpty()) {
                        clickButton(PictureTapeMenu.PAGE_PHOTO_BUTTON);
                        Minecrft.get().getSoundManager().play(SimpleSoundInstance.forUI(
                                Exposure.SoundEvents.PHOTOGRAPH_PLACE.get(), 0.7f, 1.1f));
                    }
                });

        addRenderableWidget(photographWidget);

        return new Page(photo, photographWidget);
    }

    // RENDER

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateWidgetVisibility();

        inventoryLabelY = isInAddingMode() ? getMenu().getPlayerInventorySlots().getFirst().y - 12 : -999;

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (isInAddingMode()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            for (Slot slot : getMenu().slots) {
                if (!slot.getItem().isEmpty() && !(slot.getItem().getItem() instanceof PhotographItem)) {
                    guiGraphics.blit(AlbumGUI.TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1, 350, 176, 188,
                            18, 18, 512, 512);
                }
            }
            RenderSystem.disableBlend();
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void updateWidgetVisibility() {
        // Note should be hidden when adding photograph because it's drawn over the slots. Blit offset does not help.

        for (Page page : pages) {
            page.photographWidget.visible = !getMenu().getPhotograph().isEmpty()
                    || (!isInAddingMode());
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(guiGraphics);
        renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 15);
        super.renderLabels(guiGraphics, mouseX, mouseY);

        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (isInAddingMode() && hoveredSlot != null && !hoveredSlot.getItem()
                .isEmpty() && !(hoveredSlot.getItem().getItem() instanceof PhotographItem)) {
            return; // Do not render tooltips for greyed-out items
        }

        if (!isInAddingMode()) {
            for (Page page : pages) {
                if (page.photographWidget.isHoveredOrFocused()) {
                    page.photographWidget.renderTooltip(guiGraphics, x, y);
                    return;
                }
            }
        }

        super.renderTooltip(guiGraphics, x, y);
    }

    @Override
    public @NotNull List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltipLines = super.getTooltipFromContainerItem(stack);
        if (isInAddingMode() && hoveredSlot != null && hoveredSlot.getItem() == stack
                && stack.getItem() instanceof PhotographItem) {
            tooltipLines.add(Component.empty());
            tooltipLines.add(Component.translatable("gui.exposure.album.left_click_to_add"));
        }
        return tooltipLines;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(AlbumGUI.TEXTURE, leftPos, topPos, 0, 0, 0,
                imageWidth, imageHeight, 512, 512);

        int currentSpreadIndex = getMenu().getCurrentIndex();
        drawPageNumbers(guiGraphics, currentSpreadIndex);

        if (isInAddingMode()) {
            AlbumPlayerInventorySlot firstSlot = getMenu().getPlayerInventorySlots().getFirst();
            int x = firstSlot.x - 8;
            int y = firstSlot.y - 18;
            guiGraphics.blit(AlbumGUI.TEXTURE, leftPos + x, topPos + y, 10, 0, 188, 176, 100, 512, 512);

            for (Page page : pages) {
                guiGraphics.blitSprite(PhotographSlotWidget.EMPTY_SPRITES.enabledFocused(),
                        page.photoArea.getX(), page.photoArea.getY(), page.photoArea.getWidth(), page.photoArea.getHeight());
            }
        }
    }

    protected void drawPageNumbers(GuiGraphics guiGraphics, int currentSpreadIndex) {
        Font font = Minecrft.get().font;

        String leftPageNumber = Integer.toString(currentSpreadIndex * 2 + 1);
        String rightPageNumber = Integer.toString(currentSpreadIndex * 2 + 2);

        guiGraphics.drawString(font, leftPageNumber, leftPos + 71 + (8 - font.width(leftPageNumber) / 2),
                topPos + 167, -1, false);

        guiGraphics.drawString(font, rightPageNumber, leftPos + 212 + (8 - font.width(rightPageNumber) / 2),
                topPos + 167,-1, false);

    }


    // CONTROLS:

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInAddingMode()) {
            if (!isHoveringOverInventory(mouseX, mouseY)
                    && (!hasClickedOutside(mouseX, mouseY, leftPos, topPos, button) || getMenu().getCarried().isEmpty())) {
                clickButton(PictureTapeMenu.CANCEL_ADDING_PHOTO_BUTTON);
                return true;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        if (!(getFocused() instanceof TextBox)) {
            setFocused(null); // Clear focus on mouse click because it's annoying. But keep on textbox to type.
        }

        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isQuickCrafting && !getMenu().getCarried().isEmpty() && getMenu().getCarried().getCount() == 1) {
            isQuickCrafting = false; // Fixes weird issue with carried item not placing when dragging slightly
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean handleComponentClicked(@Nullable Style style) {
        if (style == null)
            return false;

        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent == null)
            return false;
        else if (clickEvent.getAction() == ClickEvent.Action.CHANGE_PAGE) {
            String pageIndexStr = clickEvent.getValue();
            int pageIndex = Integer.parseInt(pageIndexStr) - 1;
            forcePage(pageIndex);
            return true;
        }

        boolean handled = super.handleComponentClicked(style);
        if (handled && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND)
            onClose();
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isInAddingMode())
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        else
            return this.getFocused() != null && this.isDragging() && button == 0
                    && this.getFocused().mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    protected void clickButton(int buttonId) {
        getMenu().clickMenuButton(Minecrft.player(), buttonId);
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, buttonId);

        if (buttonId == PictureTapeMenu.CANCEL_ADDING_PHOTO_BUTTON) {
            setFocused(null);
        }

        if (buttonId == PictureTapeMenu.PREVIOUS_PAGE_BUTTON || buttonId == PictureTapeMenu.NEXT_PAGE_BUTTON) {
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean isHoveringOverInventory(double mouseX, double mouseY) {
        if (!isInAddingMode()) {
            return false;
        }

        AlbumPlayerInventorySlot firstSlot = getMenu().getPlayerInventorySlots().getFirst();
        int x = firstSlot.x - 8;
        int y = firstSlot.y - 18;
        return isHovering(x, y, 176, 100, mouseX, mouseY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton)
                && !isHoveringOverInventory(mouseX, mouseY);
    }

    @SuppressWarnings("UnusedReturnValue")
    protected boolean forcePage(int pageIndex) {
        try {
            int newSpreadIndex = pageIndex / 2;

            if (newSpreadIndex == getMenu().getCurrentIndex() || newSpreadIndex < 0
                    || newSpreadIndex > getMenu().getTapeContent().size() / 2) {
                return false;
            }

            PagingDirection pagingDirection = newSpreadIndex < getMenu().getCurrentIndex()
                    ? PagingDirection.PREVIOUS : PagingDirection.NEXT;

            int pageChanges = 0; // Safeguard against infinite loop. Probably not needed. But I don't mind it.
            while (newSpreadIndex != getMenu().getCurrentIndex() || !pager.canChangePage(pagingDirection)) {
                if (pageChanges > 16) {
                    break;
                }

                pager.changePage(pagingDirection);
                pageChanges++;
            }
            return true;
        } catch (Exception e) {
            Exposure.LOGGER.error("Cannot force page: {}", e.toString());
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_TAB)
            return super.keyPressed(keyCode, scanCode, modifiers);

        if (isInAddingMode() && (Minecrft.options().keyInventory.matches(keyCode, scanCode)
                || keyCode == InputConstants.KEY_ESCAPE)) {
            clickButton(PictureTapeMenu.CANCEL_ADDING_PHOTO_BUTTON);
            return true;
        }

        return keyBindings.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {

        return keyBindings.keyReleased(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
    }


    // MISC:

    protected boolean isInAddingMode() {
        return getMenu().isInAddingPhotographMode();
    }

    protected void forEachPage(Consumer<Page> pageAction) {
        for (Page page : pages) {
            pageAction.accept(page);
        }
    }

    protected class Page {
        public final Rect2i photoArea;
        public final PhotographSlotWidget photographWidget;

        private Page(Rect2i photoArea, PhotographSlotWidget photographWidget) {
            this.photoArea = photoArea;
            this.photographWidget = photographWidget;
        }

        public boolean isMouseOver(Rect2i area, double mouseX, double mouseY) {
            return isHovering(area.getX() - leftPos, area.getY() - topPos,
                    area.getWidth(), area.getHeight(), mouseX, mouseY);
        }

    }
}