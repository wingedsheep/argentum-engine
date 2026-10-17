package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Crew Captain — Streets of New Capenna #180
 * {B}{R}{G} · Creature — Human Warrior 4/2
 *
 * Haste
 * This creature has indestructible as long as it entered this turn.
 */
val CrewCaptain = card("Crew Captain") {
    manaCost = "{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Creature — Human Warrior"
    power = 4
    toughness = 2
    oracleText = "Haste\nThis creature has indestructible as long as it entered this turn."

    keywords(Keyword.HASTE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.source()),
            condition = Conditions.SourceEnteredThisTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Jakub Kasper"
        flavorText = "The Riveteers are as good at raising buildings as they are at razing them."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99806615-2f4a-4fe4-82f8-83445ae93a97.jpg?1783923089"
    }
}
