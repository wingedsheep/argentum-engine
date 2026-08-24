package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wei Strike Force
 * {2}{B}
 * Creature — Human Soldier
 * 2/1
 * Horsemanship
 */
val WeiStrikeForce = card("Wei Strike Force") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)"

    keywords(Keyword.HORSEMANSHIP)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Tang Xiaogu"
        flavorText = "At the battle of Chang'an, Ma Chao defeated the two generals Cao Cao sent to guard the pass but was forced to flee when Cao Cao's trickery turned his own ally against him."
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6eae8ce1-b21f-4d64-91ff-e98bf6a4548a.jpg"
    }
}
