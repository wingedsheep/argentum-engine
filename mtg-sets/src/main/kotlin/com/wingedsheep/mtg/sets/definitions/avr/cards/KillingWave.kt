package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.PayDynamicLifeEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Killing Wave
 * {X}{B}
 * Sorcery
 * For each creature, its controller sacrifices it unless they pay X life.
 *
 * The 2012-05-01 ruling fixes the shape: the active player decides for all of their creatures
 * first, then each other player in turn order — hence `ForEachPlayer(ActivePlayerFirst)` (APNAP
 * order) wrapping a per-creature `ForEachInGroup` over the creatures *that* player controls. Each
 * iteration rebinds the resolution controller, so both `youControl()` and the pay-or-suffer
 * prompt land on the player whose creatures are being resolved.
 *
 * Paying is a cost, not a loss the player can decline to be able to afford: `Gate.MayPay` checks
 * affordability first, so a player with less life than X is never offered the choice and simply
 * sacrifices (CR 119.4). X = 0 costs nothing to pay, but the choice is still offered — a player
 * may decline and sacrifice anyway, which is what the rules allow.
 *
 * Known deviation: the ruling has every player *decide* first and then pay/sacrifice
 * simultaneously; here each player's payments and sacrifices resolve before the next player is
 * asked. Only observable through death triggers that would otherwise all see the same board.
 */
val KillingWave = card("Killing Wave") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "For each creature, its controller sacrifices it unless they pay X life."

    spell {
        effect = Effects.ForEachPlayer(
            Player.ActivePlayerFirst,
            listOf(
                Effects.ForEachInGroup(
                    filter = GroupFilter.AllCreaturesYouControl,
                    effect = OptionalCostEffect(
                        cost = PayDynamicLifeEffect(DynamicAmount.XValue),
                        ifPaid = Effects.Composite(emptyList()),
                        ifNotPaid = Effects.SacrificeTarget(EffectTarget.Self),
                        // The gate labels its own "yes" button with the computed cost ("Pay 2
                        // life") and its "no" with "Don't pay", so the prompt only has to state
                        // the stakes. *Which* creature each of the N identical prompts covers is
                        // carried by `DecisionContext.subjectEntityId`, which the gate executor
                        // stamps from the enclosing per-entity iteration: the client shows that
                        // creature beside the spell and rings it on the battlefield.
                        descriptionOverride = "Pay life to keep this creature, or sacrifice it.",
                    ),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "Steve Argyle"
        flavorText = "\"I come looking for demons and I find a plane full of angels. " +
            "I hate angels.\"\n—Liliana Vess"
        imageUri =
            "https://cards.scryfall.io/normal/front/3/3/33de2371-175e-4f8a-9636-35f996e3cf24.jpg?1783940697"

        ruling(
            "2012-05-01",
            "First, the active player chooses whether to pay X life for each creature they " +
                "control. Then each other player in turn order chooses for their creatures. Then " +
                "each player pays life and sacrifices creatures at the same time. Players will " +
                "know the decisions of players who chose before them.",
        )
        ruling(
            "2012-05-01",
            "A player may choose to pay life for some creatures and sacrifice the rest. " +
                "It's not an all-or-nothing decision.",
        )
        ruling("2012-05-01", "You can't pay more life than you have.")
        ruling(
            "2012-05-01",
            "If you can't sacrifice a creature (perhaps because of Sigarda, Host of Herons), " +
                "you can choose not to pay life and nothing will happen.",
        )
    }
}
