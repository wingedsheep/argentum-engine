package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zhang Fei, Fierce Warrior
 * {4}{W}{W}
 * Legendary Creature — Human Soldier Warrior
 * 4/4
 */
val ZhangFeiFierceWarrior = card("Zhang Fei, Fierce Warrior") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier Warrior"
    power = 4
    toughness = 4
    oracleText = "Vigilance; horsemanship (This creature can't be blocked except by creatures with horsemanship.)"

    keywords(Keyword.VIGILANCE, Keyword.HORSEMANSHIP)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Qiao Dafu"
        flavorText = "Zhang Fei's uncharacteristic alliance with a defeated Riverlands general, Yan Yan, allowed Shu forces to advance through forty-five Riverlands strongpoints with no casualties."
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a58ec38-f1dc-4c69-9d26-a4599bce586a.jpg"
    }
}
