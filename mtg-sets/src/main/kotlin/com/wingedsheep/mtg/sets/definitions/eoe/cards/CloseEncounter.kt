package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.dsl.Costs

/**
 * Close Encounter
 * {1}{G}
 * Instant
 *
 * As an additional cost to cast this spell, choose a creature you control or
 * a warped creature card you own in exile.
 * Close Encounter deals damage equal to the power of the chosen creature or
 * card to target creature.
 *
 * Rulings:
 *  - If you chose a creature on the battlefield, use that creature's power
 *    when Close Encounter resolves to determine how much damage is dealt.
 *    If that creature is no longer on the battlefield when Close Encounter
 *    resolves, use that creature's power as it last existed on the battlefield
 *    to determine how much damage is dealt.
 */
val CloseEncounter = card("Close Encounter") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, choose a creature you control or a warped creature card you own in exile.\n" +
        "Close Encounter deals damage equal to the power of the chosen creature or card to target creature."

    // "You control" / "you own" are implicit — per-zone iteration already
    // restricts to the caster's battlefield and the caster's exile sub-zone.
    additionalCost(
        Costs.additional.ChooseEntity(
            zoneFilters = mapOf(
                Zone.BATTLEFIELD to GameObjectFilter.Creature,
                Zone.EXILE to GameObjectFilter.Creature.warpExiled(),
            ),
            storeAs = "chosen",
            captureSnapshot = true,
            descriptionOverride = "choose a creature you control or a warped creature card you own in exile",
        )
    )

    spell {
        val damaged = target("creature", Targets.Creature)
        effect = Effects.DealDamage(
            DynamicAmount.EntityProperty(
                EntityReference.FromCostStorage("chosen"),
                EntityNumericProperty.Power,
            ),
            damaged,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Inkognit"
        flavorText = "Many mistake terrasymbiotic philosophy as pacifism. They are rarely wrong twice."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8a77351-9115-470f-8141-222c1916b337.jpg?1752947272"

        ruling(
            "2025-07-25",
            "If you chose a creature on the battlefield, use that creature's power when Close Encounter resolves to determine how much damage is dealt. If that creature is no longer on the battlefield when Close Encounter resolves, use that creature's power as it last existed on the battlefield to determine how much damage is dealt."
        )
    }
}
