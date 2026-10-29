package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Summon: Kujata — Final Fantasy Commander #61
 * {5}{R} · Enchantment Creature — Saga Ox · 7/5
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Lightning — This creature deals 3 damage to each of up to two target creatures.
 * II — Ice — Up to three target creatures can't block this turn.
 * III — Fire — Discard a card, then draw two cards. When you discard a card this way, this
 *       creature deals damage equal to that card's mana value to each opponent.
 * Trample, haste
 *
 * - Chapters I and II are one "up to N target creatures" declaration fanned out by
 *   [Effects.ForEachTarget] (Unearthly Blizzard's shape for II).
 * - Chapter III is the discard → reflexive-trigger shape of The Ancient One with the draw moved
 *   after it: the discard happens, then the two cards are drawn, and the reflexive "when you do"
 *   goes on the stack afterwards carrying the discarded card, whose mana value it reads. With an
 *   empty hand nothing is discarded and the reflexive never triggers. By the time the reflexive
 *   resolves the Saga has been sacrificed (chapter III is its last), so the damage is dealt by it
 *   as it last existed on the battlefield.
 */
val SummonKujata = card("Summon: Kujata") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Saga Ox"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Lightning — This creature deals 3 damage to each of up to two target creatures.\n" +
        "II — Ice — Up to three target creatures can't block this turn.\n" +
        "III — Fire — Discard a card, then draw two cards. When you discard a card this way, this " +
        "creature deals damage equal to that card's mana value to each opponent.\n" +
        "Trample, haste"
    power = 7
    toughness = 5

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    sagaChapter(1) {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.DealDamage(3, EffectTarget.ContextTarget(0))
        )
    }

    sagaChapter(2) {
        targets(TargetFilter.Creature, count = 3, optional = true)
        effect = Effects.ForEachTarget(
            Effects.CantBlock(target = EffectTarget.ContextTarget(0))
        )
    }

    sagaChapter(3) {
        effect = Effects.ReflexiveTrigger(
            action = Patterns.Hand.discardCards(1),
            optional = false,
        ) {
            effect = Effects.DealDamage(
                DynamicAmounts.manaValueOf(Patterns.Hand.discarded),
                EffectTarget.PlayerRef(Player.EachOpponent),
            )
        } then Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Alexandre Honoré"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba934866-efd0-4126-b1ff-9c7e82b73bd6.jpg?1783906353"
        ruling(
            "2025-06-06",
            "If the discarded card has {X} in its mana cost, X is 0 for the purpose of determining its mana value."
        )
    }
}
