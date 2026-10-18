package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Seismic Wave — Kamigawa: Neon Dynasty #161 (canonical printing)
 * {2}{R} · Instant
 *
 * Seismic Wave deals 2 damage to any target and 1 damage to each nonartifact creature target
 * opponent controls.
 *
 * Two targets: "any target" and the opponent. The group is bound to the opponent target by
 * reference ([GameObjectFilter.targetPlayerControls] with the target), not by the implicit
 * "first player target" lookup — "any target" may itself be a player (even you), and the 1
 * damage must still land only on the targeted opponent's creatures. The creatures aren't
 * targets; they're read when the spell resolves.
 */
val SeismicWave = card("Seismic Wave") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Seismic Wave deals 2 damage to any target and 1 damage to each nonartifact creature " +
        "target opponent controls."

    spell {
        val anyTarget = target(Targets.Any)
        val opponent = target(Targets.Opponent)
        effect = Effects.DealDamage(2, anyTarget) then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.nonartifact().targetPlayerControls(opponent)),
                Effects.DealDamage(1, EffectTarget.IterationEntity)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Aaron J. Riley"
        flavorText = "\"Step carefully, Imperial. Even the earth rises against you.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e55b8ffb-c2e4-4676-9051-ff6c686cad0b.jpg?1783923859"
    }
}
