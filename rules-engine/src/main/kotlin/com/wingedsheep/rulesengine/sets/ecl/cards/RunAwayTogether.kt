package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CompositeEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ReturnToHandEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.targeting.CreatureTargetFilter
import com.wingedsheep.rulesengine.targeting.TargetCreature

/**
 * Run Away Together
 *
 * {1}{U} Instant
 * Choose two target creatures controlled by different players.
 * Return those creatures to their owners' hands.
 */
object RunAwayTogether {
    val definition = CardDefinition.instant(
        name = "Run Away Together",
        manaCost = ManaCost.parse("{1}{U}"),
        oracleText = "Choose two target creatures controlled by different players. " +
                "Return those creatures to their owners' hands.",
        metadata = ScryfallMetadata(
            collectorNumber = "67",
            rarity = Rarity.COMMON,
            artist = "Annie Stegg",
            imageUri = "https://cards.scryfall.io/normal/front/a/a/aa1a1a1a-1a1a-1a1a-1a1a-1a1a1a1a1a1a.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Run Away Together") {
        // Target one creature you control and one opponent controls
        // TODO: "different players" constraint needs TargetConstraint infrastructure
        targets(
            TargetCreature(filter = CreatureTargetFilter.YouControl),
            TargetCreature(filter = CreatureTargetFilter.OpponentControls)
        )

        // Return both to hands
        spell(
            CompositeEffect(
                effects = listOf(
                    ReturnToHandEffect(target = EffectTarget.ContextTarget(0)),
                    ReturnToHandEffect(target = EffectTarget.ContextTarget(1))
                )
            )
        )
    }
}
