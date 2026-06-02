package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Savage Offensive
 * {1}{R}
 * Sorcery
 * Kicker {G}
 * Creatures you control gain first strike until end of turn. If this spell was kicked,
 * they get +1/+1 until end of turn.
 */
val SavageOffensive = card("Savage Offensive") {
    manaCost = "{1}{R}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Kicker {G} (You may pay an additional {G} as you cast this spell.)\n" +
        "Creatures you control gain first strike until end of turn. If this spell was kicked, " +
        "they get +1/+1 until end of turn."

    keywordAbility(KeywordAbility.kicker("{G}"))

    spell {
        effect = GroupPatterns.grantKeywordToAll(Keyword.FIRST_STRIKE, Filters.Group.creaturesYouControl) then
            ConditionalEffect(
                condition = WasKicked,
                effect = GroupPatterns.modifyStatsForAll(1, 1, Filters.Group.creaturesYouControl)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/356744f3-e444-4f4e-bf00-80bb6b2ef76f.jpg?1562905776"
    }
}
