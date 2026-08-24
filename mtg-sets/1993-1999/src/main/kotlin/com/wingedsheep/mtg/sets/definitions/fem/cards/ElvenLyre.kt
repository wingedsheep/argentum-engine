package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Elven Lyre
 * {2}
 * Artifact
 * {1}, {T}, Sacrifice this artifact: Target creature gets +2/+2 until end of turn.
 */
val ElvenLyre = card("Elven Lyre") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice this artifact: Target creature gets +2/+2 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        val t = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.ModifyStats(2, 2, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Kaja Foglio"
        flavorText = "Scholars are uncertain whether it was the actual sound or some other magical property of the Elven Lyre that transformed its player."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3a8cd72-04c0-46f7-a249-f1cecddfdc26.jpg?1783947881"
    }
}
