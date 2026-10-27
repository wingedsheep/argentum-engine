package com.wingedsheep.mtg.sets.definitions.eve.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Crumbling Ashes
 * {1}{B}
 * Enchantment
 * At the beginning of your upkeep, destroy target creature with a -1/-1 counter on it.
 */
val CrumblingAshes = card("Crumbling Ashes") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, destroy target creature with a -1/-1 counter on it."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.withCounter(CounterType.MINUS_ONE_MINUS_ONE))
        )
        effect = Effects.Destroy(creature)
        description = "At the beginning of your upkeep, destroy target creature with a -1/-1 counter on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Chuck Lukacs"
        flavorText = "\"I was there to watch my brothers' quenching. I drew the slightest warmth from their dying rage.\"\n—Illulia of Nighthearth"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e73ac3c-bd5a-4993-be65-76396b7d38ad.jpg?1783942687"
        ruling("2008-08-01", "This ability is mandatory. If you're the only player who controls a creature with a -1/-1 counter on it as your upkeep begins, you must target one of your own creatures.")
        ruling("2008-08-01", "You can't target a creature unless it already has a -1/-1 counter on it as your upkeep begins. Putting a -1/-1 counter on a creature during your upkeep won't let Crumbling Ashes destroy it that turn.")
        ruling("2008-08-01", "If the targeted creature loses all its -1/-1 counters by the time the ability resolves, the ability won't resolve because its only target is illegal. The creature won't be destroyed.")
    }
}
