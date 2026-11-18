package com.wingedsheep.mtg.sets.definitions.jou.cards

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
 * Colossal Heroics — Journey into Nyx #118 (canonical printing)
 * {2}{G} · Instant
 *
 * Strive — This spell costs {1}{G} more to cast for each target beyond the first.
 * Any number of target creatures each get +2/+2 until end of turn. Untap those creatures.
 *
 * Strive is one [CostModification.IncreaseManaPerUnit] of {1}{G} per
 * [CostReductionSource.ChosenTargets] beyond the first. Each still-legal target gets the buff and
 * is untapped; illegal targets are skipped (CR 608.2b). Untapped creatures are legal targets too.
 */
val ColossalHeroics = card("Colossal Heroics") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Strive — This spell costs {1}{G} more to cast for each target beyond the first.\n" +
        "Any number of target creatures each get +2/+2 until end of turn. Untap those creatures."

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
            Effects.ModifyStats(2, 2, EffectTarget.ContextTarget(0)),
            Effects.Untap(EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6139443-e31c-4eea-a99e-df9498fe18a8.jpg?1783939416"
        ruling("2014-04-26", "Colossal Heroics can target any creatures, not just ones that are tapped.")
        ruling("2014-04-26", "You choose how many targets each spell with a strive ability has and what those targets are as you cast it. It's legal to cast such a spell with no targets, although this is rarely a good idea. You can't choose the same target more than once for a single strive spell.")
        ruling("2014-04-26", "The mana cost and mana value of strive spells don't change no matter how many targets they have. Strive abilities affect only what you pay.")
        ruling("2014-04-26", "If all of the spell's targets are illegal when the spell tries to resolve, it won't resolve and none of its effects will happen. If one or more of its targets are legal when it tries to resolve, the spell will resolve and affect only those legal targets. It will have no effect on any illegal targets.")
        ruling("2014-04-26", "If a spell or ability allows you to cast a strive spell without paying its mana cost, you must pay the additional costs for any targets beyond the first.")
    }
}
