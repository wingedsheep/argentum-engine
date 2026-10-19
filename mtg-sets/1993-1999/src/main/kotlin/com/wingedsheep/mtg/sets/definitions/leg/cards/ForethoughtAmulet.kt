package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CapDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Forethought Amulet
 * {5}
 * Artifact
 * At the beginning of your upkeep, sacrifice this artifact unless you pay {3}.
 * If an instant or sorcery source would deal 3 or more damage to you, it deals 2 damage to you instead.
 *
 * The upkeep tax is the Sunken City shape ([Effects.PayOrSuffer] + [SacrificeSelfEffect]). The
 * damage clause is the Divine Presence [CapDamage] replacement narrowed to damage dealt to you by an
 * instant or sorcery source (the Harsh Judgment source filter): `maxAmount = 2` turns "3 or more → 2"
 * exactly, and damage of 2 or less is below the cap and left alone.
 */
val ForethoughtAmulet = card("Forethought Amulet") {
    manaCost = "{5}"
    typeLine = "Artifact"
    oracleText = "At the beginning of your upkeep, sacrifice this artifact unless you pay {3}.\n" +
        "If an instant or sorcery source would deal 3 or more damage to you, it deals 2 damage to you instead."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Mana("{3}"),
            suffer = SacrificeSelfEffect,
        )
    }

    replacementEffect(
        CapDamage(
            maxAmount = 2,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.You,
                source = GameObjectFilter.InstantOrSorcery,
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "277"
        artist = "Melissa A. Benson"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/700f53d3-0a84-4c55-8495-786f0f0783db.jpg?1783948029"
        ruling("2004-10-04", "This does not reduce damage which is already 2 or 1.")
    }
}
