package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Emergency Eject
 * {2}{W}
 * Instant
 * 
 * Destroy target nonland permanent. Its controller creates a Lander token. (It's an artifact with "{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.")
 */
val EmergencyEject = card("Emergency Eject") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target nonland permanent. Its controller creates a Lander token. (It's an artifact with \"{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\")"

    spell {
        val target = target("target nonland permanent", Targets.NonlandPermanent)
        effect = Effects.Destroy(target).then(
            Effects.CreateLander(controller = EffectTarget.TargetController)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Leon Tukker"
        flavorText = "\"This is your captain speaking. Our eternity drive has ruptured. Light be with you all.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc98b2d4-86fc-4c47-b2a7-1f3c89463607.jpg?1752946606"
    }
}
