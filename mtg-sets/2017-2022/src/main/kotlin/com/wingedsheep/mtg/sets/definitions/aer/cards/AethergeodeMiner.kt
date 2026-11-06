package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aethergeode Miner
 * {1}{W}
 * Creature — Dwarf Scout
 * 3/1
 * Whenever this creature attacks, you get {E}{E} (two energy counters).
 * Pay {E}{E}: Exile this creature, then return it to the battlefield under its owner's control.
 *
 * The blink returns a new object (CR 400.7): removed from combat, counters and Auras gone. If the
 * Miner has already left the battlefield when the ability resolves, `Self` no longer resolves and
 * neither move happens.
 */
val AethergeodeMiner = card("Aethergeode Miner") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Scout"
    power = 3
    toughness = 1
    oracleText = "Whenever this creature attacks, you get {E}{E} (two energy counters).\n" +
        "Pay {E}{E}: Exile this creature, then return it to the battlefield under its owner's control."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 2)
        effect = Effects.Move(EffectTarget.Self, Zone.EXILE) then
            Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD)
        description = "Pay {E}{E}: Exile this creature, then return it to the battlefield under its owner's control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Winona Nelson"
        flavorText = "Gremlins in the wild feed on nodules of pure aether, which are also prized by miners."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80bb3abd-ebaf-4dc2-97eb-ed4a2b005177.jpg?1783936787"
        ruling(
            "2017-02-09",
            "After Aethergeode Miner returns to the battlefield, it will be a new object with no connection to the " +
                "creature that was exiled. It won't be in combat or have any additional abilities it may have had " +
                "when it was exiled. Any +1/+1 counters on it or Auras attached to it are removed, and any " +
                "Equipment will no longer be attached."
        )
    }
}
