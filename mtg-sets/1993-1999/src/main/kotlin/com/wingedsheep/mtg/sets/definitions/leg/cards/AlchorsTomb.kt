package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Alchor's Tomb
 * {4}
 * Artifact
 * {2}, {T}: Target permanent you control becomes the color of your choice. (This effect lasts indefinitely.)
 */
val AlchorsTomb = card("Alchor's Tomb") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Target permanent you control becomes the color of your choice. (This effect lasts indefinitely.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val t = target(TargetFilter.PermanentYouControl)
        effect = Effects.ChooseColorThen(
            then = Effects.ChangeColorToChosen(t, Duration.Permanent),
            prompt = "Choose a color"
        )
        description = "{2}, {T}: Target permanent you control becomes the color of your choice."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "272"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4395b19-2118-4a09-8932-f9ce9bc54d6d.jpg?1783948029"
    }
}
