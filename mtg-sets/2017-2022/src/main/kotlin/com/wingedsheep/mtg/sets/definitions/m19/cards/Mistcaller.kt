package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mistcaller — Core Set 2019 #62
 * {U} · Creature — Merfolk Wizard · 1/1
 *
 * Sacrifice this creature: Until end of turn, if a nontoken creature would enter and it wasn't
 * cast, exile it instead.
 *
 * The ability grants a turn-long, floating copy of Containment Priest's replacement: a
 * [RedirectZoneChange] to exile on battlefield entries of `Creature.nontoken().notCast()`. The grant
 * is anchored to the sacrificed source (allowed off-battlefield when the grant targets the ability's
 * own source), and a non-`selfOnly` redirect grant outlives its anchor; the filter carries no
 * controller predicate, so it applies to creatures entering under any player's control.
 */
val Mistcaller = card("Mistcaller") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 1
    toughness = 1
    oracleText = "Sacrifice this creature: Until end of turn, if a nontoken creature would enter " +
        "and it wasn't cast, exile it instead."

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.GrantReplacementEffect(
            replacement = RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.nontoken().notCast(),
                    to = Zone.BATTLEFIELD,
                ),
            ),
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        )
        description = "Sacrifice this creature: Until end of turn, if a nontoken creature would " +
            "enter and it wasn't cast, exile it instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Ryan Yee"
        flavorText = "Hospitality is of little concern to those who do not wish to be disturbed."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a35c1bd0-3d1a-4e46-b74d-11db6867e0d7.jpg?1783934583"
    }
}
