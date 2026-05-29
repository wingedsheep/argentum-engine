package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Shivan Zombie
 * {B}{R}
 * Creature — Phyrexian Barbarian Zombie
 * 2/2
 * Protection from white
 */
val ShivanZombie = card("Shivan Zombie") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Phyrexian Barbarian Zombie"
    power = 2
    toughness = 2
    oracleText = "Protection from white"

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.WHITE)))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "271"
        artist = "Tony Szczudlo"
        flavorText = "Barbarians long for a glorious death in battle. Phyrexia was eager to grant that wish."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4c99269-f730-4d33-bbce-9e855e9ad0fc.jpg?1562944260"
    }
}
