# Dynamic Nutrition

Five nutrients, tracked from everything you eat, with values modelled on real nutritional data.
For Minecraft **26.1** and **26.2** on **Fabric** and **NeoForge**.

Hunger tells you how long since you last ate. It says nothing about what you ate. Dynamic Nutrition
adds the other half: carbohydrates, protein, fat, vitamins and minerals, each filled by different
foods and each drained at its own rate.

## How it works

- **Every food is worth real amounts of five nutrients.** A carrot is worth far more vitamins than
  cooked beef, and cooked beef is worth far more protein than a carrot, because the numbers come
  from actual per-100g composition rather than from a category label.
- **Nutrients drain at different speeds.** Carbohydrates burn fastest, fat is storage and barely
  moves. Five bars that emptied together would be one bar with five labels.
- **Eating the same thing over and over is worth less.** Of your last several meals, each repeat of
  a food costs part of its value, down to a floor it never drops below. Variety is the mechanic, and
  it affects nutrition only, never hunger.
- **A balanced diet is rewarded and a poor one is not.** Keep everything up and you get extra
  maximum health; run short of something and you lose some, along with a little speed and damage.
  The effect names the nutrient you are short of, so there is never a mystery debuff.

## Where to look

- **Press N** to open the nutrition screen, or use the small button in your inventory. The keybind
  is the primary route, so if the button ever collides with another mod you can simply turn it off.
- **A strip beside the hunger bar** shows all five at a glance. Each section is ten pixels, so one
  pixel is worth exactly ten points.
- **Food tooltips** show what that food is worth to you right now. Hold F3 and H for where the value
  came from.
- **The bars are marked** with the two thresholds for that nutrient. The marks turn white once you
  are past them.

## Any food mod works, with no configuration

Values are resolved in four stages. Explicit data first, then item tags, then the recipe graph, so
bread is a carbohydrate because wheat is. Anything still unresolved is worked out from the food
convention tags that both loaders ship, which is why a food from a mod nobody has written a compat
patch for still feeds you something sensible.

## Configuration

Cloth Config is **optional**. Install it for a settings screen covering the HUD strip, the inventory
button, tooltips, the variety mechanic and the status effects, including how many hearts they are
worth. Without it the mod runs on its defaults.

## Commands

| Command | Who | What |
|---|---|---|
| `/dynamicnutrition get` | anyone | your current levels |
| `/dynamicnutrition variety` | anyone | your recent meals, and what your held food is worth |
| `/dynamicnutrition unassigned` | anyone | foods that resolved to no nutrients |
| `/dynamicnutrition export` | operators | the whole resolved table as a CSV, for pack authors |

## Building

JDK 25.

```
./gradlew build
```

Jars land in `fabric/build/libs` and `neoforge/build/libs`.

## Licence

PolyForm Shield 1.0.0. See `LICENSE`, and `NOTICE.txt` for third party material.
