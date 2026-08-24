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
 * Zelyon Sword
 * {3}
 * Artifact
 * You may choose not to untap this artifact during your untap step.
 * {3}, {T}: Target creature gets +2/+0 for as long as this artifact remains tapped.
 *
 * Endoskeleton's shape: the optional-untap flag (CR 502.3) keeps the artifact tapped for as long
 * as its controller wants, and [Duration.WhileSourceTapped] ties the bonus to that state rather
 * than to end of turn.
 */
val ZelyonSword = card("Zelyon Sword") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "You may choose not to untap this artifact during your untap step.\n" +
        "{3}, {T}: Target creature gets +2/+0 for as long as this artifact remains tapped."

    flags(AbilityFlag.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val target = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = ModifyStatsEffect(2, 0, target, Duration.WhileSourceTapped("this artifact"))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Scott Kirschner"
        flavorText = "No sheath shall hold what finds its home in flesh."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4137160b-5248-4fbd-8ae8-25e9afd8fb5c.jpg?1783947880"
    }
}
