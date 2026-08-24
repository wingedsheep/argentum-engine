package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Spirit Shield
 * {3}
 * Artifact
 * You may choose not to untap this artifact during your untap step.
 * {2}, {T}: Target creature gets +0/+2 for as long as this artifact remains tapped.
 *
 * Endoskeleton's shape: the optional-untap flag (CR 502.3) keeps the artifact tapped for as long
 * as its controller wants, and [Duration.WhileSourceTapped] ties the bonus to that state rather
 * than to end of turn.
 */
val SpiritShield = card("Spirit Shield") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "You may choose not to untap this artifact during your untap step.\n" +
        "{2}, {T}: Target creature gets +0/+2 for as long as this artifact remains tapped."

    flags(AbilityFlag.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val target = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = ModifyStatsEffect(0, 2, target, Duration.WhileSourceTapped("this artifact"))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Scott Kirschner"
        flavorText = "At times, survival must outweigh all other considerations."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/213d6e0d-5ec9-441e-a38d-50ce44583e4b.jpg?1783947880"
    }
}
