package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Jetmir, Nexus of Revels — Streets of New Capenna #193
 * {1}{R}{G}{W} · Legendary Creature — Cat Demon · 5 / 4
 *
 * Each threshold (3 / 6 / 9 creatures) is two [ConditionalStaticAbility]s — the +1/+0 in layer 7c
 * and the keyword grant in layer 6 — gated by the same creature count, so the bonuses stack
 * cumulatively ("also") and appear and vanish with the creature count at projection time.
 */
val JetmirNexusOfRevels = card("Jetmir, Nexus of Revels") {
    manaCost = "{1}{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Cat Demon"
    power = 5
    toughness = 4
    oracleText =
        "Creatures you control get +1/+0 and have vigilance as long as you control three or more creatures.\n" +
        "Creatures you control also get +1/+0 and have trample as long as you control six or more creatures.\n" +
        "Creatures you control also get +1/+0 and have double strike as long as you control nine or more creatures."

    for ((count, keyword) in listOf(3 to Keyword.VIGILANCE, 6 to Keyword.TRAMPLE, 9 to Keyword.DOUBLE_STRIKE)) {
        staticAbility {
            ability = ConditionalStaticAbility(
                ability = ModifyStats(powerBonus = 1, toughnessBonus = 0, filter = GroupFilter.AllCreaturesYouControl),
                condition = Conditions.YouControlAtLeast(count, GameObjectFilter.Creature),
            )
        }
        staticAbility {
            ability = ConditionalStaticAbility(
                ability = GrantKeyword(keyword, GroupFilter.AllCreaturesYouControl),
                condition = Conditions.YouControlAtLeast(count, GameObjectFilter.Creature),
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "193"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9c69d75-651f-4b75-b65d-79999d2069f6.jpg?1783923081"
        ruling(
            "2022-04-29",
            "If a creature with double strike loses double strike after dealing damage during the first combat damage step but before dealing damage in the second combat damage step, it will not deal damage during that second combat damage step. Notably, this means that if your ninth creature dies in the first combat damage step, the rest of your creatures won't deal combat damage again unless something else is granting them double strike."
        )
    }
}
