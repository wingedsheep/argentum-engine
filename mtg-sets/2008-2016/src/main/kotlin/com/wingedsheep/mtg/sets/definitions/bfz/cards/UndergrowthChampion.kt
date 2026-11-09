package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamageByRemovingCounter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Undergrowth Champion
 * {1}{G}{G}
 * Creature — Elemental
 * 2/2
 * If damage would be dealt to this creature while it has a +1/+1 counter on it, prevent that
 * damage and remove a +1/+1 counter from this creature.
 * Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature.
 *
 * The damage clause is [PreventDamageByRemovingCounter] with `requiresCounter = true` ("while it
 * has a +1/+1 counter on it") and the default one-counter removal — one counter per damage event
 * however much damage is prevented, the same shape as Oathsworn Knight.
 */
val UndergrowthChampion = card("Undergrowth Champion") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 2
    oracleText = "If damage would be dealt to this creature while it has a +1/+1 counter on it, " +
        "prevent that damage and remove a +1/+1 counter from this creature.\n" +
        "Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature."

    replacementEffect(PreventDamageByRemovingCounter(requiresCounter = true))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "197"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d53104d8-5b9a-45b6-a2f9-b2c5d231a03d.jpg?1783938183"
        ruling("2015-08-25", "If Undergrowth Champion is dealt damage from multiple sources at the same time (for example, if it's blocked by multiple creatures), its prevention effect will apply once to all that damage. You'll remove only one +1/+1 counter.")
        ruling("2015-08-25", "If damage that can't be prevented is dealt to Undergrowth Champion while it has a +1/+1 counter on it, you'll still remove a +1/+1 counter on it.")
        ruling("2024-11-08", "A landfall ability triggers whenever a land you control enters for any reason. It triggers whenever you play a land, as well as whenever a spell or ability puts a land onto the battlefield under your control.")
        ruling("2024-11-08", "A landfall ability doesn't trigger if a permanent already on the battlefield becomes a land.")
    }
}
