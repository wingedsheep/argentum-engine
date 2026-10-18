package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Reckoner Shakedown
 * {2}{B}
 * Sorcery
 * Target opponent reveals their hand. You may choose a nonland card from it. If you do, that player
 * discards that card. If you don't, put two +1/+1 counters on a creature or Vehicle you control.
 *
 * The creature or Vehicle isn't targeted: it's chosen on resolution, after the reveal and the
 * decision whether to take a card. Declining (or finding no nonland card) falls through to the
 * counters, which do nothing if you control no creature or Vehicle.
 */
val ReckonerShakedown = card("Reckoner Shakedown") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You may choose a nonland card from it. " +
        "If you do, that player discards that card. If you don't, put two +1/+1 counters on a " +
        "creature or Vehicle you control."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val chosen = chooseUpTo(
                1,
                from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "You may choose a nonland card for that player to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            ifNotEmpty(chosen) {
                discard(chosen, opponent.asPlayer)
            } orElse {
                val mine = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.CreatureOrVehicle))
                val recipient = chooseExactly(
                    1,
                    from = mine,
                    prompt = "Choose a creature or Vehicle you control to put two +1/+1 counters on",
                    useTargetingUI = true
                )
                run(Effects.AddCountersToCollection(recipient, CounterType.PLUS_ONE_PLUS_ONE, 2))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Josu Hernaiz"
        flavorText = "\"That's a start. What else do you have for us?\"\n—Takumi, Reckoner enforcer"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf57666f-366b-43bf-95c7-39c1f7f061e2.jpg?1783923877"
        ruling(
            "2022-02-18",
            "You choose the creature or Vehicle to put counters on after the target opponent reveals " +
                "their hand and you've decided whether or not to choose a card."
        )
    }
}
