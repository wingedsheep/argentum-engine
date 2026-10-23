package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalDungeonRoomTriggers

/**
 * Hama Pashar, Ruin Seeker
 * {1}{W}{U}
 * Legendary Creature — Human Wizard
 * 2/3
 * Room abilities of dungeons you own trigger an additional time.
 */
val HamaPasharRuinSeeker = card("Hama Pashar, Ruin Seeker") {
    manaCost = "{1}{W}{U}"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "Room abilities of dungeons you own trigger an additional time."

    staticAbility {
        ability = AdditionalDungeonRoomTriggers
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "224"
        artist = "Eric Deschamps"
        flavorText = "\"Someone took the time to carve these inscriptions. The least we can do is care enough to read them.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47b8eb2e-73ac-4ad6-9ab6-4f8da7b41020.jpg?1783926447"
        ruling("2021-07-23", "Hama Pashar, Ruin Seeker's ability causes the same room you ventured into to trigger an additional time. It does not cause you to move to the next room.")
    }
}
