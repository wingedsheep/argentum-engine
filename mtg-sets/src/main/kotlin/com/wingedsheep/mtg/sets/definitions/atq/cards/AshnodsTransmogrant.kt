package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Ashnod's Transmogrant
 * {1}
 * Artifact
 * {T}, Sacrifice this artifact: Put a +1/+1 counter on target nonartifact creature.
 * That creature becomes an artifact in addition to its other types.
 *
 * Tap + sacrifice-self activated cost. The effect puts a +1/+1 counter on a nonartifact
 * creature and permanently adds the Artifact card type ([Effects.AddCardType]) in addition
 * to its existing types — the same compose-of-two pattern as Phyrexian Scriptures' first
 * chapter.
 */
val AshnodsTransmogrant = card("Ashnod's Transmogrant") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Sacrifice this artifact: Put a +1/+1 counter on target nonartifact creature. " +
        "That creature becomes an artifact in addition to its other types."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val creature = target(
            "target nonartifact creature",
            TargetCreature(filter = TargetFilter.Creature.nonartifact())
        )
        effect = Effects.Composite(listOf(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature),
            Effects.AddCardType("ARTIFACT", creature)
        ))
        description = "{T}, Sacrifice this artifact: Put a +1/+1 counter on target nonartifact creature. " +
            "That creature becomes an artifact in addition to its other types."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Mark Tedin"
        flavorText = "Ashnod found few willing to trade their humanity for the power she offered them."
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2aa5b289-36ba-49b1-a5ac-f23bf71f8241.jpg?1562904041"
    }
}
