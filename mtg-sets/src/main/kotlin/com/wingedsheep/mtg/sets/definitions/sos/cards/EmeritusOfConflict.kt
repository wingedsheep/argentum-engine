package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Emeritus of Conflict // Lightning Bolt — Secrets of Strixhaven #113
 * {1}{R} · Creature — Human Wizard · 2/2
 *
 * First strike
 * Whenever you cast your third spell each turn, this creature becomes prepared.
 * (While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)
 * //
 * Lightning Bolt — {R}, Instant: Lightning Bolt deals 3 damage to any target.
 *
 * Prepare (Secrets of Strixhaven): unlike the "enters prepared" creatures in the cycle, Emeritus
 * of Conflict does NOT have the PREPARED keyword — it only becomes prepared via its trigger, the
 * third spell you cast each turn, through [Effects.BecomePrepared]. Becoming prepared creates a
 * copy of its prepare spell ("Lightning Bolt") in exile that its controller may cast for {R};
 * casting that copy unprepares the creature. Modeled via [com.wingedsheep.sdk.model.CardLayout.PREPARE]
 * + the `prepare(name) { }` DSL.
 */
val EmeritusOfConflict = card("Emeritus of Conflict") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "First strike\n" +
        "Whenever you cast your third spell each turn, this creature becomes prepared. " +
        "(While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)"

    keywords(Keyword.FIRST_STRIKE)

    // Whenever you cast your third spell each turn, this creature becomes prepared.
    triggeredAbility {
        trigger = Triggers.NthSpellCast(3, Player.You)
        effect = Effects.BecomePrepared(EffectTarget.Self)
    }

    // Lightning Bolt — the prepare spell. Deals 3 damage to any target.
    prepare("Lightning Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Lightning Bolt deals 3 damage to any target."
        spell {
            val t = target("target", AnyTarget())
            effect = DealDamageEffect(3, t)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "113"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f58dba4f-1abb-47a3-a684-29c32bab95c0.jpg?1778165054"
    }
}
