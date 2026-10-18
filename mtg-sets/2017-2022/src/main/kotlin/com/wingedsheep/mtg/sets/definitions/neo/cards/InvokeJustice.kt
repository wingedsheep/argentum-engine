package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invoke Justice — Kamigawa: Neon Dynasty #21 (canonical printing)
 * {1}{W}{W}{W}{W} · Sorcery
 *
 * Return target permanent card from your graveyard to the battlefield, then distribute four +1/+1
 * counters among any number of creatures and/or Vehicles target player controls.
 *
 * The recipients of the counters aren't targets — only the player is — so the split is made on
 * resolution among the creatures and Vehicles that player controls *after* the return, which is why
 * a returned creature (when you target yourself) can receive counters. The player target is threaded
 * into the filter explicitly, so it never confuses itself with the graveyard-card target.
 */
val InvokeJustice = card("Invoke Justice") {
    manaCost = "{1}{W}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return target permanent card from your graveyard to the battlefield, then distribute " +
        "four +1/+1 counters among any number of creatures and/or Vehicles target player controls."

    spell {
        val returned = target(TargetFilter.PermanentInYourGraveyard)
        val player = target(Targets.Player)
        effect = Effects.Move(returned, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD) then
            Effects.DistributeCountersAmongFiltered(
                totalCounters = 4,
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                filter = GameObjectFilter.CreatureOrVehicle.targetPlayerControls(player),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a598ab6-3254-49fe-b69f-e7c759129599.jpg?1783923919"
    }
}
