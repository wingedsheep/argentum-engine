package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Lady Zhurong, Warrior Queen
 * {4}{G}
 * Legendary Creature — Human Soldier Warrior
 * 4/3
 * Horsemanship
 */
val LadyZhurongWarriorQueen = card("Lady Zhurong, Warrior Queen") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Soldier Warrior"
    power = 4
    toughness = 3
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)"

    keywordAbility(KeywordAbility.Simple(Keyword.HORSEMANSHIP))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "139"
        artist = "Miao Aili"
        flavorText = "\"A man, and such a fool! I, a woman, will fight them for you.\"\n—Lady Zhurong to her husband Meng Huo, before leading an army against the Shu"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/009661e7-c704-43a1-82e3-7da0b609844e.jpg"
    }
}
