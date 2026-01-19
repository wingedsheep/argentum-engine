package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.ReturnToHandEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Disruptor of Currents
 *
 * {3}{U}{U} Creature — Merfolk Wizard 3/3
 * Flash
 * Convoke
 * When this creature enters, return up to one other target nonland permanent
 * to its owner's hand.
 */
object DisruptorOfCurrents {
    val definition = CardDefinition.creature(
        name = "Disruptor of Currents",
        manaCost = ManaCost.parse("{3}{U}{U}"),
        subtypes = setOf(Subtype.MERFOLK, Subtype.WIZARD),
        power = 3,
        toughness = 3,
        keywords = setOf(Keyword.FLASH, Keyword.CONVOKE),
        oracleText = "Flash\nConvoke\nWhen this creature enters, return up to one other target " +
                "nonland permanent to its owner's hand.",
        metadata = ScryfallMetadata(
            collectorNumber = "47",
            rarity = Rarity.RARE,
            artist = "Caio Monteiro",
            imageUri = "https://cards.scryfall.io/normal/front/c/c/cc8c8c8c-8c8c-8c8c-8c8c-8c8c8c8c8c8c.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Disruptor of Currents") {
        keywords(Keyword.FLASH, Keyword.CONVOKE)

        // ETB: Return up to one other target nonland permanent to hand
        triggered(
            trigger = OnEnterBattlefield(),
            effect = ReturnToHandEffect(
                target = EffectTarget.TargetNonlandPermanent
            ),
            optional = true  // "up to one"
        )
    }
}
