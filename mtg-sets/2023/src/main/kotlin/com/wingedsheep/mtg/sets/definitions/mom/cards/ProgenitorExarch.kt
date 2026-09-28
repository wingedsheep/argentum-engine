package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Progenitor Exarch — March of the Machine #32
 * {X}{X}{W} · Creature — Phyrexian Cat Cleric · 1/2
 *
 * When this creature enters, incubate 3 X times.
 * {T}: Transform target Incubator token you control.
 *
 * X is read with `castX()` — the durable cast-time X that rides the permanent, so the enters
 * trigger sees it (the transient `XValue` would read 0 there). Each repetition is its own
 * incubate 3: X separate Incubator tokens, three +1/+1 counters each.
 */
val ProgenitorExarch = card("Progenitor Exarch") {
    manaCost = "{X}{X}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Cat Cleric"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, incubate 3 X times. (To incubate 3, create an Incubator " +
        "token with three +1/+1 counters on it and \"{2}: Transform this token.\" It transforms into a " +
        "0/0 Phyrexian artifact creature.)\n" +
        "{T}: Transform target Incubator token you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Repeat(DynamicAmounts.castX(), Effects.Incubate(3))
        description = "When this creature enters, incubate 3 X times."
    }

    activatedAbility {
        cost = Costs.Tap
        val incubator = target(
            TargetFilter(GameObjectFilter.Permanent.withSubtype("Incubator").token().youControl())
        )
        effect = Effects.Transform(incubator)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Marie Magny"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f86f7d03-ce71-498d-9dc5-2bd853ac0eae.jpg?1783917051"
    }
}
