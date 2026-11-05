package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.eternalize
import com.wingedsheep.sdk.model.Rarity

/**
 * Proven Combatant — Hour of Devastation #42 (canonical printing)
 * {U} · Creature — Human Warrior · 1/1
 *
 * Eternalize {4}{U}{U}
 */
val ProvenCombatant = card("Proven Combatant") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Warrior"
    power = 1
    toughness = 1
    oracleText = "Eternalize {4}{U}{U} ({4}{U}{U}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a 4/4 black Zombie Human Warrior with no mana cost. Eternalize only as a sorcery.)"

    eternalize("{4}{U}{U}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Clint Cearley"
        flavorText = "\"It's not wise to keep glory waiting.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d83fd08a-c87b-4e29-bd1d-31dea3732156.jpg?1783936051"
        ruling("2017-07-14", "The token copies exactly what was printed on the original card and nothing else, except the characteristics specifically modified by eternalize. It doesn't copy any information about the object the card was before it was put into your graveyard.")
        ruling("2017-07-14", "The token is a Zombie in addition to its other types and is black instead of its other colors. Its base power and toughness are 4/4. It has no mana cost, and thus its mana value is 0. These are copiable values of the token that other effects may copy.")
    }
}
