package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.OpponentsPlayWithHandsRevealed
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Seer's Vision
 * {2}{U}{B}
 * Enchantment
 *
 * Your opponents play with their hands revealed.
 * Sacrifice this enchantment: Look at target player's hand and choose a card from it.
 * That player discards that card. Activate only as a sorcery.
 *
 * The "play with hands revealed" clause is the visibility-only static
 * [OpponentsPlayWithHandsRevealed] (the opponent-facing sibling of RevealTopOfLibrary,
 * handled entirely by the client state transformer's hand-masking seam).
 *
 * The sacrifice ability composes the standard look-at-hand pipeline: gather the target
 * player's hand → controller picks one card (shown all cards) → discard it. "Look at"
 * (not "reveal") means the hand is only shown to the controller during the choice, which
 * the gather/select pipeline already does without a public RevealHandEffect.
 */
val SeersVision = card("Seer's Vision") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Enchantment"
    oracleText = "Your opponents play with their hands revealed.\n" +
        "Sacrifice this enchantment: Look at target player's hand and choose a card from it. " +
        "That player discards that card. Activate only as a sorcery."

    staticAbility {
        ability = OpponentsPlayWithHandsRevealed
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        timing = TimingRule.SorcerySpeed
        val targetPlayer = target("target player", TargetPlayer())
        effect = Effects.Composite(
            listOf(
                // Look at target player's hand.
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "targetHand"
                ),
                // You choose a card from it.
                SelectFromCollectionEffect(
                    from = "targetHand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    storeSelected = "toDiscard",
                    prompt = "Choose a card for that player to discard",
                    alwaysPrompt = true,
                    showAllCards = true
                ),
                // That player discards that card.
                MoveCollectionEffect(
                    from = "toDiscard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    moveType = MoveType.Discard
                )
            )
        )
        description = "Sacrifice this enchantment: Look at target player's hand and choose a card from it. " +
            "That player discards that card. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "270"
        artist = "Rebecca Guay"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c94618a-808c-4b3c-8f34-45e64d0414d3.jpg?1562897588"
    }
}
