package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Sage of the Inward Eye
 * {2}{U}{R}{W}
 * Creature — Djinn Wizard
 * 3/4
 * Flying
 * Whenever you cast a noncreature spell, creatures you control gain lifelink until end of turn.
 */
val SageOfTheInwardEye = card("Sage of the Inward Eye") {
    manaCost = "{2}{U}{R}{W}"
    colorIdentity = "WUR"
    typeLine = "Creature — Djinn Wizard"
    power = 3
    toughness = 4
    oracleText = "Flying\nWhenever you cast a noncreature spell, creatures you control gain lifelink until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = GroupPatterns.grantKeywordToAll(
            keyword = Keyword.LIFELINK,
            filter = GroupFilter.AllCreaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "195"
        artist = "Chase Stone"
        flavorText = "\"No one petal claims beauty for the lotus.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/9/297c336b-3f70-4518-b1f1-7b773774895d.jpg?1562784071"
    }
}
