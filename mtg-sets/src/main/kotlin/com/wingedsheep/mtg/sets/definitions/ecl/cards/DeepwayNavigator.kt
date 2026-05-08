package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Deepway Navigator
 * {W}{U}
 * Creature — Merfolk Wizard
 * 2/2
 *
 * Flash
 * When this creature enters, untap each other Merfolk you control.
 * As long as you attacked with three or more Merfolk this turn, Merfolk you control get +1/+0.
 */
val DeepwayNavigator = card("Deepway Navigator") {
    manaCost = "{W}{U}"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 2
    oracleText = "Flash\n" +
        "When this creature enters, untap each other Merfolk you control.\n" +
        "As long as you attacked with three or more Merfolk this turn, Merfolk you control get +1/+0."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.untapGroup(
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl().withSubtype("Merfolk"),
                excludeSelf = true
            )
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 1,
                toughnessBonus = 0,
                filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Merfolk").youControl())
            ),
            condition = Conditions.YouAttackedWithCreaturesThisTurn(
                filter = GameObjectFilter.Creature.withSubtype("Merfolk").youControl(),
                atLeast = 3
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Jacob Walker"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d988e28b-fa60-4b60-8229-7a15932c784b.jpg?1767659290"
    }
}
