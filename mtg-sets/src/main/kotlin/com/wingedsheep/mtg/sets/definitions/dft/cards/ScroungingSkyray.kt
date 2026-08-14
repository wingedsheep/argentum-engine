package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Scrounging Skyray — Aetherdrift #60
 * {1}{U} · Creature — Fish Pirate · 1/2
 *
 * Flying
 * Whenever you discard one or more cards, put that many +1/+1 counters on this creature.
 * Cycling {2}
 *
 * Batch-worded discard payoff (CR 603.2c), so it uses [Triggers.YouDiscardOneOrMore] — one trigger
 * per discard *event*, however many cards it contained — and reads the batch size back through
 * [ContextPropertyKey.TRIGGER_DISCARD_COUNT] ("that many"). Discarding three cards to one effect
 * puts three counters on; three sequential single discards fire three triggers for one each. Same
 * shape as Magmakin Artillerist's damage rider.
 *
 * Cycling *this* card never triggers its own ability: cycling discards it from hand, and the
 * battlefield-only trigger doesn't function from hand. Cycling anything else while this is on the
 * battlefield does fire it for 1.
 */
val ScroungingSkyray = card("Scrounging Skyray") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Fish Pirate"
    power = 1
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever you discard one or more cards, put that many +1/+1 counters on this creature.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YouDiscardOneOrMore
        effect = Effects.AddDynamicCounters(
            Counters.PLUS_ONE_PLUS_ONE,
            DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DISCARD_COUNT),
            EffectTarget.Self,
        )
        description = "Whenever you discard one or more cards, put that many +1/+1 counters on " +
            "this creature."
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Ron Spears"
        flavorText = "\"Oi, you ain't gonna believe what these kelpheads just threw away!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d60bece3-6f63-4d9e-bca0-cef2d38f1472.jpg?1783907904"
    }
}
