package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Rhino, Barreling Brute
 * {3}{R}{R}{G}{G}
 * Legendary Creature — Human Villain, 6/7
 * Vigilance, trample, haste
 * Whenever Rhino, Barreling Brute attacks, if you've cast a spell with mana value 4 or
 *   greater this turn, draw a card.
 */
val RhinoBarrelingBrute = card("Rhino, Barreling Brute") {
    manaCost = "{3}{R}{R}{G}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Villain"
    power = 6
    toughness = 7
    oracleText = "Vigilance, trample, haste\nWhenever Rhino, Barreling Brute attacks, if you've cast a spell with mana value 4 or greater this turn, draw a card."

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.YouCastSpellsThisTurn(atLeast = 1, filter = Filters.Unified.manaValueAtLeast(4))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Filipe Pagliuso"
        flavorText = "\"I'm Rhino. I knock things down. That's what I do. That's who I am.\"\n—Rhino, Aleksei Sytsevich"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b8ac400-1e98-49fc-94be-00386d15f2ae.jpg?1757377784"
    }
}
