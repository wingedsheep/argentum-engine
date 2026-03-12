package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Nature's Spiral
 * {1}{G}
 * Sorcery
 * Return target permanent card from your graveyard to your hand.
 */
val NaturesSpiral = card("Nature's Spiral") {
    manaCost = "{1}{G}"
    typeLine = "Sorcery"
    oracleText = "Return target permanent card from your graveyard to your hand."

    spell {
        val t = target("target", TargetObject(
            filter = TargetFilter(
                GameObjectFilter.Companion.Permanent.ownedByYou(),
                zone = Zone.GRAVEYARD
            )
        ))
        effect = MoveToZoneEffect(
            target = t,
            destination = Zone.HAND
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Florian de Gesincourt"
        flavorText = "\"As Argoth's last defenders fell to Urza's juggernauts, Titania said, 'Nature cannot be destroyed, only changed.'\""
        imageUri = "https://cards.scryfall.io/normal/front/6/6/6666b95e-80de-4bf7-bae6-8a7e991b38ad.jpg?1562736933"
    }
}
