package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Summon: Magus Sisters — Final Fantasy Commander #71
 * {4}{G} · Enchantment Creature — Saga Faerie · 5/5
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II, III — Choose one at random —
 * • Combine Powers! — Put three +1/+1 counters on target creature.
 * • Defense! — Put a shield counter on target creature. You gain 3 life.
 * • Fight! — This creature fights up to one target creature an opponent controls.
 * Haste
 *
 * - `ModalEffect.chooseOneAtRandom`: the engine draws the mode as each chapter ability goes onto
 *   the stack, before targets, from the modes that have a legal target. "Fight!" targets "up to
 *   one", so it is always in the draw (ruling).
 */
private fun magusSistersChapter() = ModalEffect.chooseOneAtRandom(
    mode("Combine Powers! — Put three +1/+1 counters on target creature") {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, creature)
    },
    mode("Defense! — Put a shield counter on target creature. You gain 3 life") {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.SHIELD, 1, creature) then Effects.GainLife(3)
    },
    mode("Fight! — This creature fights up to one target creature an opponent controls") {
        val foe = target(TargetFilter.CreatureOpponentControls, optional = true)
        effect = Effects.Fight(EffectTarget.Self, foe)
    },
)

val SummonMagusSisters = card("Summon: Magus Sisters") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Saga Faerie"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II, III — Choose one at random —\n" +
        "• Combine Powers! — Put three +1/+1 counters on target creature.\n" +
        "• Defense! — Put a shield counter on target creature. You gain 3 life.\n" +
        "• Fight! — This creature fights up to one target creature an opponent controls.\n" +
        "Haste"
    power = 5
    toughness = 5

    keywords(Keyword.HASTE)

    for (chapter in 1..3) {
        sagaChapter(chapter) { effect = magusSistersChapter() }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Rui Maruyama"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76a8cae7-e6ea-45eb-be8d-0e6a29b98aa6.jpg?1783906350"
        ruling(
            "2025-06-06",
            "As you put one of Summon: Magus Sisters's chapter abilities on the stack, you choose a mode " +
                "at random. Players can respond to the ability knowing which mode was chosen."
        )
        ruling(
            "2025-06-06",
            "In the incredibly rare case where there is no legal target for the first or second mode, " +
                "those modes can't be chosen at random. Since the third mode doesn't require a target, it " +
                "is always eligible to be chosen."
        )
        ruling(
            "2025-06-06",
            "The random choice of mode for Summon: Magus Sisters's chapter abilities is made before " +
                "targets are chosen."
        )
    }
}
