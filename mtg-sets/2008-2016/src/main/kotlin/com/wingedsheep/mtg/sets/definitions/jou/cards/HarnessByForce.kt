package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
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
 * Harness by Force — Journey into Nyx #100 (canonical printing)
 * {1}{R}{R} · Sorcery
 *
 * Strive — This spell costs {2}{R} more to cast for each target beyond the first.
 * Gain control of any number of target creatures until end of turn. Untap those creatures.
 * They gain haste until end of turn.
 *
 * Strive is one [CostModification.IncreaseManaPerUnit] of {2}{R} per
 * [CostReductionSource.ChosenTargets] beyond the first. Each still-legal target is threatened
 * (control until end of turn, untap, haste); illegal targets are skipped (CR 608.2b). Untapped
 * creatures and creatures you already control are legal targets (ruling).
 */
val HarnessByForce = card("Harness by Force") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Strive — This spell costs {2}{R} more to cast for each target beyond the first.\n" +
        "Gain control of any number of target creatures until end of turn. Untap those creatures. " +
        "They gain haste until end of turn."

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
            Effects.GainControl(EffectTarget.ContextTarget(0), Duration.EndOfTurn),
            Effects.Untap(EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "100"
        artist = "Yefim Kligerman"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bde89c1-aafa-43d9-adbf-671fa2b52d4f.jpg?1783939426"
        ruling("2014-04-26", "Harness by Force can target any creature, even one that's untapped or one you already control.")
        ruling("2014-04-26", "Gaining control of a creature doesn't cause you to gain control of any Auras or Equipment attached to it.")
        ruling("2014-04-26", "You choose how many targets each spell with a strive ability has and what those targets are as you cast it. It's legal to cast such a spell with no targets, although this is rarely a good idea. You can't choose the same target more than once for a single strive spell.")
        ruling("2014-04-26", "The mana cost and mana value of strive spells don't change no matter how many targets they have. Strive abilities affect only what you pay.")
        ruling("2014-04-26", "If all of the spell's targets are illegal when the spell tries to resolve, it won't resolve and none of its effects will happen. If one or more of its targets are legal when it tries to resolve, the spell will resolve and affect only those legal targets. It will have no effect on any illegal targets.")
        ruling("2014-04-26", "If such a spell is copied, and the effect that copies the spell allows a player to choose new targets for the copy, the number of targets can't be changed. The player may change any number of the targets, including all of them or none of them. If, for one of the targets, the player can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2014-04-26", "If a spell or ability allows you to cast a strive spell without paying its mana cost, you must pay the additional costs for any targets beyond the first.")
    }
}
