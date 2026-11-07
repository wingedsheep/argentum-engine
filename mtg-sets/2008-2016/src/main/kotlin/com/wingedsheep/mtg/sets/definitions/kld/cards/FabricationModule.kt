package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fabrication Module — Kaladesh #211
 * {3} · Artifact
 *
 * Whenever you get one or more {E} (energy counters), put a +1/+1 counter on target creature you
 * control.
 * {4}, {T}: You get {E}.
 *
 * The energy trigger is the player-keyed `Triggers.you.getsCounters` (Territorial Gorger / Brotherhood
 * Scribe shape): one grant of several {E} fires it once, so it places one +1/+1 counter, not one per {E}
 * (ruling). Its own activated ability feeds it.
 */
val FabricationModule = card("Fabrication Module") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever you get one or more {E} (energy counters), put a +1/+1 counter on target " +
        "creature you control.\n" +
        "{4}, {T}: You get {E}."

    triggeredAbility {
        trigger = Triggers.you.getsCounters(CounterType.ENERGY)
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.GetEnergy(1)
        description = "{4}, {T}: You get {E}."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Aaron Miller"
        flavorText = "Inspiration leads to design."
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f93a9693-f899-47ac-8ee0-3549d9333fff.jpg?1783937158"
        ruling(
            "2016-09-20",
            "If you get multiple {E} at once, you only put one +1/+1 counter on a creature, not one counter " +
                "for each {E}. If you get multiple {E} at different times, Fabrication Module's ability " +
                "triggers once each time."
        )
    }
}
