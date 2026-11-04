package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Step

val TendershootDryad = card("Tendershoot Dryad") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dryad"
    power = 2
    toughness = 2
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "At the beginning of each upkeep, create a 1/1 green Saproling creature token.\n" +
        "Saprolings you control get +2/+2 as long as you have the city's blessing."

    // Ascend on a permanent is a static ability that checks continuously (CR 702.131b), so the
    // keyword is the whole implementation — the engine's 702.131b state-based action
    // (AscendCitysBlessingCheck) grants the city's blessing the moment you control ten permanents,
    // not only on the turn this entered.
    keywords(Keyword.ASCEND)

    // At the beginning of each upkeep, create a 1/1 green Saproling token.
    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling")
        )
    }

    // Saprolings you control get +2/+2 as long as you have the city's blessing.
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 2,
                toughnessBonus = 2,
                filter = GroupFilter.allPermanentsWithSubtype("Saproling").youControl()
            ),
            condition = Conditions.YouHaveCitysBlessing
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "147"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a159830a-698f-423c-9da0-a734b210ecab.jpg?1783935280"
    }
}
