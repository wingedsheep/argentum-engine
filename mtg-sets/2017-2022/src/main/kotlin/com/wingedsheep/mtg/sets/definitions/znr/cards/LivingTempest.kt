package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Living Tempest — Zendikar Rising #65
 * {4}{U} · Creature — Elemental · 3/3
 *
 * Flash
 * Flying
 */
val LivingTempest = card("Living Tempest") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    oracleText = "Flash\nFlying"
    power = 3
    toughness = 3

    keywords(Keyword.FLASH, Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Campbell White"
        flavorText = "\"If you don't like the weather, just wait a few minutes and it'll change ... probably " +
            "for the worse.\"\n—Amandi, kitesail expert"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a05ed233-9b53-4c67-ab1c-0217386c9fe4.jpg"
    }
}
