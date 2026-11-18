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
 * Phalanx Formation — Journey into Nyx #21
 * {2}{W} · Instant
 *
 * Strive — This spell costs {1}{W} more to cast for each target beyond the first.
 * Any number of target creatures each gain double strike until end of turn.
 *
 * Strive is Call the Coppercoats' split tax: the generic {1} through
 * [CostModification.IncreaseGenericBy] and the {W} through [CostModification.IncreaseColoredPerUnit],
 * both counting [CostReductionSource.ChosenTargets] with `beyond = 1`. They're cost increases, so a
 * free cast still owes them and the mana value stays 3. [Effects.ForEachTarget] grants double strike
 * to each chosen creature on its own, so a target that became illegal simply misses out.
 */
val PhalanxFormation = card("Phalanx Formation") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Strive — This spell costs {1}{W} more to cast for each target beyond the first.\n" +
        "Any number of target creatures each gain double strike until end of turn. " +
        "(They deal both first-strike and regular combat damage.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseGenericBy(CostReductionSource.ChosenTargets(beyond = 1)),
        )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseColoredPerUnit(
                symbols = "{W}",
                countSource = CostReductionSource.ChosenTargets(beyond = 1),
            ),
        )
    }

    spell {
        target = TargetObject(filter = TargetFilter.Creature, unlimited = true)
        effect = Effects.ForEachTarget(
            Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e9aaba1-e480-4667-b3c9-d5665a3b4806.jpg?1783939455"
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
