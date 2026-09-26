package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAsThoughUntapped
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Masako the Humorless {2}{W}
 * Legendary Creature — Human Advisor 2/1
 * Flash
 * Tapped creatures you control can block as though they were untapped.
 *
 * Ruling: it lets a tapped creature block only if it could otherwise block — "can't block",
 * flying and every other restriction still apply.
 */
val MasakoTheHumorless = card("Masako the Humorless") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Advisor"
    oracleText = "Flash\nTapped creatures you control can block as though they were untapped."
    power = 2
    toughness = 1

    keywords(Keyword.FLASH)

    staticAbility {
        ability = CanBlockAsThoughUntapped(GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Ben Thompson"
        flavorText = "Konda's servants dared not neglect their duties for a moment under Masako's icy gaze, knowing that what she saw, Lord Konda would hear."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6b2507f-4035-47d5-8295-0a3773f187fb.jpg"
    }
}
