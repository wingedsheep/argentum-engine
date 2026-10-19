package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Voltage Surge — Kamigawa: Neon Dynasty #171 (canonical printing)
 * {R} · Instant
 *
 * As an additional cost to cast this spell, you may sacrifice an artifact.
 * Voltage Surge deals 2 damage to target creature or planeswalker. If this spell's additional cost
 * was paid, Voltage Surge deals 4 damage instead.
 *
 * The unnamed optional additional cost rides [KeywordAbility.optionalAdditionalCost], so the
 * sacrifice is paid while casting and only [Conditions.AdditionalCostWasPaid] reads it — the spell
 * is never "kicked". "Instead" picks one damage event of 4, not 2 + 2.
 */
val VoltageSurge = card("Voltage Surge") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may sacrifice an artifact.\n" +
        "Voltage Surge deals 2 damage to target creature or planeswalker. If this spell's additional " +
        "cost was paid, Voltage Surge deals 4 damage instead."

    keywordAbility(KeywordAbility.optionalAdditionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Artifact)))

    spell {
        val permanent = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker))
        effect = Effects.If(
            condition = Conditions.AdditionalCostWasPaid,
            then = Effects.DealDamage(4, permanent),
            otherwise = Effects.DealDamage(2, permanent),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "171"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a9b7141-45b9-4629-a4e4-b50f93929717.jpg?1783923855"
    }
}
