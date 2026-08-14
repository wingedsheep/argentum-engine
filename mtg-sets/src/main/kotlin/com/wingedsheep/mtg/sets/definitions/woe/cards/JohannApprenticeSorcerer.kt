package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary

/**
 * Johann, Apprentice Sorcerer
 * {2}{U}{R}
 * Legendary Creature — Human Wizard Sorcerer
 * 2/5
 * You may look at the top card of your library any time.
 * Once each turn, you may cast an instant or sorcery spell from the top of your library.
 * (You still pay its costs. Timing rules still apply.)
 */
val JohannApprenticeSorcerer = card("Johann, Apprentice Sorcerer") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Wizard Sorcerer"
    oracleText = "You may look at the top card of your library any time.\n" +
        "Once each turn, you may cast an instant or sorcery spell from the top of your library. " +
        "(You still pay its costs. Timing rules still apply.)"
    power = 2
    toughness = 5

    staticAbility {
        ability = LookAtTopOfLibrary
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(
            filter = GameObjectFilter.InstantOrSorcery,
            maxCastsPerTurn = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Dmitry Burmak"
        flavorText = "\"Okay. First I'll calm the elementals. Then I'll put out the fire. " +
            "Then I'll unflood the basement. Then I'll do the chores myself.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b88a762d-19ed-451d-a3a9-b3e7eea40f67.jpg?1783915071"
        ruling(
            "2023-09-01",
            "If Johann leaves the battlefield and returns, its new casting permission may be used " +
                "once even if the old object's permission was used that turn."
        )
    }
}
