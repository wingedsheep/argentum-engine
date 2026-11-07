package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Expedition Raptor — Oath of the Gatewatch #18 (canonical printing)
 * {3}{W}{W} · Creature — Bird · 2/2
 *
 * Flying
 * When this creature enters, support 2. (Put a +1/+1 counter on each of up to two other target creatures.)
 *
 * Support on a permanent targets *other* creatures (any controller): Generous Patron's ETB shape.
 */
val ExpeditionRaptor = card("Expedition Raptor") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhen this creature enters, support 2. (Put a +1/+1 counter on each of up to two " +
        "other target creatures.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.OtherCreature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
        )
        description = "When this creature enters, support 2 — put a +1/+1 counter on each of up " +
            "to two other target creatures."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/819d11eb-960b-4116-ba09-f03bff88b4e7.jpg?1783937927"
        ruling("2016-01-22", "You can't put more than one +1/+1 counter on any one target using the support action.")
        ruling("2016-01-22", "Support can target a creature you don't control.")
        ruling("2016-01-22", "If a spell with support has other abilities that target creatures, those abilities and the support ability can target the same creature.")
    }
}
