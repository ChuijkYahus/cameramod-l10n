# Moonlight Config Builder — quick hookup guide

How to wire up a config using Moonlight's `ConfigBuilder` (the loader-independent config + native config screen).
Everything lives in `net.mehvahdjukaar.moonlight.api.platform.configs`.

## 1. The holder pattern

Configs are declared once in a `static {}` block and exposed as `public static final Supplier<T>` fields. `define(...)`
returns the value handle (a `Supplier`); read it later with `.get()`.

```java
public class MyConfigs {
    public static final ModConfigHolder SPEC;
    public static final Supplier<Integer> SOME_VALUE;

    static {
        ConfigBuilder builder = ConfigBuilder.create("mymod", ConfigType.CLIENT); // or COMMON_SYNCED

        builder.push("category");
        SOME_VALUE = builder
                .comment("What this does. Shows as the tooltip/description in the screen.")
                .define("some_value", 8, 1, 32); // default, min, max
        builder.pop();

        SPEC = builder.build();
        SPEC.forceLoad();
    }

    // call from your mod init so the class loads and the static block runs
    public static void init() {}
}
```

`ConfigType.CLIENT` = per-client, not synced. `ConfigType.COMMON_SYNCED` = server-authoritative, synced to clients.

## 2. Categories & subcategories

`push("name")` opens a category, `pop()` closes it. Nest freely; each level is a section in the file and a sub-screen in
the UI.

```java
builder.push("mirror");
  // ... options ...
  builder.push("recursion");   // subcategory: mirror.recursion.*
    // ... options ...
  builder.pop();
builder.pop();
```

## 3. Value definers

`define(...)` is overloaded. Each returns a `Supplier` of the matching type:

```java
builder.define("flag", true);                       // boolean
builder.define("count", 8, 1, 32);                  // int (default, min, max)
builder.define("ratio", 2.0, 1.0, 16.0);            // double
builder.define("mode", MyEnum.DEFAULT);             // enum
builder.define("list", List.of("a", "b"));          // string list
```

There are many more (`defineColor`, `defineSlider`, `defineItem`, `defineBlock`, `defineRange`, `defineVec3`,
`defineObject(codec)`, `defineRegex`, ...). `comment(...)` can go before *or* after the `define`.

## 4. Icons

`icon(...)` decorates the **next** `push` (category row) or `define` (option row). A bare string uses your mod
namespace; it resolves lazily client-side as an item/block, so a name that isn't real simply shows no icon.

```java
builder.icon("television").push("television");   // category gets the television item icon
builder.icon("wave_gate").comment("...").define("ffmpeg_enabled", true); // option icon
```

## 5. Feature toggles (the checkmark switches)

A "feature" is a boolean that renders as a ✓/✗ switch with an icon, and **gates** things. The icon auto-infers from the
name (`modid:name`) unless you set one with `icon(...)`.

```java
// leaf feature: a single ✓/✗ toggle
CREEPER_DROP = builder.icon("cassette").comment("...").feature("creeper_drop", true);

// category master toggle: gates every option under the category (they gray out when off)
builder.push("mirror");
builder.comment("...");
MIRROR_ENABLED = builder.mainFeature();     // creates the "enabled" gate for this category
  MAX_SIZE = builder.comment("...").define("max_connected_size", 8, 1, 24);
builder.pop();

// shortcut for "open a gated category": push(name) + mainFeature()
SCREEN_EFFECTS = builder.pushFeature("screen_effects");
```

Feature suppliers are **effective** = own value AND every ancestor gate, composed at read time (toggling a parent never
rewrites stored child values). Non-feature children keep returning their raw `.get()` — the gate only grays them in the
UI, so still guard them in code (`if (SCREEN_EFFECTS.get()) ...`).

## 6. Reload markers

Tell the user (and the screen badge) that a change needs more than a hot reload. Call **immediately before the `define`
** it applies to; it's a single-shot flag consumed by the next value.

```java
builder.gameRestart().comment("...").define("update_fps", 10.0, 1, 60);  // needs full restart
builder.worldReload().comment("...").define("max_connected_size", 8, 1, 24); // needs world reload
builder.affectsDynamicPacks().worldReload().comment("...").define(...);      // also invalidates pack cache
```

Pick the tier honestly:

- **GAME_RESTART** — value is read once and cached at startup (e.g. baked into a `Suppliers.memoize`'d object). Nothing
  re-reads it.
- **WORLD_RELOAD** — value is baked at world/data load (recipe conditions, loot, multiblock structure cached in world
  data).
- **NONE** (default) — read live each use, applies immediately.

## 7. Gating recipes / loot / creative tabs by a config

Moonlight exposes a simple named recipe condition. Register one flag handler, then add the condition to any
recipe/advancement JSON.

```java
RegHelper.registerSimpleRecipeCondition(res("flag"), s -> switch (s) {
    case "mirror"       -> MyConfigs.isMirrorEnabled();
    case "picture_tape" -> MyConfigs.isPictureTapeEnabled();
    default             -> true;
});
```

```json
{
  "fabric:load_conditions": [ { "condition": "mymod:flag", "flag": "picture_tape" } ],
  "neoforge:conditions":    [ { "type": "mymod:flag",      "flag": "picture_tape" } ],
  "type": "minecraft:crafting_shaped",
  "...": "..."
}
```

Recipe conditions run at data load, so the backing config should be **WORLD_RELOAD + affectsDynamicPacks**. Gate
creative-tab entries and loot in code with the same helper.

## Gotchas (learned the hard way)

- **`worldReload()`/`gameRestart()` must precede a `define`, never a `push`.** On NeoForge the flag is forwarded
  straight into Forge's spec, which throws `Dangling restart value` if the next call is a `push`. So for a gated
  category, use `push(name)` → set the flag → `mainFeature()` rather than putting the flag before `pushFeature(name)`.
- **`SPEC.manuallySetValue(handle, value)` needs the raw `define` handle**, not a feature's effective supplier (that's a
  wrapped lambda and will throw). Keep a raw `Supplier` reference if you need to set a value programmatically.
- **Changing keys/category paths resets that value** to default on next load (old key is ignored). Fine in dev; be
  deliberate for released mods.
- **No manual lang needed** — readable names and comment descriptions are auto-registered. Hand-written
  `modid.configuration.*` keys are orphaned if you rename.
