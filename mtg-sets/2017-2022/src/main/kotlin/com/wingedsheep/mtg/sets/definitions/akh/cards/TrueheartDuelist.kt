package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Trueheart Duelist — Amonkhet #35
 * {1}{W} · Creature — Human Warrior · Uncommon
 * 2/2
 *
 * This creature can block an additional creature each combat.
 * Embalm {2}{W}
 */
val TrueheartDuelist = card("Trueheart Duelist") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "This creature can block an additional creature each combat.\n" +
        "Embalm {2}{W} ({2}{W}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Human Warrior with no mana cost. Embalm only as a sorcery.)"

    staticAbility {
        ability = CanBlockAdditionalForCreatureGroup(count = 1, filter = GroupFilter.source())
    }

    embalm("{2}{W}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/628f5230-6c0d-4f24-b89a-7f3d443511f3.jpg?1783936530"
    }
}
