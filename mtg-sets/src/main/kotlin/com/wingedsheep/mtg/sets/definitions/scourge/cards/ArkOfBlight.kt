package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect

/**
 * Ark of Blight
 * {2}
 * Artifact
 * {3}, {T}, Sacrifice Ark of Blight: Destroy target land.
 */
val ArkOfBlight = card("Ark of Blight") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{3}, {T}, Sacrifice Ark of Blight: Destroy target land."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap, Costs.SacrificeSelf)
        target = Targets.Land
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Doug Chaffee"
        flavorText = "When opened, it erases entire landscapes from minds and maps."
        imageUri = "https://cards.scryfall.io/large/front/f/3/f3b09956-cc34-4472-8b9f-ae355522bd5a.jpg?1562536903"
    }
}
