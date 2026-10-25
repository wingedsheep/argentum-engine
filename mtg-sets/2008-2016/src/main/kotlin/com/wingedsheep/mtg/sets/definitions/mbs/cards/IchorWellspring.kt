package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ichor Wellspring
 * {2}
 * Artifact
 * When this artifact enters or is put into a graveyard from the battlefield, draw a card.
 *
 * Two independent triggers: [Triggers.self] `enters()` and `dies()` (battlefield-to-graveyard,
 * not creature-only — Terrarion's shape). The second triggers even if the first hasn't resolved.
 */
val IchorWellspring = card("Ichor Wellspring") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters or is put into a graveyard from the battlefield, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
        description = "When this artifact enters, draw a card."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(1)
        description = "When this artifact is put into a graveyard from the battlefield, draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Steven Belledin"
        flavorText = "\"Our glorious infection has taken hold.\"\n—Elesh Norn, Grand Cenobite"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d1ea522-a0f6-45a8-8985-6fcca95d60cc.jpg?1783941368"
        ruling("2011-06-01", "The ability will trigger when Ichor Wellspring is put into a graveyard from the battlefield, even if the ability that triggered when it entered hasn't resolved yet.")
    }
}
