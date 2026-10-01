package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAlternativeCastingCost

/**
 * Primal Prayers
 * {2}{G}{G}
 * Enchantment
 *
 * When this enchantment enters, you get {E}{E} (two energy counters).
 * You may cast creature spells with mana value 3 or less by paying {E} rather than paying their
 * mana costs. If you cast a spell this way, you may cast it as though it had flash.
 *
 * The Jodah-style [GrantAlternativeCastingCost], narrowed by `spellFilter` and carrying its own
 * flash rider: the flash belongs to the {E} cast only, so the same creature cast for its mana cost
 * stays sorcery-speed. The {E} is the grant's non-mana half over the `{0}` mana idiom.
 */
val PrimalPrayers = card("Primal Prayers") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you get {E}{E} (two energy counters).\n" +
        "You may cast creature spells with mana value 3 or less by paying {E} rather than paying " +
        "their mana costs. If you cast a spell this way, you may cast it as though it had flash."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    staticAbility {
        ability = GrantAlternativeCastingCost(
            cost = "{0}",
            additionalCosts = listOf(Costs.additional.PayPlayerCounters(CounterType.ENERGY, 1)),
            spellFilter = GameObjectFilter.Creature.manaValueAtMost(3),
            asThoughFlash = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Yeong-Hao Han"
        flavorText = "When Gaea speaks, all of nature falls silent."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4eda3db-b47d-4f1e-a93d-e2ea747d935c.jpg?1783911257"

        ruling("2024-06-07", "If a spell you cast this way has {X} in its mana cost, you must choose 0 as the value of X when casting it.")
        ruling(
            "2024-06-07",
            "If you cast a spell for another cost \"rather than paying its mana cost,\" you can't choose to cast " +
                "it for any alternative costs. You can, however, pay additional costs, such as kicker costs. If " +
                "the spell has any mandatory additional costs, those must be paid to cast it."
        )
    }
}
