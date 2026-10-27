package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Secrets of the Dead
 * {2}{U}
 * Enchantment
 * Whenever you cast a spell from your graveyard, draw a card.
 */
val SecretsOfTheDead = card("Secrets of the Dead") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a spell from your graveyard, draw a card."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.CastFromZone(Zone.GRAVEYARD)))
        effect = Effects.DrawCards(1)
        description = "Whenever you cast a spell from your graveyard, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Eytan Zana"
        flavorText = "\"The veil between the living and the dead is so very thin that the whispers of the " +
            "ancients can finally be heard.\"\n—Amalric of Midvast Hall"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c982d679-581d-43e1-acd0-db77eb0987c2.jpg?1783940836"
        ruling(
            "2011-01-22",
            "Secrets of the Dead's triggered ability resolves before the spell you cast from your graveyard does."
        )
    }
}
