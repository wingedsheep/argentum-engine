package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Setessan Tactics — Journey into Nyx #140 (canonical printing)
 * {1}{G} · Instant
 *
 * Strive — This spell costs {G} more to cast for each target beyond the first.
 * Until end of turn, any number of target creatures each get +1/+1 and gain
 * "{T}: This creature fights another target creature."
 *
 * Strive is one [CostModification.IncreaseManaPerUnit] of {G} per
 * [CostReductionSource.ChosenTargets] beyond the first. Each still-legal target gets +1/+1 and the
 * granted tap ability until end of turn; the ability's own target excludes the creature that
 * activates it ("another target creature").
 */
val SetessanTactics = card("Setessan Tactics") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Strive — This spell costs {G} more to cast for each target beyond the first.\n" +
        "Until end of turn, any number of target creatures each get +1/+1 and gain " +
        "\"{T}: This creature fights another target creature.\""

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseManaPerUnit(
                symbols = "{G}",
                countSource = CostReductionSource.ChosenTargets(beyond = 1),
            ),
        )
    }

    spell {
        target = TargetObject(filter = TargetFilter.Creature, unlimited = true)
        effect = Effects.ForEachTarget(
            Effects.ModifyStats(1, 1, EffectTarget.ContextTarget(0)),
            Effects.GrantActivatedAbility(
                ability = grantedActivatedAbility {
                    cost = Costs.Tap
                    val other = target(TargetFilter.OtherCreature)
                    effect = Effects.Fight(EffectTarget.Self, other)
                    description = "This creature fights another target creature."
                },
                target = EffectTarget.ContextTarget(0),
                duration = Duration.EndOfTurn,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "140"
        artist = "Mark Winters"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/385a1939-15e5-4f13-b01b-7cdc6835da2a.jpg"
        ruling("2014-04-26", "Each of the two creatures that fight deals damage equal to its power to the other. If either one of the creatures isn't on the battlefield when the ability that instructs them to fight resolves, no damage is dealt.")
        ruling("2014-04-26", "You choose how many targets each spell with a strive ability has and what those targets are as you cast it. It's legal to cast such a spell with no targets, although this is rarely a good idea. You can't choose the same target more than once for a single strive spell.")
        ruling("2014-04-26", "The mana cost and mana value of strive spells don't change no matter how many targets they have. Strive abilities affect only what you pay.")
        ruling("2014-04-26", "If all of the spell's targets are illegal when the spell tries to resolve, it won't resolve and none of its effects will happen. If one or more of its targets are legal when it tries to resolve, the spell will resolve and affect only those legal targets. It will have no effect on any illegal targets.")
        ruling("2014-04-26", "If such a spell is copied, and the effect that copies the spell allows a player to choose new targets for the copy, the number of targets can't be changed. The player may change any number of the targets, including all of them or none of them. If, for one of the targets, the player can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2014-04-26", "If a spell or ability allows you to cast a strive spell without paying its mana cost, you must pay the additional costs for any targets beyond the first.")
    }
}
