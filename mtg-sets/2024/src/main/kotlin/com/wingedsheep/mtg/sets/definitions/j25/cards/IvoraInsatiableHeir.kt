package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ivora, Insatiable Heir — Foundations Jumpstart #50 (canonical printing; its only printing)
 * {1}{R} · Legendary Creature — Vampire Warrior · 1/1
 *
 * Trample
 * When Ivora enters and whenever it deals combat damage to a player, create a Blood token.
 * Whenever you discard a card, put a +1/+1 counter on Ivora.
 *
 * The one printed ability with two trigger conditions is two triggered abilities sharing an effect.
 * The discard trigger fires once per card you discard, from any source — a Blood token's cost,
 * a loot, cleanup.
 */
val IvoraInsatiableHeir = card("Ivora, Insatiable Heir") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Vampire Warrior"
    power = 1
    toughness = 1
    oracleText = "Trample\n" +
        "When Ivora enters and whenever it deals combat damage to a player, create a Blood token. " +
        "(It's an artifact with \"{1}, {T}, Discard a card, Sacrifice this token: Draw a card.\")\n" +
        "Whenever you discard a card, put a +1/+1 counter on Ivora."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateBlood()
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateBlood()
    }

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Canata Katana"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2ba70366-b6ae-423a-a8d8-29d2b8afd939.jpg?1783908854"
    }
}
