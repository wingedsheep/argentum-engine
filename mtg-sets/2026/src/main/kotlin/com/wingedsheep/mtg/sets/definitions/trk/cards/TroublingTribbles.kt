package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.model.Rarity

/**
 * Troubling Tribbles
 * {2}{W}
 * Creature — Tribble
 * 0/1
 *
 * This creature can't block.
 * At the beginning of your upkeep, create a token that's a copy of this creature.
 */
val TroublingTribbles = card("Troubling Tribbles") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Tribble"
    colorIdentity = "W"
    oracleText = "This creature can't block.\nAt the beginning of your upkeep, create a token that's a copy of this creature."
    power = 0
    toughness = 1
    staticAbility {
        ability = CantBlock()
    }
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.CreateTokenCopyOfSelf()
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "41"
        artist = "Filipe Pagliuso"
        flavorText = "\"They are detestable creatures.\"\n—Lieutenant Commander Worf"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0af3e535-83e6-422b-85c5-39d830cb072c.jpg?1792508160"
    }
}
