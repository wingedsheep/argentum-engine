package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeEffect
import com.wingedsheep.sdk.scripting.DestroyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.OwnerGainsLifeEffect
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Path of Peace
 * {3}{W}
 * Sorcery
 * Destroy target creature. Its owner gains 4 life.
 */
val PathOfPeace = card("Path of Peace") {
    manaCost = "{3}{W}"
    typeLine = "Sorcery"

    spell {
        target = TargetCreature()
        effect = CompositeEffect(
            listOf(
                DestroyEffect(EffectTarget.ContextTarget(0)),
                OwnerGainsLifeEffect(4)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Pete Venters"
        flavorText = "\"The soldier reaped the profits of peace.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1f3e1c9-bfad-49a1-b171-6fa344ef2eef.jpg"
    }
}
