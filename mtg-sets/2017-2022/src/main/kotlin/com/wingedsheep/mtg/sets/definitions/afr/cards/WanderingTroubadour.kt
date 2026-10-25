package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wandering Troubadour
 * {3}{G}
 * Creature — Dragon Bard
 * 4/2
 * At the beginning of your end step, if you had a land enter the battlefield under your control
 * this turn, venture into the dungeon. (Enter the first room or advance to the next room.)
 *
 * The intervening "if" reads the per-turn entry log, so a land that entered and has since left
 * the battlefield still counts.
 */
val WanderingTroubadour = card("Wandering Troubadour") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dragon Bard"
    oracleText = "At the beginning of your end step, if you had a land enter the battlefield under your control this turn, venture into the dungeon. (Enter the first room or advance to the next room.)"
    power = 4
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.PermanentTypeEnteredBattlefieldThisTurn(CardType.LAND)
        effect = Effects.VentureIntoTheDungeon()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Rudy Siswanto"
        flavorText = "\"There's a new ballad waiting to be born around every corner.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3c7f123-31c8-469b-85e5-d20c4d21b2a0.jpg?1783926452"
    }
}
