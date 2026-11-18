package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.CounterType
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
 * Solidarity of Heroes — Journey into Nyx #141 (canonical printing)
 * {2}{G} · Instant
 *
 * Strive — This spell costs {1}{G} more to cast for each target beyond the first.
 * Choose any number of target creatures. Double the number of +1/+1 counters on each of them.
 *
 * Strive is one [CostModification.IncreaseManaPerUnit] of {1}{G} per
 * [CostReductionSource.ChosenTargets] beyond the first (same shape as Ajani's Presence).
 * "Any number" is an unlimited target count with no floor; any creature is a legal target, with or
 * without counters (ruling). Each still-legal target has its +1/+1 counters doubled via
 * [Effects.DoubleCounters]; illegal ones are skipped.
 */
val SolidarityOfHeroes = card("Solidarity of Heroes") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Strive — This spell costs {1}{G} more to cast for each target beyond the first.\n" +
        "Choose any number of target creatures. Double the number of +1/+1 counters on each of them."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseManaPerUnit(
                symbols = "{1}{G}",
                countSource = CostReductionSource.ChosenTargets(beyond = 1),
            ),
        )
    }

    spell {
        target = TargetObject(filter = TargetFilter.Creature, unlimited = true)
        effect = Effects.ForEachTarget(
            Effects.DoubleCounters(CounterType.PLUS_ONE_PLUS_ONE, EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9ba88e10-d306-44df-bdbf-f5ba85887fe8.jpg?1783939408"
        ruling("2014-04-26", "Solidarity of Heroes can target any creatures, not just ones with +1/+1 counters on them. Notably, heroic abilities of any target creatures that put +1/+1 counters on that creature will resolve before Solidarity of Heroes.")
        ruling("2014-04-26", "You choose how many targets each spell with a strive ability has and what those targets are as you cast it. It's legal to cast such a spell with no targets, although this is rarely a good idea. You can't choose the same target more than once for a single strive spell.")
        ruling("2014-04-26", "The mana cost and mana value of strive spells don't change no matter how many targets they have. Strive abilities affect only what you pay.")
        ruling("2014-04-26", "If all of the spell's targets are illegal when the spell tries to resolve, it won't resolve and none of its effects will happen. If one or more of its targets are legal when it tries to resolve, the spell will resolve and affect only those legal targets. It will have no effect on any illegal targets.")
        ruling("2014-04-26", "If such a spell is copied, and the effect that copies the spell allows a player to choose new targets for the copy, the number of targets can't be changed. The player may change any number of the targets, including all of them or none of them. If, for one of the targets, the player can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2014-04-26", "If a spell or ability allows you to cast a strive spell without paying its mana cost, you must pay the additional costs for any targets beyond the first.")
    }
}
