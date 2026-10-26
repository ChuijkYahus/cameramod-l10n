package net.mehvahdjukaar.vista.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.mehvahdjukaar.moonlight.api.events.AfterLanguageLoadEvent;
import net.mehvahdjukaar.moonlight.api.resources.ResType;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicClientResourceProvider;
import net.mehvahdjukaar.moonlight.api.resources.pack.PackGenerationStrategy;
import net.mehvahdjukaar.moonlight.api.resources.pack.ResourceGenTask;
import net.mehvahdjukaar.moonlight.api.util.math.ColorUtils;
import net.mehvahdjukaar.moonlight.api.util.math.colors.HSVColor;
import net.mehvahdjukaar.moonlight.api.util.math.colors.RGBColor;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.world.item.DyeColor;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class VistaDynamicResources extends DynamicClientResourceProvider {

    public VistaDynamicResources() {
        super(VistaMod.res("color_shaders"), PackGenerationStrategy.REGEN_ON_EVERY_RELOAD); //leave like this since they are config dependant
    }

    @Override
    protected void regenerateDynamicAssets(Consumer<ResourceGenTask> consumer) {
        consumer.accept((resourceManager, resourceSink) -> {
            for (var c : DyeColor.values()) {
                RGBColor tint = new RGBColor(ColorUtils.swapFormat(0xFF000000 | c.getTextColor()));
                float sr = tint.red();
                float sg = tint.green();
                float sb = tint.blue();

                float lr = toLinear(sr);
                float lg = toLinear(sg);
                float lb = toLinear(sb);

                float lum = tint.asXYZ().y();

                HSVColor hsv = tint.asHSV();
                float hue = hsv.hue(); //0-1, not degrees
                float sat = hsv.saturation();

                //all hand tuned
                float tintStrength = 0.70f;

                float mulR = 1f - (1f - lr) * tintStrength;
                float mulG = 1f - (1f - lg) * tintStrength;
                float mulB = 1f - (1f - lb) * tintStrength;

                mulR = (float) Math.pow(mulR, 0.98f);
                mulG = (float) Math.pow(mulG, 0.98f);
                mulB = (float) Math.pow(mulB, 0.98f);

                // lift darks a bit so they get tinted too
                float addBase = 0.008f;
                float addStrength = 0.045f;
                float addScale = addBase + addStrength * (1f - lum);

                float addR = sr * addScale * 0.9f + 0.001f;
                float addG = sg * addScale * 0.9f + 0.001f;
                float addB = sb * addScale * 0.9f + 0.001f;

                addR = Math.clamp(addR, 0f, 0.12f);
                addG = Math.clamp(addG, 0f, 0.12f);
                addB = Math.clamp(addB, 0f, 0.12f);

                float saturation = 0.90f + 0.15f * sat;

                float contrast = 1.00f + 0.30f * (0.5f - lum);
                contrast = Math.clamp(contrast, 0.85f, 1.25f);

                if (hue >= 0.444f && hue <= 0.556f) { //cyan
                    contrast = Math.min(contrast + 0.05f, 1.30f);
                    saturation -= 0.03f;
                }
                if (hue >= 0.083f && hue <= 0.167f) { //orange-yellow
                    addR += 0.005f;
                }

                JsonObject json = new JsonObject();
                JsonArray targets = new JsonArray();
                targets.add(new JsonPrimitive("swap"));
                json.add("targets", targets);

                JsonArray passes = new JsonArray();

                JsonObject pass = new JsonObject();
                pass.addProperty("name", "vista:color_grade");
                pass.addProperty("intarget", "minecraft:main");
                pass.addProperty("outtarget", "swap");

                JsonArray uniforms = new JsonArray();

                JsonObject uMul = new JsonObject();
                uMul.addProperty("name", "Mul");
                JsonArray mulArr = new JsonArray();
                mulArr.add(new JsonPrimitive(round4(mulR)));
                mulArr.add(new JsonPrimitive(round4(mulG)));
                mulArr.add(new JsonPrimitive(round4(mulB)));
                uMul.add("values", mulArr);
                uniforms.add(uMul);

                JsonObject uAdd = new JsonObject();
                uAdd.addProperty("name", "Add");
                JsonArray addArr = new JsonArray();
                addArr.add(new JsonPrimitive(round4(addR)));
                addArr.add(new JsonPrimitive(round4(addG)));
                addArr.add(new JsonPrimitive(round4(addB)));
                uAdd.add("values", addArr);
                uniforms.add(uAdd);

                JsonObject uContrast = new JsonObject();
                uContrast.addProperty("name", "Contrast");
                JsonArray contrastArr = new JsonArray();
                contrastArr.add(new JsonPrimitive(round4(contrast)));
                uContrast.add("values", contrastArr);
                uniforms.add(uContrast);

                JsonObject uSat = new JsonObject();
                uSat.addProperty("name", "Saturation");
                JsonArray satArr = new JsonArray();
                satArr.add(new JsonPrimitive(round4(saturation)));
                uSat.add("values", satArr);
                uniforms.add(uSat);

                pass.add("uniforms", uniforms);
                passes.add(pass);

                JsonObject blit = new JsonObject();
                blit.addProperty("name", "blit");
                blit.addProperty("intarget", "swap");
                blit.addProperty("outtarget", "minecraft:main");
                passes.add(blit);

                json.add("passes", passes);

                resourceSink.addJson(
                        VistaMod.res("shaders/post/" + c.getSerializedName() + "_tint.json"),
                        json,
                        ResType.GENERIC
                );
            }


        });
    }

    private static float toLinear(float srgb) {
        return srgb <= 0.04045f ? srgb / 12.92f : (float) Math.pow((srgb + 0.055f) / 1.055f, 2.4);
    }

    // keeps the generated JSON readable
    private static float round4(float v) {
        return Math.round(v * 10000f) / 10000f;
    }

    @Override
    protected Collection<String> gatherSupportedNamespaces() {
        return List.of();
    }

    @Override
    protected void addDynamicTranslations(AfterLanguageLoadEvent afterLanguageLoadEvent) {
    }
}
