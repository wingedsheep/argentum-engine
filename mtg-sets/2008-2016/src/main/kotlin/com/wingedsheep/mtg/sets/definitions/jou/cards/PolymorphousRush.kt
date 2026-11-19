package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Polymorphous Rush — Journey into Nyx #46
 * {2}{U} · Instant
 *
 * Strive — This spell costs {1}{U} more to cast for each target beyond the first.
 * Choose a creature on the battlefield. Any number of target creatures you control each become a
 * copy of that creature until end of turn.
 *
 * Strive is [CostModification.IncreaseManaPerUnit] of {1}{U} per
 * [CostReductionSource.ChosenTargets] beyond the first (Hour of Need's shape); the mana value
 * stays 3.
 *
 * The creature to copy is *chosen*, not targeted, as the spell resolves (ruling) — a
 * `nonTargeting` [com.wingedsheep.sdk.dsl.PipelineBuilder.selectTarget] over any creature on the
 * battlefield, so hexproof doesn't stop it and it may be one of the targets. The still-legal
 * targets (CR 608.2b drops illegal ones from the resolving target list) are gathered with
 * [CardSource.ChosenTargets] and each becomes a copy via [Effects.EachPermanentBecomesCopyOfTarget]
 * with `affected = IterationEntity`. [Effects.ForEachInCollection] keeps the outer pipeline
 * collections visible to the body (unlike `ForEachTarget`, which wipes them), so every iteration
 * reads the same chosen creature. Copying reads copiable values only (CR 707.2), and the copy
 * reverts at end of turn.
 */
val PolymorphousRush = card("Polymorphous Rush") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Strive — This spell costs {1}{U} more to cast for each target beyond the first.\n" +
        "Choose a creature on the battlefield. Any number of target creatures you control each become " +
        "a copy of that creature until end of turn."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseManaPerUnit(
                symbols = "{1}{U}",
                countSource = CostReductionSource.ChosenTargets(beyond = 1),
            ),
        )
    }

    spell {
        target = TargetObject(filter = TargetFilter.CreatureYouControl, unlimited = true)
        effect = Effects.Pipeline {
            val copySource = selectTarget(
                TargetObject(filter = TargetFilter.Creature),
                nonTargeting = true,
                prompt = "Choose a creature on the battlefield for your targeted creatures to copy",
            )
            val targeted = gather(CardSource.ChosenTargets)
            run(
                Effects.ForEachInCollection(
                    targeted,
                    Effects.EachPermanentBecomesCopyOfTarget(
                        target = copySource.asTarget,
                        duration = Duration.EndOfTurn,
                        affected = EffectTarget.IterationEntity,
                    ),
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Ryan Barger"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2ff5f703-339f-430c-b342-77bcc2c63222.jpg?1783939445"
        ruling("2014-04-26", "The creature that's copied isn't targeted by Polymorphous Rush. You choose that creature as the spell resolves. It can be one of the targeted creatures. A creature can become a copy of itself this way, though this usually won't have any noticeable effect.")
        ruling("2014-04-26", "The target creatures copy the printed values of the chosen creature, plus any copy effects that have been applied to it. They won't copy counters on that creature or effects that have changed the creature's power, toughness, types, color, or so on.")
        ruling("2014-04-26", "If the creatures copy a creature that's copying a creature, they will become whatever the chosen creature is copying.")
        ruling("2014-04-26", "This effect can cause the targets to stop being creatures. For example, if they each become a copy of a land that has become a creature, the effect that turned that land into a creature won't be copied. The targets will become noncreature lands.")
        ruling("2014-04-26", "Effects that have already applied to the target creatures will continue to apply to them. For example, if Giant Growth had given one of those creatures +3/+3 earlier in the turn, then Polymorphous Rush makes it a copy of Pensive Minotaur (a 2/3 creature), it will become a 5/6 Pensive Minotaur.")
        ruling("2014-04-26", "You choose how many targets each spell with a strive ability has and what those targets are as you cast it. It's legal to cast such a spell with no targets, although this is rarely a good idea. You can't choose the same target more than once for a single strive spell.")
        ruling("2014-04-26", "The mana cost and mana value of strive spells don't change no matter how many targets they have. Strive abilities affect only what you pay.")
        ruling("2014-04-26", "If all of the spell's targets are illegal when the spell tries to resolve, it won't resolve and none of its effects will happen. If one or more of its targets are legal when it tries to resolve, the spell will resolve and affect only those legal targets. It will have no effect on any illegal targets.")
        ruling("2014-04-26", "If such a spell is copied, and the effect that copies the spell allows a player to choose new targets for the copy, the number of targets can't be changed. The player may change any number of the targets, including all of them or none of them. If, for one of the targets, the player can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2014-04-26", "If a spell or ability allows you to cast a strive spell without paying its mana cost, you must pay the additional costs for any targets beyond the first.")
    }
}
