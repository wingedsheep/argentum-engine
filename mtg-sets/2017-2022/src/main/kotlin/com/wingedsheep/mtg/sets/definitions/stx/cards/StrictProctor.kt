package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CounterTarget
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource

/**
 * Strict Proctor — Strixhaven: School of Mages #33
 * {1}{W} · Creature — Spirit Cleric · 1/3
 *
 * The meta-trigger keys on the engine's entry-cause stamp: every enters-the-battlefield shaped
 * trigger (a permanent's own ETB, another permanent's "whenever a [...] enters", landfall — even
 * from a graveyard) is put on the stack first, then Strict Proctor's trigger goes on above it
 * (CR 603.3b), so it resolves first and taxes the ability while it waits. "As it enters" and
 * enters-with-counters/tapped replacement effects aren't triggered abilities and are untouched.
 */
val StrictProctor = card("Strict Proctor") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Spirit Cleric"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever a permanent entering causes a triggered ability to trigger, counter that ability " +
        "unless its controller pays {2}."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anyPlayer.permanentEnteringTriggersAbility()
        effect = Effects.CounterUnlessPays(
            "{2}",
            target = CounterTarget.Ability,
            targetSource = CounterTargetSource.TriggeringEntity,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Jokubas Uogintas"
        flavorText = "\"You might believe being dead for centuries has made me more lenient. I think you'll find the opposite to be true.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95f1e36a-6838-49de-b7dc-697cbcd1e892.jpg?1783927383"
    }
}
