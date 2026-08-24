package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan

/**
 * Huang Zhong, Shu General
 * {2}{W}{W}
 * Legendary Creature — Human Soldier
 * 2/3
 *
 * The blocker-cap evasion: [CantBeBlockedByMoreThan] with the default `GroupFilter.source()`, so
 * the restriction scopes to Huang Zhong himself rather than to a group.
 */
val HuangZhongShuGeneral = card("Huang Zhong, Shu General") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Huang Zhong can't be blocked by more than one creature."

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Quan Xuejun"
        flavorText = "\"Virile in war, he kept the north in fear; His prodigies subdued the western sphere.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c079037c-f4cf-423f-ad15-ee57136d6148.jpg"
    }
}
