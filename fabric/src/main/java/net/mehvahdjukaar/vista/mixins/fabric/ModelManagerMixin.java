package net.mehvahdjukaar.vista.mixins.fabric;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.vista.client.TapeTextureManager;
import net.minecraft.client.resources.model.ModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(ModelManager.class)
public class ModelManagerMixin {

    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Ljava/util/Map;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;"))
    private static Map<?, ?> modifySpriteSourcesMap(Map<?, ?> original) {
        var builder = ImmutableMap.builder().putAll(original);
        builder.put(TapeTextureManager.ATLAS_LOCATION, TapeTextureManager.ATLAS_INFO_LOCATION);

        return builder.build();
    }
}
