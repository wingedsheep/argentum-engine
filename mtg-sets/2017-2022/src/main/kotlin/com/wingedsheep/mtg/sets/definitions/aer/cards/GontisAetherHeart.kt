package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Gonti's Aether Heart — Aether Revolt #152
 * {6} · Legendary Artifact
 *
 * Whenever Gonti's Aether Heart or another artifact you control enters, you get {E}{E}.
 * Pay eight {E}, Exile Gonti's Aether Heart: Take an extra turn after this one.
 *
 *  - The energy trigger is Dragonspark Reactor's "this or another artifact you control enters"
 *    (`Triggers.a(Artifact.youControl()).enters()`, ANY binding, so it sees the Heart itself).
 *  - The activation is a composite of the energy payment and the exile-self cost (no tap symbol),
 *    cashing in for the Magistrate's Scepter extra turn.
 */
val GontisAetherHeart = card("Gonti's Aether Heart") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "Whenever Gonti's Aether Heart or another artifact you control enters, you get {E}{E} " +
        "(two energy counters).\n" +
        "Pay eight {E}, Exile Gonti's Aether Heart: Take an extra turn after this one."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.youControl()).enters()
        effect = Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.PayPlayerCounters(CounterType.ENERGY, 8),
            Costs.ExileSelf
        )
        effect = Effects.TakeExtraTurn()
        description = "Take an extra turn"
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "152"
        artist = "Vincent Proce"
        flavorText = "Despite the magic that sustains Gonti's life, the crime lord is often described as heartless."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e4fc5ed-e90d-4965-985a-ed126a713506.jpg?1783936730"
    }
}
