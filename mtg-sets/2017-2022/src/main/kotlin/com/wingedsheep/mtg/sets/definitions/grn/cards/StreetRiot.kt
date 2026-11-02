package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Street Riot
 * {4}{R}
 * Enchantment
 * During your turn, creatures you control get +1/+0 and have trample.
 */
val StreetRiot = card("Street Riot") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "During your turn, creatures you control get +1/+0 and have trample."

    staticAbility {
        condition = Conditions.IsYourTurn
        ability = ModifyStats(1, 0, GroupFilter.AllCreaturesYouControl)
    }
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Tomasz Jedruszek"
        flavorText = "\"They said obey and you'll be happy. They said you'll be safe. But we're not safe. " +
            "We're not happy. And we will not obey.\"\n—Domri Rade"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80b352d5-4a57-41bc-b4c4-8b81255a9db8.jpg?1783934157"
    }
}
