package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Ms. Marvel, Kamala Khan — Marvel Super Heroes #67 (rare)
 * {2}{U} · Legendary Creature — Mutant Inhuman Hero · 1/4
 *
 * Reach, vigilance
 * You have no maximum hand size.
 * Embiggen Fist — Whenever you cast a spell that targets a creature you control, draw a card.
 * Until end of turn, Ms. Marvel gains "Ms. Marvel's base power is equal to the number of cards in
 * your hand."
 *
 * Three of the four lines are existing vocabulary: the two keywords, [NoMaximumHandSize] (a
 * turn-based read in the cleanup step, not a Rule 613 continuous effect), and
 * `Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsMatching(filter)))` over `Creature.youControl()` — the same facade Iron Fist,
 * Mockingbird and Colleen Wing use in this set. Ms. Marvel is herself "a creature you control", so
 * a spell aimed at her arms the trigger too.
 *
 * The fourth line is the reason this card needed engine work. The granted clause must keep
 * *tracking* the hand for the rest of the turn — it is a quoted static ability, not a one-shot
 * number. `Effects.SetBasePower` used to evaluate its `DynamicAmount` once at resolution and stamp
 * a fixed value, which froze her power at whatever the hand happened to be. It now takes
 * `reevaluateContinuously = true`, which carries the `DynamicAmount` into the layer-7b floating
 * effect and re-reads it on every projection pass.
 *
 * Two rules notes on that clause:
 *  - It is **not** a characteristic-defining ability. CR 604.3a requires the ability to be printed
 *    on the card it affects (criterion 2) and to not be one the object grants to itself
 *    (criterion 4); this one fails both. So it applies in layer 7b — CR 613.4b, "effects that refer
 *    to the base power and/or toughness of a creature apply in this layer" — with the timestamp of
 *    the grant, rather than in layer 7a.
 *  - Only *power* is set. Her printed toughness of 4 is untouched, which is why the effect carries a
 *    null toughness rather than reusing the both-stats `SetBasePowerAndToughness`. Counters and pump
 *    spells are layer 7c and still apply on top.
 *
 * Ordering inside the trigger follows the printed text: draw first, then grant. Because the grant
 * re-evaluates, the drawn card is counted either way — the order is fidelity, not arithmetic.
 */
val MsMarvelKamalaKhan = card("Ms. Marvel, Kamala Khan") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Mutant Inhuman Hero"
    power = 1
    toughness = 4
    oracleText = "Reach, vigilance\n" +
        "You have no maximum hand size.\n" +
        "Embiggen Fist — Whenever you cast a spell that targets a creature you control, draw a " +
        "card. Until end of turn, Ms. Marvel gains \"Ms. Marvel's base power is equal to the " +
        "number of cards in your hand.\""

    keywords(Keyword.REACH, Keyword.VIGILANCE)

    staticAbility {
        ability = NoMaximumHandSize()
    }

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsMatching(GameObjectFilter.Creature.youControl())))
        effect = Effects.DrawCards(1) then
            Effects.SetBasePower(
                target = EffectTarget.Self,
                power = DynamicAmounts.cardsInYourHand(),
                duration = Duration.EndOfTurn,
                reevaluateContinuously = true,
            )
        description = "Embiggen Fist — Whenever you cast a spell that targets a creature you " +
            "control, draw a card. Until end of turn, Ms. Marvel gains \"Ms. Marvel's base power " +
            "is equal to the number of cards in your hand.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Smirtouille"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dd2d627-10fc-4045-8545-03bcf75e60ca.jpg?1783902954"
    }
}
