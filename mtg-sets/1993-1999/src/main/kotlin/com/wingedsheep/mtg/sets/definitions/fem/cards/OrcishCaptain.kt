package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.FlipCoinEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Orcish Captain
 * {R}
 * Creature — Orc Warrior
 * 1/1
 * {1}: Flip a coin. If you win the flip, target Orc creature gets +2/+0 until end of turn. If you
 * lose the flip, it gets -0/-2 until end of turn.
 *
 * One target, chosen when the ability is activated; the flip only picks which modification the
 * target receives, so a target that becomes illegal fizzles the whole ability either way.
 */
val OrcishCaptain = card("Orcish Captain") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Warrior"
    oracleText = "{1}: Flip a coin. If you win the flip, target Orc creature gets +2/+0 until end " +
        "of turn. If you lose the flip, it gets -0/-2 until end of turn."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{1}")
        val t = target(
            "target Orc creature",
            TargetCreature(filter = TargetFilter.Creature.withSubtype(Subtype.ORC))
        )
        effect = FlipCoinEffect(
            wonEffect = ModifyStatsEffect(2, 0, t),
            lostEffect = ModifyStatsEffect(0, -2, t),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Mark Tedin"
        flavorText = "There's a chance to win every battle."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e43cf61d-b4d6-4461-a228-47fd8b026d33.jpg?1783947892"
    }
}
