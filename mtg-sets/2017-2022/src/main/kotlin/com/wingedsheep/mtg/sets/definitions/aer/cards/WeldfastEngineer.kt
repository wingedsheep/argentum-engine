package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Weldfast Engineer
 * {1}{B}{R}
 * Creature — Human Artificer
 * 3/3
 * At the beginning of combat on your turn, target artifact creature you control gets +2/+0
 * until end of turn.
 */
val WeldfastEngineer = card("Weldfast Engineer") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Artificer"
    power = 3
    toughness = 3
    oracleText = "At the beginning of combat on your turn, target artifact creature you control gets +2/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val artifactCreature = target(TargetFilter(GameObjectFilter.ArtifactCreature.youControl()))
        effect = Effects.ModifyStats(2, 0, artifactCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Sara Winters"
        flavorText = "\"Let's err on the side of glorious mayhem.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51464df6-557f-47e5-838e-2a30145511f0.jpg"
    }
}
