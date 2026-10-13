package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Haunting Figment
 * {1}{U}
 * Creature — Illusion
 * 2/1
 * Vigilance
 * This creature can't be blocked as long as you've cast an instant or sorcery spell this turn.
 *
 * A source-scoped [CantBeBlocked] gated on [Conditions.YouCastSpellsThisTurn] (one instant or
 * sorcery). The condition counts casts, so a countered instant still turns the evasion on.
 */
val HauntingFigment = card("Haunting Figment") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Illusion"
    power = 2
    toughness = 1
    oracleText = "Vigilance\nThis creature can't be blocked as long as you've cast an instant or sorcery spell this turn."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantBeBlocked(),
            condition = Conditions.YouCastSpellsThisTurn(1, GameObjectFilter.InstantOrSorcery),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Chris Cold"
        flavorText = "Some bad dreams stay with you forever."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d14b07ea-18e4-41fb-8136-759d5349e88a.jpg?1783921350"
    }
}
