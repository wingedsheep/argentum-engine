package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Flood of Tears — Core Set 2020 #59
 * {4}{U}{U} · Sorcery · Rare
 *
 * Return all nonland permanents to their owners' hands. If you return four or more nontoken
 * permanents you control this way, you may put a permanent card from your hand onto the
 * battlefield.
 *
 * Gather every nonland permanent, snapshot the nontoken ones you control *while they are still on
 * the battlefield*, then bounce the whole group in one move. The count is taken from the snapshot,
 * not from what actually reached a hand: per the 2019-07-12 ruling, a permanent that would be
 * returned but ends up elsewhere (exiled instead, a commander sent to the command zone) still
 * counts. The optional put is the shared `Gather → Select(up to 1) → Move` hand pattern, so the
 * permanent card may be one that was just returned.
 */
val FloodOfTears = card("Flood of Tears") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return all nonland permanents to their owners' hands. If you return four or more " +
        "nontoken permanents you control this way, you may put a permanent card from your hand onto " +
        "the battlefield."

    spell {
        effect = Effects.Pipeline(
            descriptionOverride = "Return all nonland permanents to their owners' hands. If you return " +
                "four or more nontoken permanents you control this way, you may put a permanent card " +
                "from your hand onto the battlefield."
        ) {
            val returning = gather(
                CardSource.FromZone(Zone.BATTLEFIELD, Player.Each, GameObjectFilter.NonlandPermanent)
            )
            // "nontoken permanents you control" — read while they are still on the battlefield.
            val yoursReturning = filter(returning, GameObjectFilter.Any.youControl().nontoken())

            toHand(returning)

            ifNotEmpty(yoursReturning, minSize = 4) {
                run(
                    Patterns.Hand.putFromHand(
                        filter = GameObjectFilter.Permanent,
                        prompt = "You may put a permanent card from your hand onto the battlefield"
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "Adam Paquette"
        flavorText = "\"I have wept such a torrent as to scour the land clean.\"\n—Mu Yanling"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3621820-4ab5-42bf-8a02-d5c066db4653.jpg?1783933010"

        ruling(
            "2019-07-12",
            "If you return four or more nontoken permanents, the permanent card you put onto the " +
                "battlefield may be one of the cards returned to your hand this way."
        )
        ruling(
            "2019-07-12",
            "Any abilities that trigger as the nonland permanents are returned are put onto the stack " +
                "at the same time as any abilities that trigger as the permanent card is put onto the " +
                "battlefield (if applicable)."
        )
        ruling(
            "2019-07-12",
            "If a permanent would be returned to your hand but ends up in another zone (most likely " +
                "because an effect exiled it instead or because it's a player's commander in the " +
                "Commander variant) Flood of Tears counts it when determining whether four or more of " +
                "your nontoken permanents were returned."
        )
    }
}
