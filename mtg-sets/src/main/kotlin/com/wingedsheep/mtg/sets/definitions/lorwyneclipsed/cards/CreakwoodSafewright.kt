package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Creakwood Safewright
 * {1}{B}
 * Creature — Elf Warrior
 * 5/5
 *
 * This creature enters with three -1/-1 counters on it.
 * At the beginning of your end step, if there is an Elf card in your graveyard and this
 * creature has a -1/-1 counter on it, remove a -1/-1 counter from this creature.
 */
val CreakwoodSafewright = card("Creakwood Safewright") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Elf Warrior"
    power = 5
    toughness = 5
    oracleText = "This creature enters with three -1/-1 counters on it.\n" +
        "At the beginning of your end step, if there is an Elf card in your graveyard and this " +
        "creature has a -1/-1 counter on it, remove a -1/-1 counter from this creature."

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.MinusOneMinusOne,
        count = 3,
        selfOnly = true
    ))

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.All(
            Conditions.GraveyardContainsSubtype(Subtype.ELF),
            Conditions.SourceHasCounter(CounterTypeFilter.MinusOneMinusOne)
        )
        effect = Effects.RemoveCounters(Counters.MINUS_ONE_MINUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Heather Hudson"
        flavorText = "\"I will endure grasping vine and prickling bramble to preserve even one flower.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bcc24cf-776a-4182-bf77-a611ad90b28f.jpg?1767957099"
    }
}
