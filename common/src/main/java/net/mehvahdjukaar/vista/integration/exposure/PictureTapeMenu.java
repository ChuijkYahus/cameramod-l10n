package net.mehvahdjukaar.vista.integration.exposure;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.util.PagingDirection;
import io.github.mortuusars.exposure.world.inventory.slot.AlbumPhotographSlot;
import io.github.mortuusars.exposure.world.inventory.slot.AlbumPlayerInventorySlot;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

//Credits for exposure mods devs
public class PictureTapeMenu extends AbstractContainerMenu {
    public static final int CANCEL_ADDING_PHOTO_BUTTON = -1;
    public static final int PREVIOUS_PAGE_BUTTON = PagingDirection.PREVIOUS.ordinal();
    public static final int NEXT_PAGE_BUTTON = PagingDirection.NEXT.ordinal();
    public static final int PAGE_PHOTO_BUTTON = 2;

    protected final int pictureTapeSlot;
    protected final ItemStack pictureTape;

    protected final List<ItemStack> photographSlots = new ArrayList<>();
    protected final List<AlbumPlayerInventorySlot> playerInventorySlots = new ArrayList<>();

    protected DataSlot currentIndex = DataSlot.standalone();

    protected boolean isAddingPhotograph = false;

    protected final Map<Integer, Consumer<Player>> buttonActions = new HashMap<>() {{
        put(CANCEL_ADDING_PHOTO_BUTTON, p -> {
            isAddingPhotograph = false;
            if (!getCarried().isEmpty()) {
                p.getInventory().placeItemBackInInventory(getCarried());
                setCarried(ItemStack.EMPTY);
            }
            updatePlayerInventorySlots();
        });
        put(PREVIOUS_PAGE_BUTTON, p -> {
            clickMenuButton(p, CANCEL_ADDING_PHOTO_BUTTON);
            setCurrentSpreadIndex(Math.max(0, getCurrentIndex() - 1));
        });
        put(NEXT_PAGE_BUTTON, p -> {
            clickMenuButton(p, CANCEL_ADDING_PHOTO_BUTTON);
            setCurrentSpreadIndex(Math.min((getTapeContent().size() - 1) / 2, getCurrentIndex() + 1));
        });
        put(PAGE_PHOTO_BUTTON, p -> onPhotoButtonPress(p));
    }};


    public static PictureTapeMenu fromBuffer(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        return new PictureTapeMenu(containerId, playerInventory, buffer.readVarInt());
    }

    public PictureTapeMenu(int containerId, Inventory playerInventory, int albumSlot) {
        this(ExposureCompat.PICTURE_TAPE_MENU.get(), containerId, playerInventory, albumSlot);
    }

    protected PictureTapeMenu(MenuType<? extends AbstractContainerMenu> type, int containerId, Inventory playerInventory, int albumSlot) {
        super(type, containerId);
        this.pictureTapeSlot = albumSlot;

        pictureTape = playerInventory.getItem(albumSlot);
        if (!(pictureTape.getItem() instanceof PictureTapeItem)) {
            throw new IllegalStateException("Expected PictureTapeItem in slot '" + albumSlot + "'. Got: " + pictureTape);
        }

        addPhotographSlots();
        addPlayerInventorySlots(playerInventory, 70, 115);
        addDataSlot(currentIndex);
    }

    protected void addPhotographSlots() {
        PictureTapeContent content = PictureTapeItem.getContent(pictureTape);
        this.photographSlots.addAll(content.pictures().toList());
        this.photographSlots.add(ItemStack.EMPTY); //add 1 empty at the end
        ItemStack[] photographs = content
                .pictures().toArray(ItemStack[]::new);
        SimpleContainer container = new SimpleContainer(photographs);

        for (int i = 0; i < container.getContainerSize(); i++) {
            int x = i % 2 == 0 ? 71 : 212;
            int y = 67;
            AlbumPhotographSlot slot = new AlbumPhotographSlot(container, i, x, y) {
                @Override
                public void set(ItemStack stack) {
                    super.set(stack);
                    onPhotographSlotChanged(getContainerSlot(), stack);
                }
            };
            addSlot(slot);
          //  photographSlots.add(slot);
        }

        //add 1 empty at the end
    }

    private void onPhotographSlotChanged(int slotIndex, ItemStack stack) {
        PictureTapeItem.setPictureAtIndex(pictureTape, slotIndex, stack);
//        List<AlbumPage> pages = getPages();
//        AlbumPage page = pages.get(slotIndex);
//        page = page.setPhotograph(stack);
//        pages.set(slotIndex, page);
    }

