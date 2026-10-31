package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * War Screecher
 * {1}{W}
 * Creature — Bird
 * 1/3
 * Flying
 * {5}{W}, {T}: Other creatures you control get +1/+1 until end of turn.
 */
val WarScreecher = card("War Screecher") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "{5}{W}, {T}: Other creatures you control get +1/+1 until end of turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{W}"), Costs.Tap)
        effect = Patterns.Group.modifyStatsForAll(1, 1, GroupFilter.OtherCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Dan Murayama Scott"
        flavorText = "Soaring over Vitu-Ghazi, New Prahv, and Orzhova, the Conclave's trained falcons provided critical intelligence."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2ff7868-eeb9-4292-a2f4-cac18f9ba270.jpg?1783933471"
    }
}
