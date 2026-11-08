package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Highspire Infusion
 * {1}{G}
 * Instant
 * Target creature gets +3/+3 until end of turn. You get {E}{E} (two energy counters).
 *
 * The energy is part of the targeted spell's resolution, so if the creature becomes an illegal
 * target the spell doesn't resolve and no energy is gained.
 */
val HighspireInfusion = card("Highspire Infusion") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +3/+3 until end of turn. You get {E}{E} (two energy counters)."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, 3, t) then Effects.GetEnergy(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Anna Steinbauer"
        flavorText = "\"If nothing else, the Inventors' Fair taught us that we're only beginning to discover aether's true potential.\"\n—Kershan Danil, Highspire artisan"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08325c39-0eeb-4bd0-9adc-2892c1c6637e.jpg?1783936743"
        ruling(
            "2017-02-09",
            "If the target creature becomes an illegal target, the spell doesn't resolve and none of its effects happen. You won't get {E}{E}."
        )
    }
}
