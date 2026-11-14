package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Turn to Frog — Magic 2012 #78 (canonical printing)
 * {1}{U} · Instant
 *
 * Until end of turn, target creature loses all abilities and becomes a blue Frog with base power
 * and toughness 1/1.
 *
 * "Becomes a blue Frog" replaces creature types and colors but keeps other card types and
 * supertypes (ruling), so it is [Effects.SetCreatureSubtypes] (Layer 4) + [Effects.ChangeColor]
 * (Layer 5) rather than the type-replacing [Effects.BecomeArtifact] Mercurial Transformation uses.
 * [Effects.RemoveAllAbilities] is Layer 6 and [Effects.SetBasePowerAndToughness] Layer 7b, so
 * counters and pumps still apply on top — the Chromium, the Mutable composition, aimed at a target.
 */
val TurnToFrog = card("Turn to Frog") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature loses all abilities and becomes a blue Frog with base power and toughness 1/1."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.RemoveAllAbilities(creature, Duration.EndOfTurn) then
            Effects.SetCreatureSubtypes(setOf("Frog"), creature, Duration.EndOfTurn) then
            Effects.ChangeColor(creature, setOf(Color.BLUE), Duration.EndOfTurn) then
            Effects.SetBasePowerAndToughness(1, 1, creature, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Warren Mahy"
        flavorText = "\"Ribbit.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b43d9a1e-0767-4a9b-81b4-4ff2f3dde1d5.jpg?1783941086"
        ruling("2014-07-18", "The creature will lose all other colors and creature types, but it will retain any other card types (such as artifact) or supertypes (such as legendary) it may have.")
        ruling("2014-07-18", "Turn to Frog overwrites all previous effects that set the creature's base power and toughness to specific values. Any power- or toughness-setting effects that start to apply after Turn to Frog resolves will overwrite this effect.")
        ruling("2014-07-18", "If the affected creature gains an ability after Turn to Frog resolves, it will keep that ability.")
        ruling("2014-07-18", "Effects that modify a creature's power and/or toughness, such as the effect of Titanic Growth, will apply to the creature no matter when they started to take effect. The same is true for any counters that change its power and/or toughness and effects that switch its power and toughness.")
    }
}
