package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Realm-Cloaked Giant // Cast Off
 * {5}{W}{W}
 * Creature — Giant (7/7)
 * Vigilance
 *
 * Adventure: Cast Off — {3}{W}{W}, Sorcery — Adventure
 * Destroy all non-Giant creatures.
 */
val RealmCloakedGiant = card("Realm-Cloaked Giant") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Giant"
    oracleText = "Vigilance"
    power = 7
    toughness = 7
    keywords(Keyword.VIGILANCE)

    adventure("Cast Off") {
        manaCost = "{3}{W}{W}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Destroy all non-Giant creatures. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.DestroyAll(GameObjectFilter.Creature.notSubtype(Subtype.GIANT))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "26"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e5c8cf1-1d7c-49e5-bfad-7e13c418118f.jpg?1783932668"
    }
}
