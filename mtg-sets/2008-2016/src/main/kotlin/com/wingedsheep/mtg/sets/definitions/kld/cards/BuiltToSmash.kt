package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Built to Smash — Kaladesh #108
 * {R} · Instant
 *
 * Target attacking creature gets +3/+3 until end of turn. If it's an artifact creature, it gains
 * trample until end of turn.
 *
 * Same shape as [BuiltToLast]: the +3/+3 always applies and the trample grant is a
 * resolution-time rider over [Conditions.TargetMatchesFilter] on the same target.
 */
val BuiltToSmash = card("Built to Smash") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target attacking creature gets +3/+3 until end of turn. If it's an artifact creature, " +
        "it gains trample until end of turn."

    spell {
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(3, 3, creature) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.ArtifactCreature, creature),
                then = Effects.GrantKeyword(Keyword.TRAMPLE, creature),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Svetlin Velinov"
        flavorText = "Free from Consulate regulations, renegade-built automatons push performance to the limit."
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f054978-446c-4565-b51e-8f1f0e0f01e2.jpg?1783937197"
        ruling("2016-09-20", "An artifact creature gains trample in addition to getting +3/+3, not instead of it.")
    }
}
