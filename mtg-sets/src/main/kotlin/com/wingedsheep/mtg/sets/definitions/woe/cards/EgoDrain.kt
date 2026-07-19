package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Ego Drain
 * {B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from it. That player discards
 * that card. If you don't control a Faerie, exile a card from your hand.
 *
 * The Faerie check happens on resolution, after the discard (CR 608.2) — so a Faerie that dies
 * in response still leaves you paying the exile rider. The rider is not optional: you choose
 * which card, but you must exile one if you have any.
 */
val EgoDrain = card("Ego Drain") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it. " +
        "That player discards that card. If you don't control a Faerie, exile a card from your hand."

    spell {
        val opponent = target("target opponent", TargetOpponent())
        effect = Effects.Pipeline {
            // 1. Target opponent reveals their hand.
            run(RevealHandEffect(opponent))
            // 2. Gather it so you can pick the card to strip.
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)), name = "opponentHand")
            // 3. You choose a nonland card from it.
            val chosen = chooseExactly(
                1, from = hand,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true,
                name = "toDiscard"
            )
            // 4. That player discards it (MoveType.Discard so discard triggers see it).
            move(
                chosen,
                CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                moveType = MoveType.Discard
            )
            // 5. The Faerie rider.
            run(
                ConditionalEffect(
                    condition = Conditions.YouControl(
                        GameObjectFilter.Creature.withSubtype(Subtype.FAERIE),
                        negate = true
                    ),
                    effect = Patterns.Hand.exileFromHand(1, EffectTarget.Controller)
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Valera Lutfullina"
        flavorText = "\"You've wasted your life. Let's see if I can do any better with it.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8faf36da-bbad-4d6e-a530-502d47a2dd23.jpg?1783915109"
    }
}
