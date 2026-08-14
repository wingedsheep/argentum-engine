package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bilbo Baggins, Burglar // Take a Glance — The Hobbit #34
 * {2}{U} · Legendary Creature — Halfling Rogue · Common
 * 2/1
 *
 * When Bilbo Baggins enters, draw a card.
 *
 * Adventure: Take a Glance — {U}, Sorcery — Adventure
 * Scry 2.
 *
 * The Adventure is the cheap look-ahead you cast on turn one; the creature half is the same card
 * cast later from exile, replaying its enters trigger. Both halves are existing facades — the scry
 * is [Patterns.Library.scry], not a hand-rolled reorder.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the caster
 * cast it as the creature spell while it remains in exile.)
 */
val BilboBagginsBurglar = card("Bilbo Baggins, Burglar") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Halfling Rogue"
    power = 2
    toughness = 1
    oracleText = "When Bilbo Baggins enters, draw a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
        description = "When Bilbo Baggins enters, draw a card."
    }

    adventure("Take a Glance") {
        manaCost = "{U}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Scry 2. (Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Patterns.Library.scry(2)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Kieran Yanner"
        flavorText = "\"Now it is the burglar's turn. You must go on and find out if all is " +
            "perfectly safe and canny.\"\n—Thorin"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a109b3e-9f5b-4625-abb7-6b992c10530b.jpg?1785323194"
    }
}
