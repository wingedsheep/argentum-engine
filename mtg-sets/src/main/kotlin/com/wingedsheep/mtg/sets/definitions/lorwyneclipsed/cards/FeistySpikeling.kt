package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Feisty Spikeling
 * {1}{R/W}
 * Creature — Shapeshifter
 * 2/1
 *
 * Changeling (This card is every creature type.)
 * During your turn, this creature has first strike.
 */
val FeistySpikeling = card("Feisty Spikeling") {
    manaCost = "{1}{R/W}"
    typeLine = "Creature — Shapeshifter"
    power = 2
    toughness = 1
    oracleText = "Changeling (This card is every creature type.)\nDuring your turn, this creature has first strike."

    keywords(Keyword.CHANGELING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, StaticTarget.SourceCreature),
            condition = Conditions.IsYourTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "223"
        artist = "Tiffany Turrill"
        flavorText = "As the changeling found its form, the innocent mischief in its gaze was replaced with daring ferocity."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f69f3a27-ecda-4d27-82fe-612ed57dbb28.jpg?1767957251"
    }
}
