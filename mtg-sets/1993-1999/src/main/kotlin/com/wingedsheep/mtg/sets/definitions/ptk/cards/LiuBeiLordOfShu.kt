package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Liu Bei, Lord of Shu
 * {3}{W}{W}
 * Legendary Creature — Human Soldier
 * 2/4
 * Horsemanship
 * Liu Bei gets +2/+2 as long as you control a permanent named Guan Yu, Sainted Warrior
 * or a permanent named Zhang Fei, Fierce Warrior.
 */
val LiuBeiLordOfShu = card("Liu Bei, Lord of Shu") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 4
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)\n" +
        "Liu Bei gets +2/+2 as long as you control a permanent named Guan Yu, Sainted Warrior or a permanent named Zhang Fei, Fierce Warrior."

    keywordAbility(KeywordAbility.Simple(Keyword.HORSEMANSHIP))

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(2, 2, Filters.Self),
            condition = Conditions.Any(
                Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Any.named("Guan Yu, Sainted Warrior")),
                Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Any.named("Zhang Fei, Fierce Warrior")),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Qiao Dafu"
        flavorText = "\"Only wisdom and virtue can truly win men's devotion.\"\n—Liu Bei"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b804c879-fe23-4d7f-9e7d-1da41b5c0973.jpg?1783946131"
    }
}
