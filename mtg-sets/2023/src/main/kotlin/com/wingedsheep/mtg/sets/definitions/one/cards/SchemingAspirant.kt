package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scheming Aspirant
 * {1}{B}
 * Creature — Phyrexian Advisor
 * 1/3
 *
 * Whenever you proliferate, each opponent loses 2 life and you gain 2 life.
 */
val SchemingAspirant = card("Scheming Aspirant") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Advisor"
    power = 1
    toughness = 3
    oracleText = "Whenever you proliferate, each opponent loses 2 life and you gain 2 life."

    triggeredAbility {
        trigger = Triggers.you.proliferates()
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Lauren K. Cannon"
        flavorText = "\"I have raised many of my own contenders for the Coliseum. " +
            "This one will be fearsome one day, mark my words.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8de80f71-82b5-499c-afc0-6bbae6b896ad.jpg?1783918041"
    }
}
