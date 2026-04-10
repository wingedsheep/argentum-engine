package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedIfCastSpellType
import com.wingedsheep.sdk.scripting.GameObjectFilter

val RelicRunner = card("Relic Runner") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Human Rogue"
    oracleText = "Relic Runner can't be blocked if you've cast a historic spell this turn. (Artifacts, legendaries, and Sagas are historic.)"
    power = 2
    toughness = 1

    staticAbility {
        ability = CantBeBlockedIfCastSpellType(spellFilter = GameObjectFilter.Historic)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Josu Hernaiz"
        flavorText = "Her knack for tampering with wards got her kicked out of the academy, but they also got her back in."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76e5993e-f619-4de5-aa94-7569b0efe415.jpg?1562737966"
        ruling("2020-08-07", "Once Relic Runner has been blocked, casting a historic spell won't remove the blocking creature from combat or cause Relic Runner to become unblocked.")
    }
}
