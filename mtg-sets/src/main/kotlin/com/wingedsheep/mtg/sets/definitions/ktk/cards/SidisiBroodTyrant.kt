package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Sidisi, Brood Tyrant
 * {1}{B}{G}{U}
 * Legendary Creature — Snake Shaman
 * 3/3
 *
 * Whenever Sidisi, Brood Tyrant enters the battlefield or attacks, mill three cards.
 * Whenever one or more creature cards are put into your graveyard from your library,
 * create a 2/2 black Zombie creature token.
 */
val SidisiBroodTyrant = card("Sidisi, Brood Tyrant") {
    manaCost = "{1}{B}{G}{U}"
    colorIdentity = "UBG"
    typeLine = "Legendary Creature — Snake Shaman"
    power = 3
    toughness = 3
    oracleText = "Whenever Sidisi, Brood Tyrant enters the battlefield or attacks, mill three cards.\n" +
        "Whenever one or more creature cards are put into your graveyard from your library, " +
        "create a 2/2 black Zombie creature token."

    // Whenever Sidisi enters the battlefield, mill three cards.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.mill(3)
    }

    // Whenever Sidisi attacks, mill three cards.
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = LibraryPatterns.mill(3)
    }

    // Whenever one or more creature cards are put into your graveyard from your library,
    // create a 2/2 black Zombie creature token.
    triggeredAbility {
        trigger = Triggers.CreaturesPutIntoGraveyardFromLibrary
        effect = CreateTokenEffect(
            count = 1,
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            imageUri = "https://cards.scryfall.io/normal/front/b/a/ba7da3d0-2471-48ab-8e7c-af8046d9e0be.jpg?1562640008"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "199"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffa2b070-952e-4242-83bb-3e73135ceeeb.jpg?1562796690"
    }
}
