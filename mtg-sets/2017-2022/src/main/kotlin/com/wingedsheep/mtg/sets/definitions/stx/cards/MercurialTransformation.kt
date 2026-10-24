package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mercurial Transformation — Strixhaven: School of Mages #47 (canonical printing)
 * {1}{U} · Sorcery — Lesson
 *
 * Until end of turn, target nonland permanent loses all abilities and becomes your choice of a
 * blue Frog creature with base power and toughness 1/1 or a blue Octopus creature with base power
 * and toughness 4/4.
 *
 * The Frog-or-Octopus choice is made as the spell resolves (ruling) — [Effects.ChooseAction]. Each
 * branch is the Trickster's Elk transform as a one-shot: [Effects.BecomeArtifact] with
 * `cardTypes = CREATURE` replaces every card type (CR 205.1a — an artifact, enchantment or
 * planeswalker stops being one; supertypes such as legendary stay), replaces every subtype with
 * Frog / Octopus, sets the color to blue and removes all abilities (Layer 6, timestamped). Then
 * [Effects.SetBasePowerAndToughness] sets the Layer 7b base P/T, so counters and pumps still apply
 * on top. Everything lasts until end of turn. Lesson is only a subtype.
 */
private fun mercurialForm(target: EffectTarget, subtype: String, size: Int): Effect =
    Effects.BecomeArtifact(
        target = target,
        cardTypes = setOf("CREATURE"),
        subtypes = setOf(subtype),
        colors = setOf(Color.BLUE),
        loseAllAbilities = true,
        duration = Duration.EndOfTurn
    ) then Effects.SetBasePowerAndToughness(size, size, target, Duration.EndOfTurn)

val MercurialTransformation = card("Mercurial Transformation") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery — Lesson"
    oracleText = "Until end of turn, target nonland permanent loses all abilities and becomes your " +
        "choice of a blue Frog creature with base power and toughness 1/1 or a blue Octopus " +
        "creature with base power and toughness 4/4."

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.ChooseAction(
            listOf(
                EffectChoice(
                    label = "It becomes a blue 1/1 Frog creature with no abilities",
                    effect = mercurialForm(permanent, "Frog", 1),
                ),
                EffectChoice(
                    label = "It becomes a blue 4/4 Octopus creature with no abilities",
                    effect = mercurialForm(permanent, "Octopus", 4),
                ),
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Craig J Spearing"
        flavorText = "\"For sun's sake, I just want a crab!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/d/adcbe115-5482-4e5a-95bb-8ccbb01d3547.jpg?1783927377"
        ruling("2021-04-16", "You choose Frog or Octopus as Mercurial Transformation resolves, not when you cast it.")
    }
}
