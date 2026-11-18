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
 * Rouse the Mob — Journey into Nyx #109 (canonical printing)
 * {R} · Instant
 *
 * Strive — This spell costs {2}{R} more to cast for each target beyond the first.
 * Any number of target creatures each get +2/+0 and gain trample until end of turn.
 *
 * Same shape as Ajani's Presence: strive is a per-target-beyond-the-first cost increase, and
 * "any number" is an unlimited target count with no floor.
 */
val RouseTheMob = card("Rouse the Mob") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Strive — This spell costs {2}{R} more to cast for each target beyond the first.\n" +
        "Any number of target creatures each get +2/+0 and gain trample until end of turn."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseManaPerUnit(
                symbols = "{2}{R}",
                countSource = CostReductionSource.ChosenTargets(beyond = 1),
            ),
        )
    }

    spell {
        target = TargetObject(filter = TargetFilter.Creature, unlimited = true)
        effect = Effects.ForEachTarget(
            Effects.ModifyStats(2, 0, EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "John Stanko"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64a07e7d-e203-4ecd-98bf-f8ef08c38d78.jpg?1783939421"
        ruling("2014-04-26", "You choose how many targets each spell with a strive ability has and what those targets are as you cast it. It's legal to cast such a spell with no targets, although this is rarely a good idea. You can't choose the same target more than once for a single strive spell.")
        ruling("2014-04-26", "The mana cost and mana value of strive spells don't change no matter how many targets they have. Strive abilities affect only what you pay.")
        ruling("2014-04-26", "If all of the spell's targets are illegal when the spell tries to resolve, it won't resolve and none of its effects will happen. If one or more of its targets are legal when it tries to resolve, the spell will resolve and affect only those legal targets. It will have no effect on any illegal targets.")
        ruling("2014-04-26", "If such a spell is copied, and the effect that copies the spell allows a player to choose new targets for the copy, the number of targets can't be changed. The player may change any number of the targets, including all of them or none of them. If, for one of the targets, the player can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2014-04-26", "If a spell or ability allows you to cast a strive spell without paying its mana cost, you must pay the additional costs for any targets beyond the first.")
    }
}
