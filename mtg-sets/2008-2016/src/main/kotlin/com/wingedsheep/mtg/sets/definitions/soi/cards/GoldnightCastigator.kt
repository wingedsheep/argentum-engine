package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Goldnight Castigator — Shadows over Innistrad #162
 * {2}{R}{R} · Creature — Angel · 4/9
 *
 * Flying, haste
 * If a source would deal damage to you, it deals double that damage to you instead.
 * If a source would deal damage to this creature, it deals double that damage to this creature instead.
 *
 * Two separate replacement effects: each Castigator you control doubles damage to *you* (so two of
 * them quadruple it), but each only doubles damage dealt to *itself* — the rulings below.
 */
val GoldnightCastigator = card("Goldnight Castigator") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Angel"
    power = 4
    toughness = 9
    oracleText = "Flying, haste\n" +
        "If a source would deal damage to you, it deals double that damage to you instead.\n" +
        "If a source would deal damage to this creature, it deals double that damage to this creature instead."
    keywords(Keyword.FLYING, Keyword.HASTE)

    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.You)
        )
    )
    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.Self)
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "162"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca3d3c9a-d41b-4743-87a6-68d116460fe2.jpg?1783937751"
        ruling(
            "2016-04-08",
            "If multiple replacement effects could apply to damage being dealt to you or Goldnight " +
                "Castigator, you choose the order to apply those effects."
        )
        ruling(
            "2016-04-08",
            "If you control two Goldnight Castigators and a source would deal damage to you, it deals four " +
                "times that much damage instead. If you control a third Goldnight Castigator, the source " +
                "deals eight times that much damage to you, and so on. Damage dealt to a Goldnight " +
                "Castigator is only doubled, regardless of how many are on the battlefield."
        )
    }
}
