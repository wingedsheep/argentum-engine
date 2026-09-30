package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cutthroat Centurion
 * {2}{B}
 * Artifact Creature — Phyrexian Warrior
 * 2/2
 *
 * Sacrifice another artifact or creature: This creature gets +2/+2 until end of turn.
 * Activate only once each turn.
 */
val CutthroatCenturion = card("Cutthroat Centurion") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Phyrexian Warrior"
    oracleText = "Sacrifice another artifact or creature: This creature gets +2/+2 until end of turn. " +
        "Activate only once each turn."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.CreatureOrArtifact)
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Ariel Perez"
        flavorText = "\"We both serve the Demon Thane. Your parts would better serve with me.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3890961-d636-4ccc-9c42-2088a8531c9b.jpg?1783918048"
    }
}
