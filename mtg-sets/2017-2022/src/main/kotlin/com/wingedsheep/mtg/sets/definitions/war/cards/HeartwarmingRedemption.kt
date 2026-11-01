package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Heartwarming Redemption
 * {2}{R}{W}
 * Instant
 * Discard all the cards in your hand, then draw that many cards plus one. You gain life equal to
 * the number of cards in your hand.
 *
 * Tolarian Winds' discard-then-draw pipeline with a +1 on the draw, then a life gain that reads the
 * hand size as it stands after the draw (the spell itself is on the stack, so it is never counted).
 * An empty hand discards nothing and draws one.
 */
val HeartwarmingRedemption = card("Heartwarming Redemption") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Instant"
    oracleText = "Discard all the cards in your hand, then draw that many cards plus one. " +
        "You gain life equal to the number of cards in your hand."

    spell {
        effect = Effects.Pipeline {
            val discarded = gather(CardSource.FromZone(Zone.HAND, Player.You))
            move(discarded, CardDestination.ToZone(Zone.GRAVEYARD, Player.You), moveType = MoveType.Discard)
            run(Effects.DrawCards(discarded.count + 1))
            run(Effects.GainLife(DynamicAmounts.cardsInYourHand()))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Howard Lyon"
        flavorText = "Kytheon had known war every day of his life. Now he finally knew peace."
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af569235-f5cb-4e3e-ba77-c1c429ed0a60.jpg?1783933396"
        ruling("2019-05-03", "If you have no cards in hand as Heartwarming Redemption resolves, you discard nothing then draw one card.")
        ruling("2019-05-03", "The amount of life you gain is determined after you draw during Heartwarming Redemption's resolution. No player may take actions between the time cards are drawn and the time you gain life.")
    }
}
