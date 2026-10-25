package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragon's Fire — Adventures in the Forgotten Realms #139
 * {1}{R} · Instant
 *
 * As an additional cost to cast this spell, you may reveal a Dragon card from your hand or choose
 * a Dragon you control.
 * Dragon's Fire deals 3 damage to target creature or planeswalker. If you revealed a Dragon card or
 * chose a Dragon as you cast this spell, Dragon's Fire deals damage equal to the power of that card
 * or creature instead.
 *
 * The unnamed optional cost rides [KeywordAbility.optionalAdditionalCost] over a
 * `ChooseEntity` spanning your battlefield Dragons and the Dragon cards in your hand (the Monstrous
 * Emergence shape). `captureSnapshot` freezes a chosen battlefield Dragon's power so the damage
 * still reads its last-known power if it leaves before resolution (per the 2021-07-23 ruling).
 * The choice is stored under "beheld" — the key the spell's resolution pipeline always exposes
 * for cost-chosen cards — and "instead" is one damage event, never 3 plus the Dragon's power.
 */
val DragonsFire = card("Dragon's Fire") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may reveal a Dragon card from your hand " +
        "or choose a Dragon you control.\n" +
        "Dragon's Fire deals 3 damage to target creature or planeswalker. If you revealed a Dragon " +
        "card or chose a Dragon as you cast this spell, Dragon's Fire deals damage equal to the power " +
        "of that card or creature instead."

    keywordAbility(
        KeywordAbility.optionalAdditionalCost(
            Costs.additional.ChooseEntity(
                zoneFilters = mapOf(
                    Zone.HAND to GameObjectFilter.Any.withSubtype(Subtype.DRAGON),
                    Zone.BATTLEFIELD to GameObjectFilter.Any.withSubtype(Subtype.DRAGON),
                ),
                storeAs = "beheld",
                captureSnapshot = true,
                descriptionOverride = "Reveal a Dragon card from your hand or choose a Dragon you control",
            )
        )
    )

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.If(
            condition = Conditions.AdditionalCostWasPaid,
            then = Effects.DealDamage(DynamicAmounts.powerOf(EffectTarget.PipelineTarget("beheld")), t),
            otherwise = Effects.DealDamage(3, t),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/9666987a-5267-46d3-9b00-31baa92a9cbc.jpg?1783926480"
        ruling(
            "2021-07-23",
            "If you chose a Dragon you control, use that Dragon's power as Dragon's Fire resolves to " +
                "determine how much damage is dealt. If the Dragon you chose is no longer on the " +
                "battlefield as Dragon's Fire resolves, Dragon's Fire will deal damage equal to the " +
                "power it had the last time it was on the battlefield."
        )
    }
}
