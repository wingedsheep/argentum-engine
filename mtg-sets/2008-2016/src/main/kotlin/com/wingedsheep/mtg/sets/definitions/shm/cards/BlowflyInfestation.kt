package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blowfly Infestation
 * {2}{B}
 * Enchantment
 * Whenever a creature dies, if it had a -1/-1 counter on it, put a -1/-1 counter on target creature.
 */
val BlowflyInfestation = card("Blowfly Infestation") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature dies, if it had a -1/-1 counter on it, put a -1/-1 counter on target creature."

    // "if it had a -1/-1 counter on it" reads the dying creature's last-known counters; once it has
    // died that fact can't change, so the intervening-if folds into the trigger's subject filter.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withCounter(CounterType.MINUS_ONE_MINUS_ONE)).dies()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, creature)
        description = "Whenever a creature dies, if it had a -1/-1 counter on it, put a -1/-1 counter on target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Drew Tucker"
        flavorText = "When the giant Tollek Worldslayer fell, a corruption erupted from his corpse to carry on his name."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae027b38-a6cc-41f7-8f0d-9d1402b46517.jpg?1783942756"
        ruling("2008-05-01", "When Blowfly Infestation's ability resolves, it will put a single -1/-1 counter on the targeted creature. It doesn't matter how many -1/-1 counters were on the creature that went to a graveyard as long as it had at least one on it.")
        ruling("2008-05-01", "This ability is mandatory. If you're the only player who controls any creatures when this ability triggers, you must target one of them.")
    }
}
