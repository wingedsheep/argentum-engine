package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Deanna Troi, Psychic Counsel
 * {1}{W}
 * Legendary Creature — Betazoid Human Advisor
 * 2/2
 *
 * Other creatures you control enter with an additional +1/+1 counter on them.
 */
val DeannaTroiPsychicCounsel = card("Deanna Troi, Psychic Counsel") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Betazoid Human Advisor"
    oracleText = "Other creatures you control enter with an additional +1/+1 counter on them."
    power = 2
    toughness = 2

    replacementEffect(
        EntersWithCounters(
            count = 1,
            otherOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl(),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Lie Setiawan"
        flavorText = "\"Confidence is faith in oneself. It can't easily be given by another.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a027761-84e7-4cbf-a629-ae9bd3d8467b.jpg?1793100632"
    }
}
