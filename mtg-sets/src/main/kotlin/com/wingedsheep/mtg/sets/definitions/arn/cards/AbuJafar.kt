package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Abu Ja'far
 * {W}
 * Creature — Human
 * 0/1
 * When this creature dies, destroy all creatures blocking or blocked by it.
 * They can't be regenerated.
 *
 * The dies trigger destroys the combat pairing (CR 509) captured as last-known information
 * when Abu Ja'far left the battlefield — the live combat cross-references are torn down by the
 * time the trigger resolves, so [Effects.DestroyCreaturesBlockingOrBlockedBySource] reads the
 * snapshot carried on the leaves-battlefield event.
 */
val AbuJafar = card("Abu Ja'far") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human"
    power = 0
    toughness = 1
    oracleText = "When this creature dies, destroy all creatures blocking or blocked by it. They can't be regenerated."

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.DestroyCreaturesBlockingOrBlockedBySource(noRegenerate = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Ken Meyer, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e9ad288-d164-44a6-96ec-4185a1587f1a.jpg?1562897827"
    }
}
