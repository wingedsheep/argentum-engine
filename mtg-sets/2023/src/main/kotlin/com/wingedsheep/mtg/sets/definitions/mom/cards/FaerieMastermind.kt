package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Faerie Mastermind
 * {1}{U}
 * Creature — Faerie Rogue
 * 2/1
 * Flash
 * Flying
 * Whenever an opponent draws their second card each turn, you draw a card.
 * {3}{U}: Each player draws a card.
 */
val FaerieMastermind = card("Faerie Mastermind") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Rogue"
    power = 2
    toughness = 1
    oracleText = "Flash\nFlying\n" +
        "Whenever an opponent draws their second card each turn, you draw a card.\n" +
        "{3}{U}: Each player draws a card."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anOpponent.drawsNth(2)
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Joshua Raphael"
        flavorText = "Yuta Takahashi, World Champion XXVII"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52d3005f-a1c7-4ef5-911f-ccc0752f4181.jpg?1783917037"
    }
}
