package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.OnBecomesTapped
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Tributary Vaulter
 *
 * {2}{W} Creature — Merfolk Warrior 1/3
 * Flying
 * Whenever this creature becomes tapped, another target Merfolk you control
 * gets +2/+0 until end of turn.
 */
object TributaryVaulter {
    val definition = CardDefinition.creature(
        name = "Tributary Vaulter",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype.MERFOLK, Subtype.WARRIOR),
        power = 1,
        toughness = 3,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying\nWhenever this creature becomes tapped, another target Merfolk you control " +
                "gets +2/+0 until end of turn.",
        metadata = ScryfallMetadata(
            collectorNumber = "40",
            rarity = Rarity.COMMON,
            artist = "Jarel Threat",
            imageUri = "https://cards.scryfall.io/normal/front/b/b/bb1b1b1b-1b1b-1b1b-1b1b-1b1b1b1b1b1b.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Tributary Vaulter") {
        keywords(Keyword.FLYING)

        // When tapped, another Merfolk you control gets +2/+0
        // Note: Full implementation needs targeted triggered ability support with subtype filter
        // For now uses TargetControlledCreature - Merfolk restriction would need TargetValidator enhancement
        triggered(
            trigger = OnBecomesTapped(selfOnly = true),
            effect = ModifyStatsEffect(
                powerModifier = 2,
                toughnessModifier = 0,
                target = EffectTarget.TargetControlledCreature,
                untilEndOfTurn = true
            )
        )
    }
}
