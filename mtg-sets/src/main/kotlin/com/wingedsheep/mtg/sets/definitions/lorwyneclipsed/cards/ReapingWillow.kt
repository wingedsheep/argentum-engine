package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Reaping Willow
 * {1}{W/B}{W/B}{W/B}
 * Creature — Treefolk Cleric
 * 3/6
 *
 * Lifelink
 * This creature enters with two -1/-1 counters on it.
 * {1}{W/B}, Remove two counters from this creature: Return target creature card
 * with mana value 3 or less from your graveyard to the battlefield.
 * Activate only as a sorcery.
 */
val ReapingWillow = card("Reaping Willow") {
    manaCost = "{1}{W/B}{W/B}{W/B}"
    typeLine = "Creature — Treefolk Cleric"
    power = 3
    toughness = 6
    oracleText = "Lifelink\n" +
        "This creature enters with two -1/-1 counters on it.\n" +
        "{1}{W/B}, Remove two counters from this creature: Return target creature card " +
        "with mana value 3 or less from your graveyard to the battlefield. " +
        "Activate only as a sorcery."

    keywords(Keyword.LIFELINK)

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.MinusOneMinusOne,
        count = 2,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{W/B}"),
            Costs.RemoveCounterFromSelf(Counters.MINUS_ONE_MINUS_ONE, count = 2)
        )
        val creature = target(
            "target creature card with mana value 3 or less from your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Creature.ownedByYou().manaValueAtMost(3),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = MoveToZoneEffect(
            target = creature,
            destination = Zone.BATTLEFIELD
        )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "240"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97c33cc2-3573-410a-908b-c0392fff524b.jpg?1767952397"
        ruling(
            "2025-11-17",
            "If a card in a graveyard has {X} in its mana cost, X is 0 for the purpose of determining its mana value."
        )
    }
}
