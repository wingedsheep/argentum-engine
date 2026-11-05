package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity

/**
 * Glyph Keeper — Amonkhet #55
 * {3}{U}{U} · Creature — Sphinx · Rare
 * 5/3
 *
 * Flying
 * Whenever this creature becomes the target of a spell or ability for the first time each turn,
 * counter that spell or ability.
 * Embalm {5}{U}{U}
 *
 * "That spell or ability" is the targeting object, not the triggering entity (the Sphinx itself),
 * so the counter reads `CounterTargetSource.TargetingSource`. Only the first targeting each turn
 * triggers; later spells and abilities resolve normally. The embalmed token is a
 * copy, so it has the same ability.
 */
val GlyphKeeper = card("Glyph Keeper") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    power = 5
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature becomes the target of a spell or ability for the first time each turn, " +
        "counter that spell or ability.\n" +
        "Embalm {5}{U}{U} ({5}{U}{U}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Sphinx with no mana cost. Embalm only as a sorcery.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.becomesTarget(firstTimeEachTurn = true)
        effect = Effects.CounterTargetingSource()
    }

    embalm("{5}{U}{U}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2663738-1688-4306-be6d-6a1f94a2319c.jpg?1783936521"
    }
}
