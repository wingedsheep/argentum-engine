package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Hivis of the Scale
 * {3}{R}{R}
 * Legendary Creature — Lizard Shaman
 * 3/4
 * You may choose not to untap Hivis during your untap step.
 * {T}: Gain control of target Dragon for as long as you control Hivis and Hivis remains tapped.
 */
val HivisOfTheScale = card("Hivis of the Scale") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Lizard Shaman"
    power = 3
    toughness = 4
    oracleText = "You may choose not to untap Hivis of the Scale during your untap step.\n{T}: Gain control of target Dragon for as long as you control Hivis of the Scale and Hivis of the Scale remains tapped."

    flags(AbilityFlag.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Tap
        val t = target("target", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Dragon"))
        ))
        effect = GainControlEffect(t, Duration.WhileSourceTapped())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "182"
        artist = "Andrew Robinson"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee84e61e-d99a-489b-a3b1-cb45fc81bce6.jpg?1562722404"
    }
}
