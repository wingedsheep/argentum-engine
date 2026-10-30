package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Belle of the Brawl
 * {2}{B}
 * Creature — Human Knight
 * 3/2
 *
 * Menace
 * Whenever this creature attacks, other Knights you control get +1/+0 until end of turn.
 */
val BelleOfTheBrawl = card("Belle of the Brawl") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Knight"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever this creature attacks, other Knights you control get +1/+0 until end of turn."
    power = 3
    toughness = 2
    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.modifyStatsForAll(
            power = 1,
            toughness = 0,
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype("Knight").youControl(), excludeSelf = true),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Magali Villeneuve"
        flavorText = "Locthwain knights take pride in being the last ones standing in a fight or on a dance floor."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c3c992a-70b5-4d3c-9a96-93c3365691ac.jpg?1783932644"
    }
}
