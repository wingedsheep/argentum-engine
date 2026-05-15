package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

val TendershootDryad = card("Tendershoot Dryad") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dryad"
    power = 2
    toughness = 2
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "At the beginning of each upkeep, create a 1/1 green Saproling creature token.\n" +
        "Saprolings you control get +2/+2 as long as you have the city's blessing."

    keywords(Keyword.ASCEND)

    // Ascend: when this enters, if you control 10+ permanents, gain the city's blessing.
    // Wired explicitly per CR 702.131a (Ascend on a permanent spell == ETB intervening-if).
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ConditionalEffect(
            condition = Conditions.ControlPermanentsAtLeast(10),
            effect = Effects.GainCitysBlessing()
        )
    }

    // At the beginning of each upkeep, create a 1/1 green Saproling token.
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling"),
            imageUri = "https://cards.scryfall.io/normal/front/5/9/590abe94-9c71-4429-b1b5-8b5de877de03.jpg?1721427861"
        )
    }

    // Saprolings you control get +2/+2 as long as you have the city's blessing.
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 2,
                toughnessBonus = 2,
                filter = GroupFilter.allCreaturesWithSubtype("Saproling").youControl()
            ),
            condition = Conditions.YouHaveCitysBlessing
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a60bdb4-53b3-46f2-b7a1-7954117b42a1.jpg?1721429404"
    }
}
