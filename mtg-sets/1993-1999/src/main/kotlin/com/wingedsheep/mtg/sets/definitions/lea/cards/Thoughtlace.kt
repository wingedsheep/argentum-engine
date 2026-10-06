package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * Thoughtlace
 * {U}
 * Instant
 * Target spell or permanent becomes blue. (Mana symbols on that permanent remain unchanged.)
 *
 * No duration in the oracle text, so the Layer-5 color change is permanent. The target is a
 * spell on the stack or a permanent on the battlefield ([TargetSpellOrPermanent]).
 */
val Thoughtlace = card("Thoughtlace") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target spell or permanent becomes blue. (Mana symbols on that permanent remain unchanged.)"

    spell {
        val t = target(TargetSpellOrPermanent())
        effect = Effects.ChangeColor(t, setOf(Color.BLUE), Duration.Permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23749375-1416-47a4-9251-52f41fe2fae9.jpg?1783948700"
    }
}
