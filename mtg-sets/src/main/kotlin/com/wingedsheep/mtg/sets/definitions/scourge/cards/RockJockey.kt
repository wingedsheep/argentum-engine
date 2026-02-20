package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.PlayedLandThisTurn
import com.wingedsheep.sdk.scripting.effects.PreventLandPlaysThisTurnEffect

/**
 * Rock Jockey
 * {2}{R}
 * Creature — Goblin
 * 3/3
 * You can't cast this spell if you've played a land this turn.
 * You can't play lands if you cast this spell this turn.
 */
val RockJockey = card("Rock Jockey") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Goblin"
    power = 3
    toughness = 3
    oracleText = "You can't cast this spell if you've played a land this turn.\nYou can't play lands if you cast this spell this turn."

    spell {
        castOnlyIf(NotCondition(PlayedLandThisTurn))
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = PreventLandPlaysThisTurnEffect
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Brian Snõddy"
        flavorText = "\"Everybody out of my way!\""
        imageUri = "https://cards.scryfall.io/large/front/8/f/8f836e90-3255-48bd-a174-6a127528551e.jpg?1562532100"
    }
}
