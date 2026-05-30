package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tidal Visionary
 * {U}
 * Creature — Merfolk Wizard
 * 1/1
 * {T}: Target creature becomes the color of your choice until end of turn.
 *
 * Reuses the single-color recolor pattern from Blind Seer: [Effects.ChooseColorThen]
 * pauses for a color choice, then [Effects.ChangeColorToChosen] applies the Layer-5
 * color change for the rest of the turn.
 */
val TidalVisionary = card("Tidal Visionary") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 1
    toughness = 1
    oracleText = "{T}: Target creature becomes the color of your choice until end of turn."

    activatedAbility {
        cost = Costs.Tap
        val t = target("target", Targets.Creature)
        effect = Effects.ChooseColorThen(
            then = Effects.ChangeColorToChosen(t),
            prompt = "Choose a color"
        )
        description = "{T}: Target creature becomes the color of your choice until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Glen Angus"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a72a3051-7f46-4b6b-b4fb-0f170d9687ab.jpg?1562928768"
    }
}
