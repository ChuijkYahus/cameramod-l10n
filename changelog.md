- mirrors now drop their reflection texture to half then quarter resolution as the player moves away (two LOD steps),
  without changing the render distance cap.
- added Supernatural compat: its vampires (both mobs and players) are hidden from mirrors & camera feeds.
- added an ugly mixin in Simple Clouds mod because tey dont have an API to turn off their rendering & are using global
  states which get easily messed up when multiple cameras are there.
- improved network overlays for wave gate to differentiate between retries, not found errors or forbidden.
- added vampirism compat. theres a tag for that.