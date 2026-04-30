package com.wingedsheep.mtg.sets.definitions.spiderman.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.conditions.SourceIsModified
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Skyward Spider
 * {W/U}{W/U}
 * Creature — Spider Human Hero
 * 2/2
 *
 * Ward {2}
 * This creature has flying as long as it's modified. (Equipment, Auras you control,
 * and counters are modifications.)
 */
val SkywardSpider = card("Skyward Spider") {
    manaCost = "{W/U}{W/U}"
    typeLine = "Creature — Spider Human Hero"
    power = 2
    toughness = 2
    oracleText = "Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)\nThis creature has flying as long as it's modified. (Equipment, Auras you control, and counters are modifications.)"

    keywords(Keyword.WARD)
    keywordAbility(KeywordAbility.ward("{2}"))

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, StaticTarget.SourceCreature),
            condition = SourceIsModified
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Bachzim"
        flavorText = "\"What would Uncle Jonah do if he saw me now?\"\n—Spider-Woman, Mattie Franklin"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5cbb580-cd02-4c60-acb7-b7ed1f1fce59.jpg?1757377845"
    }
}
