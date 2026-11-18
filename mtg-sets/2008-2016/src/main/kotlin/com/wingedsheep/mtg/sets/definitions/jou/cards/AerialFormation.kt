package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Aerial Formation — Journey into Nyx #30
 * {U} · Instant
 *
 * Strive — This spell costs {2}{U} more to cast for each target beyond the first.
 * Any number of target creatures each get +1/+1 and gain flying until end of turn.
 *
 * Strive is one [CostModification.IncreaseManaPerUnit] of {2}{U} counting
 * [CostReductionSource.ChosenTargets] with `beyond = 1` (mana value stays 1). [Effects.ForEachTarget]
 * pumps and grants flying to each chosen creature on its own, so an illegal target simply misses out.
 */
val AerialFormation = card("Aerial Formation") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Strive — This spell costs {2}{U} more to cast for each target beyond the first.\n" +
        "Any number of target creatures each get +1/+1 and gain flying until end of turn."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseManaPerUnit(
                symbols = "{2}{U}",
                countSource = CostReductionSource.ChosenTargets(beyond = 1),
            ),
        )
    }

    spell {
        target = TargetObject(filter = TargetFilter.Creature, unlimited = true)
        effect = Effects.ForEachTarget(
            Effects.ModifyStats(1, 1, EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fce86785-bc03-451e-8422-8bb53082cc12.jpg?1783939451"
        ruling(
            "2014-04-26",
            "You choose how many targets each spell with a strive ability has and what those targets are as you cast it. It's legal to cast such a spell with no targets, although this is rarely a good idea. You can't choose the same target more than once for a single strive spell.",
        )
        ruling(
            "2014-04-26",
            "The mana cost and mana value of strive spells don't change no matter how many targets they have. Strive abilities affect only what you pay.",
        )
        ruling(
            "2014-04-26",
            "If a spell or ability allows you to cast a strive spell without paying its mana cost, you must pay the additional costs for any targets beyond the first.",
        )
    }
}
