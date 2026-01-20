package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.TimingRestriction
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.targeting.CreatureTargetFilter
import com.wingedsheep.rulesengine.targeting.TargetCreature

/**
 * Surly Farrier
 *
 * {1}{G} Creature — Kithkin Citizen 2/2
 * {T}: Target creature you control gets +1/+1 and gains vigilance until end of turn.
 * Activate only as a sorcery.
 */
object SurlyFarrier {
    val definition = CardDefinition.creature(
        name = "Surly Farrier",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.KITHKIN, Subtype.CITIZEN),
        power = 2,
        toughness = 2,
        oracleText = "{T}: Target creature you control gets +1/+1 and gains vigilance until end of turn. Activate only as a sorcery.",
        metadata = ScryfallMetadata(
            collectorNumber = "196",
            rarity = Rarity.COMMON,
            artist = "Jake Murray",
            imageUri = "https://cards.scryfall.io/normal/front/c/c/ccdd3456-7890-1234-efgh-ccdd34567890.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Surly Farrier") {
        val creatureTarget = targets(
            TargetCreature(filter = CreatureTargetFilter.YouControl)
        )

        // {T}: Target creature you control gets +1/+1 and gains vigilance until end of turn
        // Activate only as a sorcery
        activated(
            cost = AbilityCost.Tap,
            effect = ModifyStatsEffect(
                powerModifier = 1,
                toughnessModifier = 1,
                target = EffectTarget.ContextTarget(creatureTarget.index),
                untilEndOfTurn = true
            ) then GrantKeywordUntilEndOfTurnEffect(
                keyword = Keyword.VIGILANCE,
                target = EffectTarget.ContextTarget(creatureTarget.index)
            ),
            timing = TimingRestriction.SORCERY
        )
    }
}
