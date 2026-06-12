package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Teferi, Temporal Pilgrim
 * {3}{U}{U}
 * Legendary Planeswalker — Teferi
 * Starting Loyalty: 4
 *
 * Whenever you draw a card, put a loyalty counter on Teferi, Temporal Pilgrim.
 * 0: Draw a card.
 * −2: Create a 2/2 blue Spirit creature token with vigilance and "Whenever you
 *     draw a card, put a +1/+1 counter on this token."
 * −12: Target opponent chooses a permanent they control and returns it to its
 *      owner's hand. Then they shuffle each nonland permanent they control into
 *      its owner's library.
 */
val TeferiTemporalPilgrim = card("Teferi, Temporal Pilgrim") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Teferi"
    startingLoyalty = 4
    oracleText = "Whenever you draw a card, put a loyalty counter on Teferi, Temporal Pilgrim.\n" +
        "0: Draw a card.\n" +
        "−2: Create a 2/2 blue Spirit creature token with vigilance and \"Whenever you draw a card, put a +1/+1 counter on this token.\"\n" +
        "−12: Target opponent chooses a permanent they control and returns it to its owner's hand. Then they shuffle each nonland permanent they control into its owner's library."

    // Whenever you draw a card, put a loyalty counter on Teferi.
    triggeredAbility {
        trigger = Triggers.YouDraw
        effect = Effects.AddCounters(Counters.LOYALTY, 1, EffectTarget.Self)
    }

    // 0: Draw a card.
    loyaltyAbility(0) {
        effect = Effects.DrawCards(1)
    }

    // −2: Create a 2/2 blue Spirit creature token with vigilance and
    //     "Whenever you draw a card, put a +1/+1 counter on this token."
    loyaltyAbility(-2) {
        effect = CreateTokenEffect(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.VIGILANCE),
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.YouDraw.event,
                    binding = Triggers.YouDraw.binding,
                    effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/3/4/349e3241-8f9d-4c52-9848-e01b575fd372.jpg?1675455446"
        )
    }

    // −12: Target opponent chooses a permanent they control and returns it to its owner's
    //      hand. Then they shuffle each nonland permanent they control into its owner's library.
    loyaltyAbility(-12) {
        target("opponent", Targets.Opponent)
        effect = Effects.Pipeline {
            // Target opponent picks a permanent they control; return it to its owner's hand.
            val theirPermanents = gather(
                CardSource.ControlledPermanents(Player.ContextPlayer(0)),
                name = "their_permanents"
            )
            val chosen = chooseExactly(
                1, from = theirPermanents,
                chooser = Chooser.TargetPlayer,
                prompt = "Choose a permanent you control to return to its owner's hand",
                useTargetingUI = true,
                name = "chosen"
            )
            move(
                chosen,
                CardDestination.ToZone(Zone.HAND, Player.ContextPlayer(0))
            )
            // Then shuffle each remaining nonland permanent they control into its owner's library.
            val theirNonlands = gather(
                CardSource.ControlledPermanents(
                    Player.ContextPlayer(0),
                    GameObjectFilter.NonlandPermanent
                ),
                name = "their_nonlands"
            )
            move(
                theirNonlands,
                CardDestination.ToZone(
                    Zone.LIBRARY,
                    Player.ContextPlayer(0),
                    ZonePlacement.Shuffled
                )
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "66"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23a4f1ec-eadf-4f1e-8821-f22293ad2580.jpg?1674420627"
    }
}
