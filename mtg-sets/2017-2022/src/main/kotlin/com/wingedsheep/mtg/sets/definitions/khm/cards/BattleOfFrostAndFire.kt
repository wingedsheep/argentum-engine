package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Battle of Frost and Fire — Kaldheim #204
 * {3}{U}{R} · Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — This Saga deals 4 damage to each non-Giant creature and each planeswalker.
 * II — Scry 3.
 * III — Whenever you cast a spell with mana value 5 or greater this turn, draw two cards, then
 * discard a card.
 *
 * Chapter I sweeps one union filter, so a permanent that is both a creature and a planeswalker is
 * dealt 4 damage once, and a Giant planeswalker is still hit. Chapter III creates a delayed trigger
 * (Glimpse of Nature's shape) that fires on every qualifying cast until end of turn, independent of
 * the Saga, which has been sacrificed by then.
 */
val BattleOfFrostAndFire = card("Battle of Frost and Fire") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — This Saga deals 4 damage to each non-Giant creature and each planeswalker.\n" +
        "II — Scry 3.\n" +
        "III — Whenever you cast a spell with mana value 5 or greater this turn, draw two cards, then discard a card."

    sagaChapter(1) {
        effect = Patterns.Group.dealDamageToAll(
            4,
            GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype.GIANT) or GameObjectFilter.Planeswalker)
        )
    }

    sagaChapter(2) {
        effect = Effects.Scry(3)
    }

    sagaChapter(3) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.you.casts(GameObjectFilter.Any.manaValueAtLeast(5)),
            fireOnce = false,
            expiry = DelayedTriggerExpiry.EndOfTurn,
            effect = Effects.DrawCards(2) then Effects.Discard(1)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/620ed5b7-8874-40d0-9245-98876bdec153.jpg?1783928201"
        ruling(
            "2021-02-05",
            "The triggered ability created by the chapter III ability may trigger multiple times during the turn, " +
                "even though Battle of Frost and Fire will likely no longer be on the battlefield."
        )
    }
}
