package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Beanstalk Giant // Fertile Footsteps
 * {6}{G}
 * Creature — Giant
 * star/star
 *
 * Beanstalk Giant's power and toughness are each equal to the number of lands you control.
 *
 * Adventure: Fertile Footsteps — {2}{G}, Sorcery — Adventure
 * Search your library for a basic land card, put it onto the battlefield, then shuffle.
 *
 * The characteristic-defining ability is `dynamicStats` over lands you control (as
 * `inv/cards/MolimoMaroSorcerer.kt`); the Adventure half is the stock basic-land ramp tutor
 * (as `tsp/cards/SearchForTomorrow.kt`).
 */
val BeanstalkGiant = card("Beanstalk Giant") {
    manaCost = "{6}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Giant"
    oracleText = "Beanstalk Giant's power and toughness are each equal to the number of lands you control."

    dynamicStats(DynamicAmounts.landsYouControl())

    adventure("Fertile Footsteps") {
        manaCost = "{2}{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Search your library for a basic land card, put it onto the battlefield, then " +
            "shuffle. (Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                shuffleAfter = true
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a66f5ea7-ddbb-4b89-b812-77bd17972cf9.jpg?1783932613"
        ruling(
            "2019-10-04",
            "The ability that defines Beanstalk Giant's power and toughness works in all zones, not just the battlefield."
        )
    }
}
