package net.mehvahdjukaar.vista.common.picture_tape;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Immutable content of a picture tape: an ordered list of stored items (filled maps).
 * The list is always kept compact (no empty gaps) by the menu.
 */
public final class PictureTapeContent {

    public static final Codec<PictureTapeContent> CODEC = ItemStack.CODEC.listOf()
            .xmap(PictureTapeContent::new, p -> p.pictures);

    public static final StreamCodec<RegistryFriendlyByteBuf, PictureTapeContent> STREAM_CODEC = ItemStack.LIST_STREAM_CODEC
            .map(PictureTapeContent::new, p -> p.pictures);

    public static final PictureTapeContent EMPTY = new PictureTapeContent(List.of());

    private final List<ItemStack> pictures;
    private final int playSpeed;

    public PictureTapeContent(List<ItemStack> pictures, int playSpeed) {
        this.pictures = List.copyOf(pictures);
        this.playSpeed = playSpeed;
    }

    public PictureTapeContent(List<ItemStack> pictures) {
        this(pictures, 40);
    }

    public int playbackSpeed() {
        return playSpeed;
    }

    public Stream<ItemStack> pictures() {
        return pictures.stream();
    }

    @Nullable
    public ItemStack getPicture(int index) {
        return pictures.get(index);
    }

    public int size() {
        return pictures.size();
    }

    public PictureTapeContent withInsertedAtIndex(int index, ItemStack picture) {
        List<ItemStack> newPictures = new ArrayList<>(this.pictures);
        if (index < 0 || index > newPictures.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + newPictures.size());
        }
        newPictures.add(index, picture);
        return new PictureTapeContent(newPictures, playSpeed);
    }

    public PictureTapeContent withRemovedAtIndex(int index) {
        List<ItemStack> newPictures = new ArrayList<>(this.pictures);
        if (index < 0 || index >= newPictures.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + newPictures.size());
        }
        newPictures.remove(index);
        return new PictureTapeContent(newPictures, playSpeed);
    }

    public PictureTapeContent withPlaySpeed(int speed) {
        return new PictureTapeContent(this.pictures, speed);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PictureTapeContent) obj;
        return this.playSpeed == that.playSpeed && Objects.equals(this.pictures, that.pictures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pictures, playSpeed);
    }

    @Override
    public String toString() {
        return "PictureTapeContent[pictures=" + pictures + ", playSpeed=" + playSpeed + ']';
    }
}
