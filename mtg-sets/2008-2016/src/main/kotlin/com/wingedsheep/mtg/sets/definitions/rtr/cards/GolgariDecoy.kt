package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Golgari Decoy {3}{G}
 * Creature — Elf Rogue
 * 2/2
 *
 * All creatures able to block this creature do so.
 * Scavenge {3}{G}{G} ({3}{G}{G}, Exile this card from your graveyard: Put a number of +1/+1
 * counters equal to this card's power on target creature. Scavenge only as a sorcery.)
 *
 * Canonical printing: Return to Ravnica, the card's only real printing.
 *
 * The lure clause is the static blocking requirement Noble Quarry and Nath's Elite use;
 * scavenge uses the same graveyard-activated shape as Zanikev Locust.
 */
val GolgariDecoy = card("Golgari Decoy") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Rogue"
    power = 2
    toughness = 2
    oracleText = "All creatures able to block this creature do so.\n" +
        "Scavenge {3}{G}{G} ({3}{G}{G}, Exile this card from your graveyard: Put a number of +1/+1 " +
        "counters equal to this card's power on target creature. Scavenge only as a sorcery.)"

    staticAbility { ability = MustBeBlocked(allCreatures = true) }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{G}{G}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        val t = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.sourcePower(), t)
        description = "Scavenge {3}{G}{G}"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Marco Nelor"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/511a42a8-71ce-476f-98fa-fc0dc822edcf.jpg?1783940348"
        ruling(
            "2013-04-15",
            "Exiling the creature card with scavenge is part of the cost of activating the scavenge " +
                "ability. Once the ability is activated and the cost is paid, it's too late to stop the " +
                "ability from being activated by trying to remove the creature card from the graveyard.",
        )
    }
}
