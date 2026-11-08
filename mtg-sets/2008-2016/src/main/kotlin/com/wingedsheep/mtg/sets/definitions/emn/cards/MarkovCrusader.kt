package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Markov Crusader
 * {4}{B}
 * Creature — Vampire Knight
 * 4/3
 *
 * Lifelink
 * This creature has haste as long as you control another Vampire.
 */
val MarkovCrusader = card("Markov Crusader") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Knight"
    power = 4
    toughness = 3
    oracleText = "Lifelink\nThis creature has haste as long as you control another Vampire."

    keywords(Keyword.LIFELINK)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HASTE.name, GroupFilter.source()),
            condition = Conditions.YouControl(
                GameObjectFilter.Permanent.withSubtype("Vampire"),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Johannes Voss"
        flavorText = "The surviving members of the Markov bloodline drove through a crush of horrors to reclaim their ancestral home."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87d8354b-df56-4294-9f8a-b7c92e31b88e.jpg?1783937481"
        ruling(
            "2016-07-13",
            "If Markov Crusader loses haste before the declare attackers step on the turn it enters the battlefield, it won't be able to attack. Once you've attacked with Markov Crusader, causing it to lose haste won't remove it from combat."
        )
    }
}
