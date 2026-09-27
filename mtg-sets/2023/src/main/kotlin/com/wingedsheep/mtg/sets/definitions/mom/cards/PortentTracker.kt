package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Portent Tracker — {1}{G}
 * Creature — Satyr Scout 1/1 (common, MOM #201)
 *
 * {T}: Untap target land.
 * {T}: Choose target battle. If an opponent protects it, remove a defense counter from it.
 * Otherwise, put a defense counter on it. Activate only as a sorcery.
 *
 * "An opponent protects it" reads the battle's protector (CR 310.8), not its controller — a Siege
 * you cast is controlled by you but protected by an opponent — so the branch is a resolution-time
 * [Conditions.TargetMatchesFilter] over `Battle.protectedBy()`. "Otherwise" covers every battle an
 * opponent doesn't protect, i.e. one you protect.
 */
val PortentTracker = card("Portent Tracker") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Satyr Scout"
    oracleText = "{T}: Untap target land.\n" +
        "{T}: Choose target battle. If an opponent protects it, remove a defense counter from it. " +
        "Otherwise, put a defense counter on it. Activate only as a sorcery."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Tap
        val land = target(TargetFilter.Land)
        effect = Effects.Untap(land)
    }

    activatedAbility {
        cost = Costs.Tap
        val battle = target(TargetFilter.Battle)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Battle.protectedBy(), battle),
            then = Effects.RemoveCounters(CounterType.DEFENSE, 1, battle),
            otherwise = Effects.AddCounters(CounterType.DEFENSE, 1, battle)
        )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "201"
        artist = "Caroline Gariba"
        flavorText = "In the days before the invasion, the symbol of its perpetrator appeared in strange places all across the Multiverse."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc6104d4-d0af-40da-8227-14243d778e96.jpg?1783916963"
    }
}
