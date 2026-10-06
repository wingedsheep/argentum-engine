package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * Lifelace
 * {G}
 * Instant
 * Target spell or permanent becomes green. (Mana symbols on that permanent remain unchanged.)
 *
 * No duration in the oracle text, so the Layer-5 color change is permanent.
 */
val Lifelace = card("Lifelace") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target spell or permanent becomes green. (Mana symbols on that permanent remain unchanged.)"

    spell {
        val t = target(TargetSpellOrPermanent())
        effect = Effects.ChangeColor(t, setOf(Color.GREEN), Duration.Permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38cb601b-a35c-412e-b386-e77dad3daa54.jpg?1783948675"
    }
}
