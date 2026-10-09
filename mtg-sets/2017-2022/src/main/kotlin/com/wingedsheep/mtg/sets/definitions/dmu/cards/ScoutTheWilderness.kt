package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

val ScoutTheWilderness = card("Scout the Wilderness") {
    manaCost = "{2}{G}"
    colorIdentity = "WG"
    typeLine = "Sorcery"
    oracleText = "Kicker {1}{W} (You may pay an additional {1}{W} as you cast this spell.)\nSearch your library for a basic land card, put it onto the battlefield tapped, then shuffle. If this spell was kicked, create two 1/1 white Soldier creature tokens."

    keywordAbility(KeywordAbility.kicker("{1}{W}"))

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
        ) then Effects.If(
            Conditions.WasKicked,
            Effects.CreateToken(
                count = 2,
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Soldier"),
                imageUri = "https://cards.scryfall.io/normal/front/8/c/8c4b0257-2ca5-4015-9d63-d7cf6e87ab9d.jpg?1783921132",
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "176"
        artist = "A. M. Sartor"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5ccd67d4-1a22-4378-804d-6e2c93dc4938.jpg?1783921295"

        ruling("2022-09-09", "If Scout the Wilderness was kicked, you will put two 1/1 white Soldier tokens onto the battlefield whether you found a land card while searching your library or not.")
    }
}
