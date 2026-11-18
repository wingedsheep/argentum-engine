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
 * Ajani's Presence — Journey into Nyx #2 (canonical printing)
 * {W} · Instant
 *
 * Strive — This spell costs {2}{W} more to cast for each target beyond the first.
 * Any number of target creatures each get +1/+1 and gain indestructible until end of turn.
 *
 * Strive is one [CostModification.IncreaseManaPerUnit] of {2}{W} per
 * [CostReductionSource.ChosenTargets] beyond the first. It is a cost increase, so a free cast still
 * owes it and the mana value stays 1.
 *
 * "Any number" is an unlimited target count with no floor — zero targets is a legal cast (ruling).
 * Each still-legal target gets the buff on resolution; illegal ones are skipped (CR 608.2b).
 */
val AjanisPresence = card("Ajani's Presence") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Strive — This spell costs {2}{W} more to cast for each target beyond the first.\n" +
        "Any number of target creatures each get +1/+1 and gain indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy them.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseManaPerUnit(
                symbols = "{2}{W}",
                countSource = CostReductionSource.ChosenTargets(beyond = 1),
            ),
        )
    }

    spell {
        target = TargetObject(filter = TargetFilter.Creature, unlimited = true)
        effect = Effects.ForEachTarget(
            Effects.ModifyStats(1, 1, EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e77e2fd5-5c46-4f6b-ac43-ec23fab57a1a.jpg?1783939464"
        ruling("2014-04-26", "You choose how many targets each spell with a strive ability has and what those targets are as you cast it. It's legal to cast such a spell with no targets, although this is rarely a good idea. You can't choose the same target more than once for a single strive spell.")
        ruling("2014-04-26", "The mana cost and mana value of strive spells don't change no matter how many targets they have. Strive abilities affect only what you pay.")
        ruling("2014-04-26", "If all of the spell's targets are illegal when the spell tries to resolve, it won't resolve and none of its effects will happen. If one or more of its targets are legal when it tries to resolve, the spell will resolve and affect only those legal targets. It will have no effect on any illegal targets.")
        ruling("2014-04-26", "If a spell or ability allows you to cast a strive spell without paying its mana cost, you must pay the additional costs for any targets beyond the first.")
    }
}
