package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Hystrodon
 * {4}{G}
 * Creature — Beast
 * 3/4
 * Trample
 * Whenever Hystrodon deals combat damage to a player, you may draw a card.
 * Morph {1}{G}{G}
 */
val Hystrodon = card("Hystrodon") {
    manaCost = "{4}{G}"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 4
    oracleText = "Trample\nWhenever Hystrodon deals combat damage to a player, you may draw a card.\nMorph {1}{G}{G}"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = MayEffect(DrawCardsEffect(1))
    }

    morph = "{1}{G}{G}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "266"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/large/front/1/c/1c964473-7c54-4c2d-a3eb-dba01c842103.jpg?1562901719"
    }
}
