package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Taoist Mystic
 * {2}{G}
 * Creature — Human Mystic
 * 2/2
 * This creature can't be blocked by creatures with horsemanship.
 */
val TaoistMystic = card("Taoist Mystic") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Mystic"
    power = 2
    toughness = 2
    oracleText = "This creature can't be blocked by creatures with horsemanship."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.withKeyword(Keyword.HORSEMANSHIP))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Qu Xin"
        flavorText = "By appearing in places miles apart at the same time, Zuo Ci exhibited the mystic's ability to \"shrink the land.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/2/023ae64a-7888-4ad2-b879-0649d8e341ac.jpg"
    }
}
