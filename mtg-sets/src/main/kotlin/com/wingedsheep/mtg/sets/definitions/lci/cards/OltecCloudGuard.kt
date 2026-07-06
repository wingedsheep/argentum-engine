package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Oltec Cloud Guard — {3}{W}
 * Creature — Human Soldier
 * 3/2
 * Flying
 * When this creature enters, create a 1/1 colorless Gnome artifact creature token.
 */
val OltecCloudGuard = card("Oltec Cloud Guard") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "Flying\nWhen this creature enters, create a 1/1 colorless Gnome artifact creature token."
    power = 3
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Gnome"),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Evyn Fong"
        flavorText = "Many Oltec youth dream of one day joining the elite bat-riding cavalry of the Thousand Moons military force."
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02d68a38-2e0b-401b-b67d-a55e2af5b18d.jpg?1782694589"
    }
}
