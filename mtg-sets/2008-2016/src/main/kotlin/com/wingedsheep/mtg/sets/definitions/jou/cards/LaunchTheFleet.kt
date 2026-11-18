package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Launch the Fleet — Journey into Nyx #15
 * {W} · Sorcery
 *
 * Strive — This spell costs {1} more to cast for each target beyond the first.
 * Until end of turn, any number of target creatures each gain "Whenever this creature attacks,
 * create a 1/1 white Soldier creature token that's tapped and attacking."
 *
 * Strive is Call the Coppercoats' generic half alone: [CostModification.IncreaseGenericBy] over
 * [CostReductionSource.ChosenTargets] with `beyond = 1`. It's a cost increase, so a free cast still
 * owes it and the mana value stays 1. [Effects.ForEachTarget] grants the attack trigger to each
 * chosen creature on its own (Open into Wonder's shape), so a target that became illegal simply
 * misses out. Each token's defender is chosen as it enters (CR 508.4, ruling), hence
 * [EnteringAttackDefender.CONTROLLER_CHOOSES]. Journey into Nyx printed no Soldier token, so the
 * art is Theros's 1/1 white Soldier (the block's own).
 */
val LaunchTheFleet = card("Launch the Fleet") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Strive — This spell costs {1} more to cast for each target beyond the first.\n" +
        "Until end of turn, any number of target creatures each gain \"Whenever this creature attacks, " +
        "create a 1/1 white Soldier creature token that's tapped and attacking.\""

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseGenericBy(CostReductionSource.ChosenTargets(beyond = 1)),
        )
    }

    spell {
        target = TargetObject(filter = TargetFilter.Creature, unlimited = true)
        effect = Effects.ForEachTarget(
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.attacks(),
                    effect = Effects.CreateToken(
                        power = 1,
                        toughness = 1,
                        colors = setOf(Color.WHITE),
                        creatureTypes = setOf("Soldier"),
                        tapped = true,
                        attacking = true,
                        attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
                        imageUri = "https://cards.scryfall.io/normal/front/3/1/31f320f4-baae-46cb-840e-421f010a0a20.jpg?1783939699",
                    ),
                    descriptionOverride = "Whenever this creature attacks, create a 1/1 white Soldier " +
                        "creature token that's tapped and attacking.",
                ),
                target = EffectTarget.ContextTarget(0),
                duration = Duration.EndOfTurn,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc3e52c0-2f79-4537-8f1c-70b68836bac0.jpg?1783939456"
        ruling(
            "2014-04-26",
            "You declare which player or planeswalker each token is attacking as you put it onto the battlefield. It doesn't have to be the same player or planeswalker the original creature is attacking.",
        )
        ruling(
            "2014-04-26",
            "Although the tokens are attacking, they were never declared as attacking creatures (for purposes of abilities that trigger whenever a creature attacks, for example).",
        )
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
