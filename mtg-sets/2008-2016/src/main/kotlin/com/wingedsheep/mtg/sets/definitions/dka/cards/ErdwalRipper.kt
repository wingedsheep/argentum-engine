package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Erdwal Ripper
 * {1}{R}{R}
 * Creature — Vampire
 * 2/1
 * Haste
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 */
val ErdwalRipper = card("Erdwal Ripper") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire"
    oracleText = "Haste\n" +
        "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."
    power = 2
    toughness = 1
    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Kev Walker"
        flavorText = "Savage vampires lurk in the Erdwal's network of passageways where prey is plentiful and easy to catch."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/769ea5e9-6d05-4bc6-8f14-00eb2532c8b5.jpg"
    }
}
