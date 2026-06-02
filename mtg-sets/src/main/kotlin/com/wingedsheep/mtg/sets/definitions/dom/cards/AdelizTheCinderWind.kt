package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Adeliz, the Cinder Wind
 * {1}{U}{R}
 * Legendary Creature — Human Wizard
 * 2/2
 * Flying, haste
 * Whenever you cast an instant or sorcery spell, Wizards you control get +1/+1 until end of turn.
 */
val AdelizTheCinderWind = card("Adeliz, the Cinder Wind") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Flying, haste\nWhenever you cast an instant or sorcery spell, Wizards you control get +1/+1 until end of turn."

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.YouCastInstantOrSorcery
        effect = GroupPatterns.modifyStatsForAll(
            power = 1,
            toughness = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().withSubtype("Wizard"))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Zezhou Chen"
        flavorText = "The passionate intensity of the Ghitu tempered by the cool insight of Tolarian training."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3c950e2-a43b-47f8-9ad6-1909ccc7acbf.jpg?1562911270"
        ruling("2018-04-27", "Adeliz's last ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2018-04-27", "Adeliz's last ability affects only Wizards you control at the time it resolves, including Adeliz itself.")
    }
}
