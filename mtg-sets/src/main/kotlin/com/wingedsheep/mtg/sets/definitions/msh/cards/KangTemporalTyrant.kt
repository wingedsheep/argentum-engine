package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kang, Temporal Tyrant — Marvel Super Heroes #217
 * {2}{U}{B} · Legendary Creature — Human Villain · 3/4
 *
 * Whenever Kang attacks, he connives.
 * Whenever you draw your second card each turn, each opponent loses 1 life and you gain 1 life.
 *
 * Not to be confused with Kang the Conqueror (MSH #62), a different card.
 *
 * Modeling notes:
 *  - "He connives" is the conniving permanent being Kang himself, so [Effects.Connive] over
 *    [EffectTarget.Self] — the Mob Lookout facade with the self target instead of a chosen one.
 *    The facade carries the whole draw → discard → conditional +1/+1 counter package, so the
 *    "if you discarded a nonland card" clause is not re-modelled here.
 *  - `Triggers.Attacks` is SELF-bound: it fires only for Kang attacking, matching "Whenever Kang
 *    attacks" (as opposed to the batch "whenever you attack").
 *  - "Whenever you draw your second card each turn" is [Triggers.NthCardDrawn], which reads the
 *    per-turn draw counter and fires exactly once even when a single multi-card draw crosses the
 *    threshold (Knights of Dol Amroth's precedent).
 *  - The second ability is *not* a drain: "each opponent loses 1 life **and** you gain 1 life" is a
 *    fixed 1 life gained regardless of how much life was actually lost, so it composes a plain
 *    [Effects.LoseLife] at `Player.EachOpponent` with a fixed [Effects.GainLife] — a `DrainLife`
 *    would scale the gain with the number of opponents and drop to 0 if the loss were prevented.
 */
val KangTemporalTyrant = card("Kang, Temporal Tyrant") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Villain"
    power = 3
    toughness = 4
    oracleText = "Whenever Kang attacks, he connives. (Draw a card, then discard a card. If you " +
        "discarded a nonland card, put a +1/+1 counter on this creature.)\n" +
        "Whenever you draw your second card each turn, each opponent loses 1 life and you gain 1 life."

    // Whenever Kang attacks, he connives.
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Connive(EffectTarget.Self)
        description = "Whenever Kang attacks, he connives."
    }

    // Whenever you draw your second card each turn, each opponent loses 1 life and you gain 1 life.
    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        effect = Effects.Composite(
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)),
            Effects.GainLife(1),
        )
        description = "Whenever you draw your second card each turn, each opponent loses 1 life " +
            "and you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "David Szabo"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb96ee86-5139-472a-9c4b-8a8a4280fc7e.jpg?1783902901"
    }
}
