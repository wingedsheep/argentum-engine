package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.scripting.OnCycle
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.dsl.Effects

/**
 * Krosan Tusker
 * {5}{G}{G}
 * Creature — Boar Beast
 * 6/5
 * Cycling {2}{G}
 * When you cycle Krosan Tusker, you may search your library for a basic land card,
 * reveal that card, put it into your hand, then shuffle.
 */
val KrosanTusker = card("Krosan Tusker") {
    manaCost = "{5}{G}{G}"
    typeLine = "Creature — Boar Beast"
    power = 6
    toughness = 5
    oracleText = "Cycling {2}{G}\nWhen you cycle Krosan Tusker, you may search your library for a basic land card, reveal that card, put it into your hand, then shuffle."

    keywordAbility(KeywordAbility.cycling("{2}{G}"))

    triggeredAbility {
        trigger = OnCycle(controllerOnly = true)
        effect = MayEffect(
            Effects.SearchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffle = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "272"
        artist = "Kev Walker"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b872f85-60c5-44c4-956d-a8aa8132908b.jpg?1710406409"
    }
}
