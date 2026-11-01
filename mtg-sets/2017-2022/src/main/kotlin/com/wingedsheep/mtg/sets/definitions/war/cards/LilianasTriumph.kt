package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Liliana's Triumph — War of the Spark #98 (canonical printing)
 * {1}{B}
 * Instant
 * Each opponent sacrifices a creature of their choice. If you control a Liliana planeswalker,
 * each opponent also discards a card.
 *
 * The Liliana check is read on resolution, after the sacrifice; opponents still discard even if
 * they had no creature to sacrifice.
 */
val LilianasTriumph = card("Liliana's Triumph") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Each opponent sacrifices a creature of their choice. " +
        "If you control a Liliana planeswalker, each opponent also discards a card."

    spell {
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature,
            1,
            EffectTarget.PlayerRef(Player.EachOpponent)
        ) then Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Liliana")),
            then = Effects.Discard(1, EffectTarget.PlayerRef(Player.EachOpponent))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "98"
        artist = "Kieran Yanner"
        flavorText = "Her triumph came not from escaping her death, but in claiming her independence."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84803db8-fdb0-462b-92f6-33d591593d2d.jpg?1783933442"
        ruling(
            "2021-03-19",
            "As Liliana's Triumph resolves, the opponent whose turn it is (or, if it's your turn, the next " +
                "opponent in turn order) chooses a creature to sacrifice, then each other opponent in turn " +
                "order does the same, knowing the choices made before them. Then all the chosen creatures are " +
                "sacrificed at the same time. Then, if you control a Liliana planeswalker, in the same order, " +
                "each opponent chooses a card in their hand without revealing it, and those cards are " +
                "discarded at the same time."
        )
        ruling(
            "2021-03-19",
            "You can cast Liliana's Triumph even if some or all of your opponents will be unable to sacrifice " +
                "a creature. If you control a Liliana planeswalker, they'll still discard a card, even if they " +
                "couldn't sacrifice a creature."
        )
        ruling("2019-05-03", "Whether you control an appropriate planeswalker is checked only as the spell resolves.")
        ruling(
            "2019-05-03",
            "Controlling more than one planeswalker of the appropriate type won't have any more of a benefit " +
                "than just controlling one."
        )
    }
}
