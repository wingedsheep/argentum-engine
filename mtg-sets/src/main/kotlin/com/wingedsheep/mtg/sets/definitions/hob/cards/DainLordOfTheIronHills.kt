package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.storied
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dáin, Lord of the Iron Hills
 * {1}{W}
 * Legendary Creature — Dwarf Noble
 * 2/2
 *
 * Vigilance
 * Storied.
 * As long as you have an enduring story, creatures can't attack you unless their controller pays {1}
 * for each of those creatures.
 *
 * A Ghostly Prison that only switches on with the enduring story. The gate rides [AttackTax.condition]
 * rather than a [com.wingedsheep.sdk.scripting.ConditionalStaticAbility] wrapper: `AttackPhaseManager.
 * calculateTotalAttackTax` scans `cardDef.staticAbilities` **raw** and only recognizes a bare
 * [AttackTax], so a wrapped one would silently tax nothing. The field exists for exactly this shape
 * (Archangel of Tithes gates on being untapped); it is evaluated with Dáin's controller as "you", which
 * is the player being attacked.
 */
val DainLordOfTheIronHills = card("Dáin, Lord of the Iron Hills") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Dwarf Noble"
    oracleText = "Vigilance\n" +
        "Storied (If you control three or more artifacts, legendaries, and/or Sagas, you have an " +
        "enduring story for the rest of the game.)\n" +
        "As long as you have an enduring story, creatures can't attack you unless their controller " +
        "pays {1} for each of those creatures."
    power = 2
    toughness = 2

    keywords(Keyword.VIGILANCE)
    storied()

    staticAbility {
        ability = AttackTax(
            amountPerAttacker = DynamicAmount.Fixed(1),
            condition = Conditions.YouHaveEnduringStory
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Tomas Duchek"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99d27749-d16c-45e9-accc-6a01351c17f9.jpg?1785496921"
    }
}
