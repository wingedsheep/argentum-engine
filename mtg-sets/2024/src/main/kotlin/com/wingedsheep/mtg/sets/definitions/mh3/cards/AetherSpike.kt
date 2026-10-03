package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Aether Spike
 * {1}{U}
 * Instant
 * Choose target spell. You get {E}{E} (two energy counters), then you may pay any amount of {E}.
 * Counter that spell unless its controller pays {1} for each {E} paid this way.
 *
 * The energy amount is chosen on resolution (2024-06-07 ruling), so it is a `PayCounters` step
 * whose paid amount feeds the dynamic "unless pays" tax. Paying zero makes the tax {0}, which the
 * counter executor treats as paid — the spell resolves.
 */
val AetherSpike = card("Aether Spike") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose target spell. You get {E}{E} (two energy counters), then you may pay any amount of {E}. " +
        "Counter that spell unless its controller pays {1} for each {E} paid this way."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.GetEnergy(2) then
            Effects.PayCounters(CounterType.ENERGY, storeAmountAs = "paid") then
            Effects.CounterUnlessDynamicPays(DynamicAmounts.storedNumber("paid"))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Campbell White"
        flavorText = "\"The shield doesn't block the fire physically. It tells the fire to stop.\"\n" +
            "—Genku, future shaper"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/012f5195-10ab-4b32-a5de-a79341cd536a.jpg?1783911294"
        ruling("2024-06-07", "You choose the target spell as you cast Aether Spike. You don't choose how much {E} to pay until Aether Spike is resolving. No player may take actions between the time you choose how much {E} to pay and the time the target spell's controller chooses whether or not to pay {1} for each {E} paid this way.")
        ruling("2024-06-07", "You may pay zero {E}. You will get {E}{E}, and the target spell will resolve as long as its controller chooses to pay {0}. In most cases, this won't be a difficult choice for them.")
    }
}
