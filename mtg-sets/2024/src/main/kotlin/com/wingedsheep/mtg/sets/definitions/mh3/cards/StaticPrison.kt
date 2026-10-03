package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Static Prison
 * {W}
 * Enchantment
 *
 * When this enchantment enters, exile target nonland permanent an opponent controls until this
 * enchantment leaves the battlefield. You get {E}{E} (two energy counters).
 * At the beginning of your first main phase, sacrifice this enchantment unless you pay {E}.
 *
 * "Your first main phase" is the precombat main phase (CR 505.1a). The exile is the linked
 * "until leaves" shape: the ETB exiles and a leaves trigger returns the card. If the target is
 * illegal on resolution the whole ability fizzles, so no energy is gained either (ruling).
 */
val StaticPrison = card("Static Prison") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target nonland permanent an opponent controls " +
        "until this enchantment leaves the battlefield. You get {E}{E} (two energy counters).\n" +
        "At the beginning of your first main phase, sacrifice this enchantment unless you pay {E}."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter(GameObjectFilter.NonlandPermanent.opponentControls()))
        effect = Effects.ExileUntilLeaves(permanent) then Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.PayPlayerCounters(CounterType.ENERGY, 1),
            suffer = Effects.SacrificeTarget(EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd16222e-349c-4a2b-a7c8-8eb35a8ab332.jpg?1783911296"

        ruling("2024-06-07", "If Static Prison leaves the battlefield before its first triggered ability resolves, the target permanent won't be exiled.")
        ruling("2024-06-07", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
    }
}
