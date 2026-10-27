package com.wingedsheep.mtg.sets.definitions.c20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Call the Coppercoats
 * {2}{W}
 * Instant
 *
 * Strive — This spell costs {1}{W} more to cast for each target beyond the first.
 * Choose any number of target opponents. Create X 1/1 white Human Soldier creature tokens, where X
 * is the number of creatures those opponents control.
 *
 * Strive is Officious Interrogation's per-target tax ([CostReductionSource.ChosenTargets] with
 * `beyond = 1`) split across its two halves: the generic {1} through
 * [CostModification.IncreaseGenericBy] (Fireball's tax) and the {W} through
 * [CostModification.IncreaseColoredPerUnit], which only adds colored symbols. Both are cost
 * increases, so a free cast still owes them and the mana value stays 3.
 *
 * "Those opponents" is [Player.EachTargetedPlayer]: the count sums every targeted opponent still
 * legal on resolution, so an opponent that became an illegal target isn't counted (ruling).
 */
val CallTheCoppercoats = card("Call the Coppercoats") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Strive — This spell costs {1}{W} more to cast for each target beyond the first.\n" +
        "Choose any number of target opponents. Create X 1/1 white Human Soldier creature tokens, " +
        "where X is the number of creatures those opponents control."

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
        target(TargetOpponent(unlimited = true))
        effect = Effects.CreateToken(
            count = DynamicAmounts.count(
                Player.EachTargetedPlayer,
                Zone.BATTLEFIELD,
                GameObjectFilter.Creature,
            ),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/9/0/90de6d1e-e654-4f4a-8014-061ce69e540f.jpg?1783930948",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Cristi Balanescu"
        flavorText = "When the alarm crystals flash, Drannith's finest answer the call."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e11cb353-d864-4b22-bdaf-1d42c25f0918.jpg?1783931224"
        ruling(
            "2020-04-17",
            "Any target opponents that are no longer legal targets by the time Call the Coppercoats resolves won't have their creatures counted when determining how many tokens you create.",
        )
        ruling(
            "2020-04-17",
            "You choose how many targets each spell with a strive ability has and what those targets are as you cast it. It's legal to cast such a spell with no targets, although this is rarely a good idea. You can't choose the same target more than once for a single strive spell.",
        )
        ruling("2020-04-17", "The mana value of a strive spell doesn't change no matter how many targets it has.")
        ruling(
            "2020-04-17",
            "If a spell or ability allows you to cast a strive spell without paying its mana cost, you must pay the additional cost for any targets beyond the first.",
        )
    }
}
