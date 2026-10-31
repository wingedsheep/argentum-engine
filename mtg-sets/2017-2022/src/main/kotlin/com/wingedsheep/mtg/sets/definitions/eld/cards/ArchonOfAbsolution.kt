package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Archon of Absolution
 * {3}{W}
 * Creature — Archon
 * 3/2
 * Flying
 * Protection from white
 * Creatures can't attack you or planeswalkers you control unless their controller pays {1} for
 * each of those creatures.
 *
 * The tax is the unconditional form of Archangel of Tithes' [AttackTax]: {1} per attacker,
 * covering planeswalkers too.
 */
val ArchonOfAbsolution = card("Archon of Absolution") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Archon"
    oracleText = "Flying\nProtection from white\nCreatures can't attack you or planeswalkers you control unless their controller pays {1} for each of those creatures."
    power = 3
    toughness = 2

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.protectionFrom(Color.WHITE))

    staticAbility {
        ability = AttackTax(
            amountPerAttacker = DynamicAmounts.fixed(1),
            coversPlaneswalkers = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/684ca60c-7ed4-49e1-b54a-91d129539375.jpg?1783932681"
    }
}
