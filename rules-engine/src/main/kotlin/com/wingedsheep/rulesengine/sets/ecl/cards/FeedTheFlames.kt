package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.DealDamageEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.targeting.TargetCreature

/**
 * Feed the Flames
 *
 * {3}{R} Instant
 * Feed the Flames deals 5 damage to target creature.
 * If that creature would die this turn, exile it instead.
 */
object FeedTheFlames {
    val definition = CardDefinition.instant(
        name = "Feed the Flames",
        manaCost = ManaCost.parse("{3}{R}"),
        oracleText = "Feed the Flames deals 5 damage to target creature. " +
                "If that creature would die this turn, exile it instead.",
        metadata = ScryfallMetadata(
            collectorNumber = "137",
            rarity = Rarity.COMMON,
            artist = "Xabi Gaztelua",
            imageUri = "https://cards.scryfall.io/normal/front/a/a/aa4a4a4a-4a4a-4a4a-4a4a-4a4a4a4a4a4a.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Feed the Flames") {
        targets(TargetCreature())

        // Deal 5 damage to target creature
        // TODO: "exile instead of dying" replacement effect needs infrastructure
        spell(
            DealDamageEffect(
                amount = 5,
                target = EffectTarget.TargetCreature
            )
        )
    }
}
