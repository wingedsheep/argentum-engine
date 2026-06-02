package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
/**
 * Qarsi Revenant — Tarkir: Dragonstorm #86
 * {1}{B}{B} · Creature — Vampire · 3/3
 *
 * Flying, deathtouch, lifelink
 * Renew — {2}{B}, Exile this card from your graveyard: Put a flying counter, a deathtouch
 * counter, and a lifelink counter on target creature. Activate only as a sorcery.
 *
 * Flying / deathtouch / lifelink are keyword counters (CR 122.1d): the [StateProjector]'s
 * KEYWORD_COUNTER_MAP grants the matching keyword to any creature carrying the counter, so
 * the Renew ability simply stacks three [Effects.AddCounters] on one target.
 */
val QarsiRevenant = card("Qarsi Revenant") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 3
    toughness = 3
    oracleText = "Flying, deathtouch, lifelink\n" +
        "Renew — {2}{B}, Exile this card from your graveyard: Put a flying counter, a deathtouch " +
        "counter, and a lifelink counter on target creature. Activate only as a sorcery."

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH, Keyword.LIFELINK)

    renew("{2}{B}") {
        val creature = target("creature", Targets.Creature)
        effect = Effects.Composite(listOf(
            Effects.AddCounters(Counters.FLYING, 1, creature),
            Effects.AddCounters(Counters.DEATHTOUCH, 1, creature),
            Effects.AddCounters(Counters.LIFELINK, 1, creature)
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Lorenzo Mastroianni"
        flavorText = "\"Evil wears many faces. Keep your windows closed at night, or it might wear yours.\"\n—Sultai warning"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c93a0f6-5e50-4dda-9ff6-da741fb839ff.jpg?1743204304"
    }
}
