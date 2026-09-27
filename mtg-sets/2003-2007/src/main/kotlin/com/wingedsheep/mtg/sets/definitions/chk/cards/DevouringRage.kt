package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Devouring Rage — Champions of Kamigawa #164
 * {4}{R} · Instant — Arcane
 *
 * As an additional cost to cast this spell, you may sacrifice any number of Spirits.
 * Target creature gets +3/+0 until end of turn. For each Spirit sacrificed this way, that creature
 * gets an additional +3/+0 until end of turn.
 */
val DevouringRage = card("Devouring Rage") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant — Arcane"
    oracleText = "As an additional cost to cast this spell, you may sacrifice any number of Spirits.\n" +
        "Target creature gets +3/+0 until end of turn. For each Spirit sacrificed this way, that creature " +
        "gets an additional +3/+0 until end of turn."

    additionalCost(Costs.additional.SacrificePermanents(GameObjectFilter.Permanent.withSubtype("Spirit")))

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(
            power = 3 + DynamicAmounts.permanentsSacrificedThisWay() * 3,
            toughness = DynamicAmounts.fixed(0),
            target = creature,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Vance Kovacs"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/608a6e1e-3e95-4ce4-aaf6-5f14c0456850.jpg?1783944301"
    }
}
