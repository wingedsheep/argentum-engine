package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Gonti, Lord of Luxury
 * {2}{B}{B}
 * Legendary Creature — Aetherborn Rogue
 * 2/3
 *
 * Deathtouch
 * When Gonti enters, look at the top four cards of target opponent's library, exile one of them
 * face down, then put the rest on the bottom of that library in a random order. You may cast that
 * card for as long as it remains exiled, and mana of any type can be spent to cast that spell.
 *
 * The Black Cat, Cunning Thief pipeline (top four, exile exactly one face down with
 * [FaceDownMode.HIDDEN], the rest to the bottom in a random order) with Thief of Sanity's grant:
 * a `Permanent` may-play over the exiled card with `withAnyManaType`, and `nonLandOnly` for the
 * "cast" wording (a land exiled this way can't be played). The may-play grant is also what lets
 * its holder look at the face-down card (Visibility's face-down exile access), and it goes to the
 * ability's controller at resolution, so it survives Gonti leaving play or changing control.
 */
val GontiLordOfLuxury = card("Gonti, Lord of Luxury") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Aetherborn Rogue"
    power = 2
    toughness = 3
    oracleText = "Deathtouch\n" +
        "When Gonti enters, look at the top four cards of target opponent's library, exile one of " +
        "them face down, then put the rest on the bottom of that library in a random order. You " +
        "may cast that card for as long as it remains exiled, and mana of any type can be spent " +
        "to cast that spell."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val topFour = gather(
                CardSource.TopOfLibrary(
                    count = 4,
                    player = opponent.asPlayer,
                )
            )
            val (exiled, rest) = chooseExactlySplit(
                1,
                from = topFour,
                chooser = Chooser.Controller,
                prompt = "Exile one of those cards face down.",
                showAllCards = true
            )
            exile(exiled, opponent.asPlayer, faceDown = FaceDownMode.HIDDEN)
            move(
                rest,
                CardDestination.ToZone(
                    Zone.LIBRARY,
                    opponent.asPlayer,
                    ZonePlacement.Bottom,
                ),
                order = CardOrder.Random
            )
            run(Effects.GrantMayPlayFromExile(
                from = exiled,
                expiry = MayPlayExpiry.Permanent,
                withAnyManaType = true,
                nonLandOnly = true,
            ))
        }
        description = "When Gonti enters, look at the top four cards of target opponent's " +
            "library, exile one of them face down, then put the rest on the bottom of that " +
            "library in a random order. You may cast that card for as long as it remains " +
            "exiled, and mana of any type can be spent to cast that spell."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/7735ecda-9bb0-4ef9-86b2-16e5b6592e61.jpg?1783937205"

        ruling("2016-09-20", "An effect that instructs you to \"cast\" a card doesn't allow you " +
            "to play lands.")
        ruling("2016-09-20", "You may look at and cast that card (and spend mana as though it " +
            "were mana of any type to do so) even if Gonti leaves the battlefield. If another " +
            "player gains control of Gonti, that player can't look at or cast the card, and you " +
            "still can.")
        ruling("2016-09-20", "Casting the card causes it to leave exile. You can't cast it " +
            "multiple times.")
        ruling("2016-09-20", "Gonti doesn't change when you can cast the exiled card. For " +
            "example, if you exile a creature card without flash, you can cast it only during " +
            "your main phase when the stack is empty.")
    }
}
