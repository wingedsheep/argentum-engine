package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shroudstomper
 * {3}{W}{W}{B}{B}
 * Creature — Elemental
 * 5/5
 *
 * Deathtouch
 * Whenever this creature enters or attacks, each opponent loses 2 life. You gain 2 life
 * and draw a card.
 *
 * "Enters or attacks" is two distinct triggered abilities sharing one payoff (CR has no "or"
 * trigger combiner), modeled like Sentinel of the Nameless City: one fires on
 * [Triggers.EntersBattlefield], the other on [Triggers.Attacks]. The shared payoff composes a
 * 2-life [Effects.LoseLife] against [Player.EachOpponent], a 2-life [Effects.GainLife] for the
 * controller, and a single [Effects.DrawCards].
 */
val Shroudstomper = card("Shroudstomper") {
    manaCost = "{3}{W}{W}{B}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Elemental"
    power = 5
    toughness = 5
    oracleText = "Deathtouch\nWhenever this creature enters or attacks, each opponent loses 2 " +
        "life. You gain 2 life and draw a card."

    keywords(Keyword.DEATHTOUCH)

    val payoff = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
        .then(Effects.GainLife(2))
        .then(Effects.DrawCards(1))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = payoff
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = payoff
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Campbell White"
        flavorText = "\"At least the razorkin are happy when they kill you. The walkers don't even " +
            "notice.\"\n—Angus, survivor scout"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53f746bf-0642-48db-835e-27a7fe369fef.jpg?1726286739"
    }
}
