package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deep Spawn
 * {5}{U}{U}{U}
 * Creature — Homarid
 * 6/6
 * Trample
 * At the beginning of your upkeep, sacrifice this creature unless you mill two cards.
 * {U}: This creature gains shroud until end of turn and doesn't untap during your next untap step.
 * Tap this creature.
 *
 * The upkeep clause is an alternative *cost*, not a choice of effects: milling two is the price
 * of keeping it, so an empty library means the sacrifice happens. Shares its activated ability
 * with [HomaridWarrior].
 */
val DeepSpawn = card("Deep Spawn") {
    manaCost = "{5}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Homarid"
    oracleText = "Trample\n" +
        "At the beginning of your upkeep, sacrifice this creature unless you mill two cards.\n" +
        "{U}: This creature gains shroud until end of turn and doesn't untap during your next " +
        "untap step. Tap this creature. (A creature with shroud can't be the target of spells or abilities.)"
    power = 6
    toughness = 6

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = PayOrSufferEffect(
            cost = Costs.pay.Atom(CostAtom.Mill(2)),
            suffer = SacrificeSelfEffect,
        )
        description = "At the beginning of your upkeep, sacrifice this creature unless you mill two cards."
    }

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.SHROUD, EffectTarget.Self),
            GrantKeywordEffect(
                AbilityFlag.DOESNT_UNTAP.name,
                EffectTarget.Self,
                Duration.UntilAfterAffectedControllersNextUntap,
            ),
            Effects.Tap(EffectTarget.Self),
        )
        description = "{U}: This creature gains shroud until end of turn and doesn't untap during your next untap step. Tap this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69c9e4a5-735f-471c-ab1a-6e6d50ba5724.jpg?1783947913"
    }
}
