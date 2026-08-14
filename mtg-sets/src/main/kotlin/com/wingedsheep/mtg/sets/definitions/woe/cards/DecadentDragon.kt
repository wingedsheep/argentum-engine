package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Decadent Dragon // Expensive Taste
 * {2}{R}{R}
 * Creature — Dragon
 * 4/4
 * Flying, trample
 * Whenever this creature attacks, create a Treasure token.
 *
 * Adventure: Expensive Taste — {2}{B}, Instant — Adventure
 * Exile the top two cards of target opponent's library face down. You may look at and play those
 * cards for as long as they remain exiled.
 *
 * Expensive Taste is a gather → exile → grant pipeline over the *targeted opponent's* library
 * ([CardSource.TopOfLibrary] with [Player.ContextPlayer]`(0)`). Three separate clauses ride on it,
 * and each is carried by one piece:
 *
 *  - **face down** — [FaceDownMode.HIDDEN] on the move, so the cards sit in exile with no turn-up
 *    cost and are opaque to everyone by default.
 *  - **you may look at them** — the gather is a library source with the default
 *    `LookAudience.Controller`, which persists a private reveal to the caster. That reveal survives
 *    the move to exile, so the caster (and only the caster — their owner never learns what they are
 *    until they're played, per the ruling) sees through the face-down back.
 *  - **and play them for as long as they remain exiled** — [Effects.GrantMayPlayFromExile] with
 *    [MayPlayExpiry.Permanent]. "Play", not "cast", so land cards among them are playable too, and
 *    normal timing rules and mana costs still apply — all of which the permission machinery already
 *    enforces. The grant is not tied to the Dragon: it outlives the Adventure card leaving exile.
 */
val DecadentDragon = card("Decadent Dragon") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Dragon"
    oracleText = "Flying, trample\n" +
        "Whenever this creature attacks, create a Treasure token."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.CreateTreasure()
        description = "Whenever this creature attacks, create a Treasure token."
    }

    adventure("Expensive Taste") {
        manaCost = "{2}{B}"
        typeLine = "Instant — Adventure"
        oracleText = "Exile the top two cards of target opponent's library face down. You may look " +
            "at and play those cards for as long as they remain exiled. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            target("target opponent", TargetOpponent())
            effect = Effects.Composite(
                listOf(
                    GatherCardsEffect(
                        source = CardSource.TopOfLibrary(
                            DynamicAmount.Fixed(2),
                            Player.ContextPlayer(0),
                        ),
                        storeAs = "stolen",
                    ),
                    MoveCollectionEffect(
                        from = "stolen",
                        destination = CardDestination.ToZone(Zone.EXILE),
                        faceDown = FaceDownMode.HIDDEN,
                    ),
                    Effects.GrantMayPlayFromExile(from = "stolen", expiry = MayPlayExpiry.Permanent),
                ),
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Wylie Beckert"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/315cbbf7-a2ad-4565-9877-1e903d7fd797.jpg?1783915066"

        ruling(
            "2023-09-01",
            "Only you get to look at the cards exiled with Expensive Taste. Their owner doesn't " +
                "get to know what they are while they're in exile until you play them."
        )
        ruling(
            "2023-09-01",
            "You may play the cards exiled with Expensive Taste even if they are land cards."
        )
        ruling(
            "2023-09-01",
            "You must still follow all normal timing rules to play cards exiled with Expensive Taste."
        )
    }
}
