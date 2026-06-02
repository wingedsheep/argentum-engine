package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Grey Havens Navigator
 * {2}{U}
 * Creature — Elf Pilot
 * 3/2
 *
 * Flash
 * When this creature enters, scry 1.
 */
val GreyHavensNavigator = card("Grey Havens Navigator") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elf Pilot"
    power = 3
    toughness = 2
    oracleText = "Flash\nWhen this creature enters, scry 1."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Henry Peters"
        flavorText = "It was an old tradition that beyond the Shire stood the Grey Havens, from which at times elven-ships set sail, never to return."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4dd89994-bdff-47ca-a65d-10afcc7e773e.jpg?1686968120"
    }
}
