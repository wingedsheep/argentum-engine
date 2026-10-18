package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Okiba Salvage — Kamigawa: Neon Dynasty #118 (canonical printing)
 * {4}{B} · Sorcery
 *
 * Return target creature or Vehicle card from your graveyard to the battlefield. Then put two
 * +1/+1 counters on that permanent if you control an artifact and an enchantment.
 *
 * Back on Track's reanimation (the returned card keeps its entity id across the move, so the
 * counters land on the same object) with Soul Exchange's gated follow-up. The control check is
 * made on resolution, after the return, so a returned artifact counts toward it; a single
 * artifact enchantment satisfies both halves (the Kami of Terrible Secrets shape).
 */
val OkibaSalvage = card("Okiba Salvage") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target creature or Vehicle card from your graveyard to the battlefield. " +
        "Then put two +1/+1 counters on that permanent if you control an artifact and an enchantment."

    spell {
        val returned = target(TargetFilter(GameObjectFilter.CreatureOrVehicle.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Move(returned, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD) then
            Effects.If(
                condition = Conditions.All(
                    Conditions.YouControl(GameObjectFilter.Artifact),
                    Conditions.YouControl(GameObjectFilter.Enchantment),
                ),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, returned)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Lie Setiawan"
        flavorText = "\"Some fool would rather throw a whole mech away than repair one hydraulic actuator? " +
            "It must be my lucky day.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9e4485d-c208-46b4-9b5d-48e55e2ee182.jpg?1783923877"
    }
}
