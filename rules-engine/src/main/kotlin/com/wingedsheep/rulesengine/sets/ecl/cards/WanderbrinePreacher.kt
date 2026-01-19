package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GainLifeEffect
import com.wingedsheep.rulesengine.ability.OnBecomesTapped
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Wanderbrine Preacher
 *
 * {1}{W} Creature — Merfolk Cleric 2/2
 * Whenever this creature becomes tapped, you gain 2 life.
 */
object WanderbrinePreacher {
    val definition = CardDefinition.creature(
        name = "Wanderbrine Preacher",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype.MERFOLK, Subtype.CLERIC),
        power = 2,
        toughness = 2,
        oracleText = "Whenever this creature becomes tapped, you gain 2 life.",
        metadata = ScryfallMetadata(
            collectorNumber = "41",
            rarity = Rarity.COMMON,
            artist = "Kev Fang",
            imageUri = "https://cards.scryfall.io/normal/front/e/e/ee5e5e5e-5e5e-5e5e-5e5e-5e5e5e5e5e5e.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Wanderbrine Preacher") {
        // Whenever tapped, gain 2 life
        triggered(
            trigger = OnBecomesTapped(selfOnly = true),
            effect = GainLifeEffect(
                amount = 2,
                target = EffectTarget.Controller
            )
        )
    }
}
