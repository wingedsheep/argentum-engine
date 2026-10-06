package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

val Purelace = card("Purelace") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target spell or permanent becomes white. (Mana symbols on that permanent remain unchanged.)"

    spell {
        val subject = target(TargetSpellOrPermanent())
        effect = Effects.ChangeColor(subject, setOf(Color.WHITE), Duration.Permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Sandra Everingham"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2facf462-55cd-4da4-997f-2cf4add75628.jpg?1783948710"
    }
}
