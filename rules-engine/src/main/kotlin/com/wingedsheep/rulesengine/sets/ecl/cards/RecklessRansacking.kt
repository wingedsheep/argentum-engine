package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CreateTokenEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.targeting.TargetCreature

/**
 * Reckless Ransacking
 *
 * {1}{R} Instant
 * Target creature gets +3/+2 until end of turn. Create a Treasure token.
 */
object RecklessRansacking {
    val definition = CardDefinition.instant(
        name = "Reckless Ransacking",
        manaCost = ManaCost.parse("{1}{R}"),
        oracleText = "Target creature gets +3/+2 until end of turn. Create a Treasure token.",
        metadata = ScryfallMetadata(
            collectorNumber = "152",
            rarity = Rarity.COMMON,
            artist = "Daren Bader",
            imageUri = "https://cards.scryfall.io/normal/front/e/e/ee6e6e6e-6e6e-6e6e-6e6e-6e6e6e6e6e6e.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Reckless Ransacking") {
        targets(TargetCreature())

        spell(
            ModifyStatsEffect(
                powerModifier = 3,
                toughnessModifier = 2,
                target = EffectTarget.TargetCreature,
                untilEndOfTurn = true
            ) then CreateTokenEffect(
                count = 1,
                power = 0,
                toughness = 0,
                colors = emptySet(),  // Colorless
                creatureTypes = setOf("Treasure")  // TODO: Needs proper artifact token support
            )
        )
    }
}
