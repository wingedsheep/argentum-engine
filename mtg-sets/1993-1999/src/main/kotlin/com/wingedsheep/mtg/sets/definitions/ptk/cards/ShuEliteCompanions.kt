package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Shu Elite Companions
 * {4}{W}
 * Creature — Human Soldier
 */
val ShuEliteCompanions = card("Shu Elite Companions") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)"

    keywords(Keyword.HORSEMANSHIP)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Qiao Dafu"
        flavorText = "Throughout the three kingdoms, important generals were often guarded by small groups of expert soldiers known as \"elite companions.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11ac63f6-cd61-4334-902a-777410311b2d.jpg"
    }
}
