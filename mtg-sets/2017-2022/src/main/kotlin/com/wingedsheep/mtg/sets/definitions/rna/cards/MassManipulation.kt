package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mass Manipulation — Ravnica Allegiance #42
 * {X}{X}{U}{U}{U}{U} · Sorcery
 *
 * Gain control of X target creatures and/or planeswalkers.
 *
 * "X target" is exactly X (the Part Water shape), each a creature or planeswalker, then a
 * permanent [Effects.GainControl] on each target (the control change lasts indefinitely).
 */
val MassManipulation = card("Mass Manipulation") {
    manaCost = "{X}{X}{U}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Gain control of X target creatures and/or planeswalkers."

    spell {
        targets(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker), exactly = DynamicAmounts.xValue())
        effect = Effects.ForEachTarget(Effects.GainControl(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Anthony Palumbo"
        flavorText = "Those who cross the Dimir find enemies everywhere they turn."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/782760d7-59cc-4d40-a885-6a7980094fee.jpg?1783933708"
    }
}
