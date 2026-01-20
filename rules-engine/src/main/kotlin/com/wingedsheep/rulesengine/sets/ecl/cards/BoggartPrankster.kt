package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.OnYouAttack
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Boggart Prankster
 *
 * {1}{B} Creature — Goblin Warrior 1/3
 * Whenever you attack, target attacking Goblin you control gets +1/+0 until end of turn.
 */
object BoggartPrankster {
    val definition = CardDefinition.creature(
        name = "Boggart Prankster",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype.GOBLIN, Subtype.WARRIOR),
        power = 1,
        toughness = 3,
        oracleText = "Whenever you attack, target attacking Goblin you control gets +1/+0 until end of turn.",
        metadata = ScryfallMetadata(
            collectorNumber = "93",
            rarity = Rarity.COMMON,
            artist = "Karl Kopinski",
            imageUri = "https://cards.scryfall.io/normal/front/f/f/ff2f2f2f-2f2f-2f2f-2f2f-2f2f2f2f2f2f.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Boggart Prankster") {
        // Whenever you attack, target attacking Goblin gets +1/+0
        // TODO: Triggered ability targeting needs TriggeredAbility targeting infrastructure
        // Target filter should be: Attacking Goblin you control
        triggered(
            trigger = OnYouAttack(),
            effect = ModifyStatsEffect(
                powerModifier = 1,
                toughnessModifier = 0,
                target = EffectTarget.TargetCreature,
                untilEndOfTurn = true
            )
        )
    }
}
