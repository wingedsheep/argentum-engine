package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.TimingRestriction
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Timid Shieldbearer
 *
 * {1}{W} Creature — Kithkin Soldier 2/2
 * {4}{W}: Creatures you control get +1/+1 until end of turn.
 */
object TimidShieldbearer {
    val definition = CardDefinition.creature(
        name = "Timid Shieldbearer",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype.KITHKIN, Subtype.SOLDIER),
        power = 2,
        toughness = 2,
        oracleText = "{4}{W}: Creatures you control get +1/+1 until end of turn.",
        metadata = ScryfallMetadata(
            collectorNumber = "39",
            rarity = Rarity.COMMON,
            artist = "Caio Monteiro",
            imageUri = "https://cards.scryfall.io/normal/front/e/e/ee4e8a7c-5e9e-4e7e-8d5e-3e5e8e8e8e8e.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Timid Shieldbearer") {
        // Activated ability: {4}{W}: Creatures you control get +1/+1 until end of turn
        activated(
            cost = AbilityCost.Mana(white = 1, generic = 4),
            effect = ModifyStatsEffect(
                powerModifier = 1,
                toughnessModifier = 1,
                target = EffectTarget.AllControlledCreatures,
                untilEndOfTurn = true
            ),
            timing = TimingRestriction.INSTANT
        )
    }
}
