package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Manglehorn — Amonkhet #175 (canonical printing)
 * {2}{G} · Creature — Beast · 2/2
 *
 * When this creature enters, you may destroy target artifact.
 * Artifacts your opponents control enter tapped.
 */
val Manglehorn = card("Manglehorn") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may destroy target artifact.\n" +
        "Artifacts your opponents control enter tapped."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val artifact = target(TargetFilter.Artifact)
        effect = Effects.May(Effects.Destroy(artifact))
        description = "When this creature enters, you may destroy target artifact."
    }

    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Artifact.opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Lius Lasahido"
        flavorText = "\"Rely too much on your weapons and watch your dreams shatter in your hands.\"\n" +
            "—Rhonas, god of strength"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0aa3a844-97e6-4f5d-a36f-56fea4e06932.jpg?1783936473"
        ruling("2017-04-18", "If an artifact enters the battlefield under an opponent's control at the same time that Manglehorn enters the battlefield under your control, that artifact won't enter tapped.")
    }
}
