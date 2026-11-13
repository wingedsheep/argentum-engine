package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.convergeEntersWithCounters
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Prism Array — Battle for Zendikar #81
 * {4}{U} · Enchantment · Rare
 *
 * Converge — This enchantment enters with a crystal counter on it for each color of mana spent to
 * cast it.
 * Remove a crystal counter from this enchantment: Tap target creature.
 * {W}{U}{B}{R}{G}: Scry 3.
 *
 * Converge is an ability word: [convergeEntersWithCounters] composes an
 * `EntersWithDynamicCounters(CRYSTAL, DistinctColorsManaSpent)` replacement, so the counters are
 * there as it enters (zero if it was put onto the battlefield or cast without spending mana).
 * The tap ability has no mana or tap cost — it can be activated once per crystal counter, at
 * instant speed.
 */
val PrismArray = card("Prism Array") {
    manaCost = "{4}{U}"
    colorIdentity = "WUBRG"
    typeLine = "Enchantment"
    oracleText = "Converge — This enchantment enters with a crystal counter on it for each color of mana spent " +
        "to cast it.\n" +
        "Remove a crystal counter from this enchantment: Tap target creature.\n" +
        "{W}{U}{B}{R}{G}: Scry 3."

    convergeEntersWithCounters(CounterType.CRYSTAL)

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.RemoveCounterFromSelf(CounterType.CRYSTAL)
        effect = Effects.Tap(creature)
        description = "Remove a crystal counter: Tap target creature."
    }

    activatedAbility {
        cost = Costs.Mana("{W}{U}{B}{R}{G}")
        effect = Effects.Scry(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Philip Straub"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e18c46d-dd6b-4feb-9eb3-3d52985ce6e1.jpg?1783938208"

        ruling(
            "2015-08-25",
            "If you cast a spell with converge without spending any mana to cast it (perhaps because an effect " +
                "allowed you to cast it without paying its mana cost), then the number of colors spent to cast it " +
                "will be zero."
        )
        ruling(
            "2015-08-25",
            "If there are any alternative or additional costs to cast a spell with a converge ability, the mana " +
                "spent to pay those costs will count."
        )
    }
}
