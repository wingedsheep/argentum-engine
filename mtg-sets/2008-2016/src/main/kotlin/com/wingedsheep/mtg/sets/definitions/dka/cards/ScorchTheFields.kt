package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scorch the Fields
 * {4}{R}
 * Sorcery
 * Destroy target land. Scorch the Fields deals 1 damage to each Human creature.
 */
val ScorchTheFields = card("Scorch the Fields") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy target land. Scorch the Fields deals 1 damage to each Human creature."

    spell {
        val land = target(TargetFilter.Land)
        effect = Effects.Destroy(land) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.HUMAN)),
            Effects.DealDamage(1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Jaime Jones"
        flavorText = "\"Your fields will turn as black as a ghoul's tongue and fire shall be your only harvest.\"\n—Minaldra, the Vizag Atum"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05c4338d-e5c0-46b4-ab16-1f9aa97b4026.jpg"
    }
}
