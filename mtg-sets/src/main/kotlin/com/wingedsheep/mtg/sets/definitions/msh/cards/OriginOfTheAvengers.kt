package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Origin of the Avengers — Marvel Super Heroes #28
 * {1}{W} · Enchantment — Saga
 *
 * I — Scry 2.
 * II — You may put a Hero creature card with mana value 3 or less from your hand onto the
 *      battlefield. If you don't, draw a card.
 * III — Put a +1/+1 counter on each creature you control.
 *
 * Modeling notes:
 *  - Chapter II is the Courier of Comestibles / Kellan, the Kid shape: [IfYouDoEffect] gating on
 *    whether the action actually moved a card, with an empty `ifYouDo` and the draw as
 *    `ifYouDont`. `Patterns.Hand.putFromHand` is a Gather → Select(`ChooseUpTo` 1) → Move
 *    pipeline, so the "you may" lives in the selection (declining, or having no legal card,
 *    both leave the battlefield unchanged) and `SuccessCriterion.Auto` reads the terminal move's
 *    destination growth. That makes "If you don't" fire for a decline *and* for an empty hand —
 *    exactly the card's wording, which keys off the put, not off a yes/no answer.
 *  - Chapter III's "each creature you control" is [Effects.ForEachInGroup] over a
 *    [GroupFilter], counters applied to `EffectTarget.Self` per iterated creature
 *    (Cathars' Crusade's shape) — not a target.
 */
val OriginOfTheAvengers = card("Origin of the Avengers") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Scry 2.\n" +
        "II — You may put a Hero creature card with mana value 3 or less from your hand onto the " +
        "battlefield. If you don't, draw a card.\n" +
        "III — Put a +1/+1 counter on each creature you control."

    sagaChapter(1) {
        effect = Effects.Scry(2)
    }

    sagaChapter(2) {
        effect = IfYouDoEffect(
            action = Patterns.Hand.putFromHand(
                filter = GameObjectFilter.Creature.withSubtype(Subtype.HERO).manaValueAtMost(3),
                count = 1,
                prompt = "You may put a Hero creature card with mana value 3 or less onto the battlefield",
            ),
            ifYouDo = Effects.Composite(),
            ifYouDont = Effects.DrawCards(1),
        )
    }

    sagaChapter(3) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Serena Malyon"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b0701e8-e365-425a-b897-92f4df9edcb8.jpg?1784182931"
    }
}
