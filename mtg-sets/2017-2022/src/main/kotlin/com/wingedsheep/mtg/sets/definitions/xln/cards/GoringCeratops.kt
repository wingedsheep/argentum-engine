package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Goring Ceratops
 * {5}{W}{W}
 * Creature — Dinosaur
 * 3/3
 * Double strike
 * Whenever this creature attacks, other creatures you control gain double strike until end of turn.
 */
val GoringCeratops = card("Goring Ceratops") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dinosaur"
    power = 3
    toughness = 3
    oracleText = "Double strike\n" +
        "Whenever this creature attacks, other creatures you control gain double strike until end of turn."

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.grantKeywordToAll(Keyword.DOUBLE_STRIKE, GroupFilter.OtherCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Zezhou Chen"
        flavorText = "\"Follow her footsteps and she will show you the way.\"\n—Itzama the Crested"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/8309f684-5912-4191-9f64-d573f1cc84c9.jpg?1783935801"
    }
}
