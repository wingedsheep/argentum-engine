package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Radiant Strike
 * {3}{W}
 * Instant
 * Destroy target artifact or tapped creature. You gain 3 life.
 */
val RadiantStrike = card("Radiant Strike") {
    manaCost = "{3}{W}"
    typeLine = "Instant"
    oracleText = "Destroy target artifact or tapped creature. You gain 3 life."

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.TappedCreature)))
        effect = Effects.Composite(
                    listOf(
                        Effects.Destroy(t),
                        Effects.GainLife(3)
                    )
                )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Aleksi Briclot"
        flavorText = "\"The light does not ask permission to cut through the darkness.\"\n—Mentor Ultimate, to Regent Maximum Taman IV"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c38e4cb-918b-4493-872b-66c90dcfd339.jpg?1761826070"
    }
}
