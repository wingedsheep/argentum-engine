package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Liquimetal Coating
 * {2}
 * Artifact
 * {T}: Target permanent becomes an artifact in addition to its other types until end of turn.
 *
 * "In addition to its other types" is the additive [Effects.AddCardType] with
 * [Duration.EndOfTurn] — Liquimetal Torque's shape, but any permanent (lands included) is a legal
 * target.
 */
val LiquimetalCoating = card("Liquimetal Coating") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Target permanent becomes an artifact in addition to its other types until end of turn."

    activatedAbility {
        cost = Costs.Tap
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.AddCardType("ARTIFACT", permanent, Duration.EndOfTurn)
        description = "{T}: Target permanent becomes an artifact in addition to its other types until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Johann Bodin"
        flavorText = "\"They'll soon become accustomed to wearing skin that is not their own.\"\n—Elesh Norn, Grand Cenobite"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43ec9201-06e7-4a70-8dcf-7462a019965d.jpg?1783941706"
        ruling("2011-01-01", "You may target any permanent with the ability, including an artifact.")
        ruling("2011-01-01", "Becoming an artifact doesn't change what color(s) a permanent is.")
    }
}
