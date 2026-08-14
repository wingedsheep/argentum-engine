package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.MultiplyTokenCreation
import com.wingedsheep.sdk.scripting.ReplaceDrawWithEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Bard, King of Dale
 * {4}{W}{U}
 * Legendary Creature — Human Noble Archer
 * 3/5
 *
 * Reach, vigilance
 * If you would draw a card except the first one you draw in each of your draw steps, draw two cards
 * instead.
 * If one or more tokens would be created under your control, twice that many of those tokens are
 * created instead.
 *
 * Both static clauses are replacement effects. The draw replacement is deliberately per-card and
 * exempts only the turn-based first draw in each of Bard's controller's draw steps; extra draws in
 * that step are doubled. Token creation uses the same controller-scoped multiplier as Doubling
 * Season, without its counter clause.
 */
val BardKingOfDale = card("Bard, King of Dale") {
    manaCost = "{4}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Noble Archer"
    oracleText = "Reach, vigilance\n" +
        "If you would draw a card except the first one you draw in each of your draw steps, draw " +
        "two cards instead.\n" +
        "If one or more tokens would be created under your control, twice that many of those " +
        "tokens are created instead."
    power = 3
    toughness = 5

    keywords(Keyword.REACH, Keyword.VIGILANCE)

    replacementEffect(
        ReplaceDrawWithEffect(
            replacementEffect = DrawCardsEffect(2),
            appliesTo = EventPattern.DrawEvent(
                player = Player.You,
                exceptFirstInDrawStep = true,
            ),
        )
    )
    replacementEffect(MultiplyTokenCreation())

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "144"
        artist = "Xabi Gaztelua"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c05c2aa6-29c7-40f8-872e-91099b9225c4.jpg?1784377008"

        ruling(
            "2026-06-29",
            "If two or more replacement effects would apply to a card-drawing event, the player " +
                "drawing the card chooses the order in which to apply them."
        )
        ruling(
            "2026-06-29",
            "Bard's last ability only applies to tokens that are created. Copies of permanent " +
                "spells that resolve become tokens on the battlefield, but those tokens are not " +
                "created and will not be doubled by Bard's ability."
        )
        ruling(
            "2026-06-29",
            "The effects of multiple Bard, King of Dale token abilities are cumulative. Two Bards " +
                "create four times the number of tokens."
        )
    }
}
