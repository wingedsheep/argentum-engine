package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ognis, the Dragon's Lash
 * {1}{B/R}{R}{R/G}
 * Legendary Creature — Lizard Warrior
 * 3/3
 *
 * Haste
 * Whenever a creature you control with haste attacks, create a tapped Treasure token.
 *
 * The attack trigger fans out once per attacking creature (CR 603.2c) and the haste check
 * reads projected state, so creatures granted haste by continuous effects count — and Ognis
 * itself, having haste, triggers its own ability.
 */
val OgnisTheDragonsLash = card("Ognis, the Dragon's Lash") {
    manaCost = "{1}{B/R}{R}{R/G}"
    colorIdentity = "BRG"
    typeLine = "Legendary Creature — Lizard Warrior"
    power = 3
    toughness = 3
    oracleText = "Haste\n" +
        "Whenever a creature you control with haste attacks, create a tapped Treasure token."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withKeyword(Keyword.HASTE)).attacks()
        effect = Effects.CreateTreasure(tapped = true)
        description = "Whenever a creature you control with haste attacks, create a tapped Treasure token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "210"
        artist = "Andreas Zafiratos"
        flavorText = "Ziatora's second-in-command has a habit of appearing when and where her enemies least expect her."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/636058c3-bacc-4a66-8988-832ab0cc9be6.jpg?1783923075"
    }
}
