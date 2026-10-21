package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stuffed Bear
 * {2}
 * Artifact — Common (MID #259)
 *
 * {2}: This artifact becomes a 4/4 green Bear artifact creature until end of turn.
 */
val StuffedBear = card("Stuffed Bear") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{2}: This artifact becomes a 4/4 green Bear artifact creature until end of turn."

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 4,
            creatureTypes = setOf("Bear"),
            addTypes = setOf("ARTIFACT"),
            colors = setOf("GREEN"),
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "259"
        artist = "Joe Slucher"
        flavorText = "It takes a taxidermist of unusual skill to cleanly stuff and mount a beast without severing its primal, predatory drive."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4cd40276-016b-475f-906f-74d31dceaf70.jpg?1783925545"
    }
}
