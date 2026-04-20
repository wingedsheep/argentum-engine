package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PayLifeEffect

/**
 * Voracious Tome-Skimmer
 * {U/B}{U/B}{U/B}
 * Creature — Faerie Rogue
 * 2/3
 *
 * Flying
 * Whenever you cast a spell during an opponent's turn, you may pay 1 life. If you do, draw a card.
 */
val VoraciousTomeSkimmer = card("Voracious Tome-Skimmer") {
    manaCost = "{U/B}{U/B}{U/B}"
    typeLine = "Creature — Faerie Rogue"
    power = 2
    toughness = 3
    oracleText = "Flying\nWhenever you cast a spell during an opponent's turn, " +
        "you may pay 1 life. If you do, draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YouCastSpell
        triggerCondition = Conditions.IsNotYourTurn
        effect = EffectPatterns.mayPay(PayLifeEffect(1), Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "250"
        artist = "Loïc Canavaggia"
        flavorText = "She spoils the book before you read it."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a696fe5-410c-4699-b1d1-1a9c771d664a.jpg?1767658586"
    }
}
