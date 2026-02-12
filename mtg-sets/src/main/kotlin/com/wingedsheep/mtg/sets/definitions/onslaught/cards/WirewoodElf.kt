package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AddManaEffect

/**
 * Wirewood Elf
 * {1}{G}
 * Creature — Elf Druid
 * 1/2
 * {T}: Add {G}.
 */
val WirewoodElf = card("Wirewood Elf") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 2
    oracleText = "{T}: Add {G}."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "301"
        artist = "Jerry Tiritilli"
        flavorText = "\"The land belongs to nature as far as our eyes can see. The higher we climb, the more we can see.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10a34e31-97f1-40e8-9d91-a8139af7f096.jpg"
    }
}
