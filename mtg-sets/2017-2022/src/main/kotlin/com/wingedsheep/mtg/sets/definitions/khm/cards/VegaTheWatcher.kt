package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Vega, the Watcher — Kaldheim #233
 * {1}{W}{U} · Legendary Creature — Bird Spirit 2/2
 *
 * Flying
 * Whenever you cast a spell from anywhere other than your hand, draw a card.
 *
 * The negated cast-source predicate `SpellCastPredicate.CastFromZoneOtherThan(HAND)` — the same
 * trigger as Mizzix, Replica Rider.
 */
val VegaTheWatcher = card("Vega, the Watcher") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Bird Spirit"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhenever you cast a spell from anywhere other than your hand, draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.CastFromZoneOtherThan(Zone.HAND)))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Paul Scott Canavan"
        flavorText = "One eye sees the past, one the present, and one the future—and none can be deceived."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28fced7f-3078-4a54-8f76-0ef14c732e97.jpg?1783928189"
        ruling("2021-02-05", "Vega's ability will resolve before the spell that caused it to trigger. It will resolve even if that spell is countered.")
    }
}