    private void addPlayerInventorySlots(Inventory playerInventory, int x, int y) {
        // Player inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                AlbumPlayerInventorySlot slot = new AlbumPlayerInventorySlot(playerInventory, column + row * 9 + 9,
                        x + column * 18, y + row * 18);
                addSlot(slot);
                playerInventorySlots.add(slot);
            }
        }

        // Player hotbar slots
        // Hotbar should go after main inventory for Shift+Click to work properly.
        for (int index = 0; index < 9; ++index) {
            boolean disabled = index == playerInventory.selected && playerInventory.getSelected().getItem() instanceof PictureTapeItem;
            AlbumPlayerInventorySlot slot = new AlbumPlayerInventorySlot(playerInventory, index,
                    x + index * 18, y + 58) {
                @Override
                public boolean mayPickup(Player player) {
                    return !disabled;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !disabled;
                }
            };
            addSlot(slot);
            playerInventorySlots.add(slot);
        }
    }

    protected void updatePlayerInventorySlots() {
        boolean isInAddingPhotographMode = isInAddingPhotographMode();
        for (AlbumPlayerInventorySlot slot : playerInventorySlots) {
            slot.setActive(isInAddingPhotographMode);
        }
    }

    public int getAlbumSlot() {
        return pictureTapeSlot;
    }

    public boolean isInAddingPhotographMode() {
        return isAddingPhotograph;
    }

    public List<AlbumPlayerInventorySlot> getPlayerInventorySlots() {
        return playerInventorySlots;
    }

    public PictureTapeContent getTapeContent() {
        return PictureTapeItem.getContent(pictureTape);
    }

    public Optional<ItemStack> getPictureAtIndex(int pageIndex) {
        PictureTapeContent content = getTapeContent();
        if (pageIndex <= content.size() - 1)
            return Optional.ofNullable(content.getPicture(pageIndex));

        return Optional.empty();
    }

    public Optional<ItemStack> getPictureAtIndex() {
        return getPictureAtIndex(getCurrentIndex());
    }

    public Optional<AlbumPhotographSlot> getCurrentPhotograph() {
        return getCurrentPhotograph(getCurrentIndex());
    }

    public Optional<AlbumPhotographSlot> getCurrentPhotograph(int index) {
        if (index >= 0 && index < photographSlots.size())
            return Optional.empty();//  Optional.ofNullable(photographSlots.get(index));

        return Optional.empty();
    }

    public ItemStack getPhotograph() {
        return getCurrentPhotograph().map(Slot::getItem).orElse(ItemStack.EMPTY);
    }

    public int getCurrentIndex() {
        return this.currentIndex.get();
    }

    public void setCurrentSpreadIndex(int spreadIndex) {
        this.currentIndex.set(spreadIndex);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        @Nullable Consumer<Player> buttonAction = buttonActions.get(id);
        if (buttonAction != null) {
            buttonAction.accept(player);
            return true;
        }

        return false;
    }

    private void onPhotoButtonPress(Player player) {

        Optional<AlbumPhotographSlot> photographSlot = getCurrentPhotograph();
        if (photographSlot.isEmpty())
            return;

        AlbumPhotographSlot slot = photographSlot.get();
        if (!slot.hasItem()) {
            isAddingPhotograph = true;
        } else {
            ItemStack stack = slot.getItem();
            if (!player.getInventory().add(stack))
                player.drop(stack, false);

            slot.set(ItemStack.EMPTY);
        }

        updatePlayerInventorySlots();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Both sides

        if (!isInAddingPhotographMode() || slotId < 0 || slotId >= slots.size()) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        Slot slot = slots.get(slotId);
        ItemStack stack = slot.getItem();

        if (button == InputConstants.MOUSE_BUTTON_LEFT
                && slot instanceof AlbumPlayerInventorySlot
                && stack.getItem() instanceof PhotographItem
                && getCarried().isEmpty()) {
            int pageIndex = getCurrentIndex();
            Optional<AlbumPhotographSlot> photographSlot = getCurrentPhotograph(pageIndex);
            if (photographSlot.isEmpty() || !photographSlot.get().getItem().isEmpty())
                return;

            photographSlot.get().set(stack);
            slot.set(ItemStack.EMPTY);

            if (player.level().isClientSide) {
                player.playSound(Exposure.SoundEvents.PHOTOGRAPH_PLACE.get(), 0.8f, 1.1f);
            }

            isAddingPhotograph = false;
            updatePlayerInventorySlots();
        } else
            super.clicked(slotId, button, clickType, player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().getItem(pictureTapeSlot).getItem() instanceof PictureTapeItem;
    }

}