package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost

/**
 * Bogslither's Embrace
 * {1}{B}
 * Sorcery
 *
 * As an additional cost to cast this spell, blight 1 or pay {3}.
 * (To blight 1, put a -1/-1 counter on a creature you control.)
 * Exile target creature.
 */
val BogslithersEmbrace = card("Bogslither's Embrace") {
    manaCost = "{1}{B}"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, blight 1 or pay {3}. " +
        "(To blight 1, put a -1/-1 counter on a creature you control.)\n" +
        "Exile target creature."

    additionalCost(AdditionalCost.BlightOrPay(blightAmount = 1, alternativeManaCost = "{3}"))

    spell {
        val creature = target("creature", Targets.Creature)
        effect = Effects.Exile(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Justin Gerard"
        flavorText = "Shadow creatures are untamable forces of nature—chaotic and hungry."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4beca2e7-9c6d-493b-b15f-69e483a8dfff.jpg?1767957120"
    }
}
