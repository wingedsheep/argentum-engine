package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bronzebeak Moa
 * {2}{G}{W}
 * Creature — Bird
 * 2/2
 * Whenever another creature you control enters, this creature gets +3/+3 until end of turn.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val BronzebeakMoa = card("Bronzebeak Moa") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 2
    oracleText = "Whenever another creature you control enters, this creature gets +3/+3 until end of turn."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.ModifyStats(3, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "James Ryman"
        flavorText = "They fight for their home with more loyalty than any conscript."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/291c0ebc-d489-42c7-8d8a-9216c333412f.jpg"
    }
}
