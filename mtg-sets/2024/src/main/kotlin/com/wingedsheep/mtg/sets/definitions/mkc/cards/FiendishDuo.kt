package com.wingedsheep.mtg.sets.definitions.mkc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Fiendish Duo — Murders at Karlov Manor Commander #153
 * {4}{R}{R} · Creature — Devil · 5/5
 *
 * First strike
 * If a source would deal damage to an opponent, it deals double that damage to that player instead.
 *
 * The doubling reads any source — Fiendish Duo's controller's or an opponent's own — but only
 * damage to an opponent *player*; permanents opponents control are not covered.
 *
 * Canonical here rather than Game Night 2019 (GN2, the first printing): a Game Night box set isn't
 * a canonical-eligible set type, so MKC is the earliest real printing.
 */
val FiendishDuo = card("Fiendish Duo") {
    manaCost = "{4}{R}{R}"
    typeLine = "Creature — Devil"
    power = 5
    toughness = 5
    oracleText = "First strike (This creature deals combat damage before creatures without first strike.)\n" +
        "If a source would deal damage to an opponent, it deals double that damage to that player instead."
    keywords(Keyword.FIRST_STRIKE)

    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.Opponent)
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "153"
        artist = "Lucas Graciano"
        flavorText = "Half the size, double the mayhem."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebb44595-56f6-4be5-80bb-618ddd320f4a.jpg?1783912997"
    }
}
