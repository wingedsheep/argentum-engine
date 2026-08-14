package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dori, Bearer of Friends
 * {2}{R}
 * Legendary Creature — Dwarf Warrior
 * 3/2
 * Trample
 * When Dori enters, create a Treasure token.
 */
val DoriBearerOfFriends = card("Dori, Bearer of Friends") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dwarf Warrior"
    oracleText = "Trample\nWhen Dori enters, create a Treasure token. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"
    power = 3
    toughness = 2
    keywords(Keyword.TRAMPLE)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateTreasure()
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Irvin Rodriguez"
        flavorText = "\"I can't be always carrying burglars on my back, down tunnels and up trees! What do you think I am? A porter?\""
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2f60ad0-c887-4585-85f8-afcf72fb80d0.jpg?1785323237"
    }
}
