package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Viscera Seer
 * {B}
 * Creature — Vampire Wizard
 * 1/1
 * Sacrifice a creature: Scry 1. (Look at the top card of your library. You may put that card on the bottom.)
 *
 * One activated ability whose whole cost is a single [Costs.Sacrifice] over
 * `GameObjectFilter.Creature` — the sacrifice is the activation cost, not an effect, so nothing
 * else is spelled. `excludeSelf` is left at its default `false`: the Seer may eat itself, which is
 * what the printed ruling says. Scry rides the [Effects.Scry] facade (the `Patterns.Library.scry`
 * composition behind it), never a hand-rolled look-and-reorder pipeline.
 */
val VisceraSeer = card("Viscera Seer") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Wizard"
    power = 1
    toughness = 1
    oracleText = "Sacrifice a creature: Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Creature)
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "John Stanko"
        flavorText = "In matters of life and death, he trusts his gut."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6179f847-e334-4f7f-9a4e-0013942a394f.jpg"
        ruling("2010-08-15", "You can sacrifice Viscera Seer to activate its own ability.")
        ruling("2010-08-15", "If you sacrifice an attacking or blocking creature during the declare blockers step, it won't deal combat damage. If you wait until the combat damage step, but that creature is dealt lethal damage, it'll be destroyed before you get a chance to sacrifice it.")
    }
}
