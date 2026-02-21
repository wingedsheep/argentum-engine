package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.OwnerGainsLifeEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

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
        val t = target("target", TargetCreature())
        effect = CompositeEffect(
            listOf(
                MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true),
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
