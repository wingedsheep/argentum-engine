package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Battle Brawler
 * {1}{B}
 * Creature — Orc Warrior
 * 2/2
 * As long as you control a red or white permanent, this creature gets +1/+0 and has first strike.
 *
 * The colour check reads projected colours, so the bonus switches on and off continuously.
 */
val BattleBrawler = card("Battle Brawler") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Warrior"
    power = 2
    toughness = 2
    oracleText = "As long as you control a red or white permanent, this creature gets +1/+0 and has first strike."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CompositeStaticAbility(
                listOf(
                    ModifyStats(powerBonus = 1, toughnessBonus = 0, filter = GroupFilter.source()),
                    GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.source())
                )
            ),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withAnyColor(Color.RED, Color.WHITE)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Karl Kopinski"
        flavorText = "Every time he returns from battle unscathed, he feels a tinge of disappointment."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c9b5baf-6163-48a9-b58d-195edfeb437d.jpg?1783938700"
        ruling(
            "2014-11-24",
            "If Battle Brawler assigns combat damage during the first combat damage step, and then it loses " +
                "first strike before the second combat damage step (perhaps because you no longer control a red " +
                "or white permanent), it won't assign combat damage again in the second combat damage step."
        )
    }
}
