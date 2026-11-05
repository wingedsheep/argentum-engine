package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sentinel Totem — Ixalan #245
 * {1} · Artifact
 *
 * When this artifact enters, scry 1.
 * {T}, Exile this artifact: Exile all graveyards.
 *
 * The sweep is Lantern of the Lost's pipeline: gather every card in every graveyard, exile them.
 */
val SentinelTotem = card("Sentinel Totem") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, scry 1. (Look at the top card of your library. " +
        "You may put that card on the bottom.)\n" +
        "{T}, Exile this artifact: Exile all graveyards."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.ExileSelf)
        effect = Effects.Pipeline {
            val allGraveyards = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.Each,
                    filter = GameObjectFilter.Any,
                )
            )
            exile(allGraveyards)
        }
        description = "{T}, Exile this artifact: Exile all graveyards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "245"
        artist = "Anthony Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d8097eb-518a-4b8e-8d6a-a139d4ddcc8f.jpg?1783935701"
    }
}
