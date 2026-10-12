package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Trench Stalker
 * {4}{B}
 * Creature — Beast Horror
 * 4/5
 * As long as you've drawn two or more cards this turn, this creature has deathtouch and lifelink.
 *
 * Foggy Swamp Hunters' shape: two source-scoped [GrantKeyword] statics, each gated on
 * [Conditions.YouDrewCardsThisTurn] (threshold 2).
 */
val TrenchStalker = card("Trench Stalker") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Beast Horror"
    power = 4
    toughness = 5
    oracleText = "As long as you've drawn two or more cards this turn, this creature has deathtouch and lifelink."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DEATHTOUCH, GroupFilter.source()),
            condition = Conditions.YouDrewCardsThisTurn(2),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.LIFELINK, GroupFilter.source()),
            condition = Conditions.YouDrewCardsThisTurn(2),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Nino Vecia"
        flavorText = "It can hear a panicked heartbeat from a thousand paces away."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa1b987a-a3b9-4cd1-85b0-e58df4130934.jpg?1783920079"
    }
}
