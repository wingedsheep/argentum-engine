package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Jadzi, Steward of Fate // Oracle's Gift — Secrets of Strixhaven #55
 * {2}{U} · Legendary Creature — Human Wizard · 2/4
 *
 * Jadzi enters prepared. (While it's prepared, you may cast a copy of its spell. Doing so
 * unprepares it.)
 * When Jadzi enters, draw two cards, then discard two cards.
 * //
 * Oracle's Gift — {X}{X}{U}, Sorcery:
 * Create X 0/0 green and blue Fractal creature tokens, then put X +1/+1 counters on each Fractal
 * you control.
 *
 * Prepare (Secrets of Strixhaven): Jadzi enters with the PREPARED keyword (`CardLayout.PREPARE` +
 * the `prepare(name) { }` DSL). While prepared, its controller may cast a free copy of the back
 * face "Oracle's Gift"; doing so unprepares it.
 *
 * "Create X ... then put X +1/+1 counters on each Fractal you control" composes from atoms:
 *  - [Effects.CreateToken] with `count = DynamicAmount.XValue` (X is the {X}{X} paid for the
 *    prepare spell — evaluated once at resolution, CR 613.4c) for the 0/0 green/blue Fractals.
 *  - [Effects.ForEachInGroup] over every Fractal you control (the just-made tokens *and* any
 *    pre-existing Fractals), each iteration adding X +1/+1 counters to the iterated permanent
 *    (`EffectTarget.Self` inside a ForEach body, per the linter's iteration-space rule).
 */
val JadziStewardOfFate = card("Jadzi, Steward of Fate") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 4
    oracleText = "Jadzi enters prepared. (While it's prepared, you may cast a copy of its spell. " +
        "Doing so unprepares it.)\n" +
        "When Jadzi enters, draw two cards, then discard two cards."

    keywords(Keyword.PREPARED)

    // When Jadzi enters, draw two cards, then discard two cards.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(2).then(Effects.Discard(2))
    }

    // Oracle's Gift — the prepare spell.
    prepare("Oracle's Gift") {
        manaCost = "{X}{X}{U}"
        typeLine = "Sorcery"
        oracleText = "Create X 0/0 green and blue Fractal creature tokens, then put X +1/+1 " +
            "counters on each Fractal you control."
        spell {
            effect = Effects.CreateToken(
                count = DynamicAmount.XValue,
                power = 0,
                toughness = 0,
                colors = setOf(Color.GREEN, Color.BLUE),
                creatureTypes = setOf(Subtype.FRACTAL.value),
                imageUri = "https://cards.scryfall.io/normal/front/d/e/de564776-9d88-4533-8717-842eecdd0594.jpg?1775828279"
            ).then(
                Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.FRACTAL)).youControl(),
                    Effects.AddDynamicCounters(
                        counterType = Counters.PLUS_ONE_PLUS_ONE,
                        amount = DynamicAmount.XValue,
                        target = EffectTarget.Self,
                    ),
                ),
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Martina Fačková"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a95b6baf-01e6-49c3-9a26-394b127d53c3.jpg?1775937293"
    }
}
