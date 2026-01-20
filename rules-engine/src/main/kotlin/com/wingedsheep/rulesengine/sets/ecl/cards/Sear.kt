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
 * Sear
 *
 * {1}{R} Instant
 * Sear deals 4 damage to target creature or planeswalker.
 */
object Sear {
    val definition = CardDefinition.instant(
        name = "Sear",
        manaCost = ManaCost.parse("{1}{R}"),
        oracleText = "Sear deals 4 damage to target creature or planeswalker.",
        metadata = ScryfallMetadata(
            collectorNumber = "154",
            rarity = Rarity.UNCOMMON,
            artist = "Lars Grant-West",
            imageUri = "https://cards.scryfall.io/normal/front/f/f/ff7f7f7f-7f7f-7f7f-7f7f-7f7f7f7f7f7f.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Sear") {
        // TODO: Should also target planeswalkers - needs TargetCreatureOrPlaneswalker
        targets(TargetCreature())

        spell(
            DealDamageEffect(
                amount = 4,
                target = EffectTarget.TargetCreature
            )
        )
    }
}
