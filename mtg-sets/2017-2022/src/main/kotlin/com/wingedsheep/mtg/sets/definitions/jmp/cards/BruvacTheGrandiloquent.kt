package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyKeywordActionAmount
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Bruvac the Grandiloquent {2}{U}
 * Legendary Creature — Human Advisor
 * 1/4
 *
 * If an opponent would mill one or more cards, they mill twice that many cards instead.
 *
 * The mill-amount replacement is [ModifyKeywordActionAmount] with a `multiplier`, applied at every
 * mill announcement (mill effects, mill costs, pay-or-suffer mills, damage-to-mill replacements).
 * Only an instruction that says "mill" is affected (ruling). Two Bruvacs quadruple it; next to an
 * additive mill replacement (The Water Crystal) the milling player chooses the order (CR 616.1).
 */
val BruvacTheGrandiloquent = card("Bruvac the Grandiloquent") {
    manaCost = "{2}{U}"
    typeLine = "Legendary Creature — Human Advisor"
    oracleText = "If an opponent would mill one or more cards, they mill twice that many cards instead. " +
        "(To mill a card, a player puts the top card of their library into their graveyard.)"
    power = 1
    toughness = 4

    replacementEffect(
        ModifyKeywordActionAmount(
            appliesTo = EventPattern.MillEvent(player = Player.EachOpponent),
            multiplier = 2,
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "10"
        artist = "Ekaterina Burmak"
        flavorText = "\". . . and furthermore . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b43bdc7-e49e-4848-9101-6cad2ecab4dc.jpg?1783930509"

        ruling("2020-06-23", "If a player is instructed to put a card into their graveyard without using the word “mill,” Bruvac's ability doesn't apply.")
        ruling("2020-06-23", "Because Bruvac is legendary, it's unlikely that one player will control two. However, if that happens, each opponent mills four times as many cards as originally instructed. If they control three, their opponents mill eight times as many, and so on.")
    }
}
