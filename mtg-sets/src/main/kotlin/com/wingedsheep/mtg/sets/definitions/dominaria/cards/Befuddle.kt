package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Befuddle
 * {2}{U}
 * Instant
 * Target creature gets -4/-0 until end of turn. Draw a card.
 */
val Befuddle = card("Befuddle") {
    manaCost = "{2}{U}"
    typeLine = "Instant"
    oracleText = "Target creature gets -4/-0 until end of turn. Draw a card."

    spell {
        val t = target("target", TargetCreature())
        effect = Effects.ModifyStats(-4, 0, t)
            .then(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Svetlin Velinov"
        flavorText = "\"The trick to talking sense into Keldons is getting them to hold still. I learned that from Radha.\" —Jhoira"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88858285-23ac-4d7e-b8dd-a20982d95a2d.jpg?1562911270"
    }
}
