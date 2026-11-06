package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Dynavolt Tower — Kaladesh #208
 * {3} · Artifact
 *
 * Whenever you cast an instant or sorcery spell, you get {E}{E} (two energy counters).
 * {T}, Pay {E}{E}{E}{E}{E}: This artifact deals 3 damage to any target.
 */
val DynavoltTower = card("Dynavolt Tower") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever you cast an instant or sorcery spell, you get {E}{E} (two energy counters).\n" +
        "{T}, Pay {E}{E}{E}{E}{E}: This artifact deals 3 damage to any target."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 5))
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t)
        description = "{T}, Pay {E}{E}{E}{E}{E}: This artifact deals 3 damage to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "208"
        artist = "Titus Lunter"
        flavorText = "\"I like that Tezzeret. He has vision the other judges don't. He sees the potential in what we're making here.\"\n—Faiz, inventor"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c45fd2ae-0f91-47ca-99bd-22ddf946df9f.jpg?1783937159"
    }
}
