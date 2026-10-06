package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * Deathlace
 * {B}
 * Instant
 * Target spell or permanent becomes black. (Mana symbols on that permanent remain unchanged.)
 *
 * The color change has no stated duration, so it lasts indefinitely (a Layer-5 effect that
 * replaces the object's colors with black).
 */
val Deathlace = card("Deathlace") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target spell or permanent becomes black. (Mana symbols on that permanent remain unchanged.)"

    spell {
        val t = target(TargetSpellOrPermanent())
        effect = Effects.ChangeColor(t, setOf(Color.BLACK), Duration.Permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Sandra Everingham"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6ff1cefc-62cb-4525-b0c5-2b09603b4314.jpg?1783948696"
    }
}
