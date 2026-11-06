package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Fateful Showdown
 * {2}{R}{R}
 * Instant
 * Fateful Showdown deals damage to any target equal to the number of cards in your hand. Discard
 * all the cards in your hand, then draw that many cards.
 *
 * Damage reads the hand size as the spell resolves (the spell itself is on the stack, so it never
 * counts), then Tolarian Winds' discard-then-draw pipeline follows: the hand is gathered, discarded,
 * and the draw counts that collection.
 */
val FatefulShowdown = card("Fateful Showdown") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Fateful Showdown deals damage to any target equal to the number of cards in your hand. " +
        "Discard all the cards in your hand, then draw that many cards."

    spell {
        val anyTarget = target(Targets.Any)
        effect = Effects.Pipeline {
            run(Effects.DealDamage(DynamicAmounts.cardsInYourHand(), anyTarget))
            val discarded = gather(CardSource.FromZone(Zone.HAND, Player.You))
            move(discarded, CardDestination.ToZone(Zone.GRAVEYARD, Player.You), moveType = MoveType.Discard)
            run(Effects.DrawCards(discarded.count))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "114"
        artist = "Chris Rallis"
        flavorText = "Tonight at the Grand Exhibition, witness Head Judge Tezzeret face off against the " +
            "infamous renegade Pia Nalaar."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a19d5bdd-7f45-4ff1-bd1a-ac4e87572bcb.jpg?1783937194"
        ruling(
            "2016-09-20",
            "The number of cards in your hand is counted only as Fateful Showdown resolves. Fateful Showdown " +
                "is on the stack at this time, so it's not counted.",
        )
        ruling(
            "2016-09-20",
            "If Fateful Showdown's target becomes an illegal target, you won't discard your hand or draw any cards.",
        )
    }
}
