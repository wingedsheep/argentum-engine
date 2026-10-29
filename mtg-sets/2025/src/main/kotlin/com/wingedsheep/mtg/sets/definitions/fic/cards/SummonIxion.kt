package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Summon: Ixion — Final Fantasy Commander #27
 * {2}{W} · Enchantment Creature — Saga Unicorn · 3/3
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Aerospark — Exile target creature an opponent controls until this Saga leaves the battlefield.
 * II, III — Put a +1/+1 counter on each of up to two target creatures you control. You gain 2 life.
 * First strike
 *
 * - Chapter I is In the Trenches' "until this leaves the battlefield" exile
 *   ([Effects.MoveUntilSourceLeaves]): the return is part of the one-shot effect, not a leaves
 *   trigger, and nothing is exiled if the Saga is already gone when the chapter resolves. The
 *   Saga is sacrificed after chapter III, so the creature comes back then at the latest.
 * - Chapters II and III are one optional two-slot declaration fanned out by
 *   [Effects.ForEachTarget], then the life gain; choosing no targets still gains 2 life.
 */
val SummonIxion = card("Summon: Ixion") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Saga Unicorn"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Aerospark — Exile target creature an opponent controls until this Saga leaves the battlefield.\n" +
        "II, III — Put a +1/+1 counter on each of up to two target creatures you control. You gain 2 life.\n" +
        "First strike"
    power = 3
    toughness = 3

    keywords(Keyword.FIRST_STRIKE)

    sagaChapter(1) {
        val victim = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.MoveUntilSourceLeaves(victim, Zone.EXILE)
    }

    for (chapter in 2..3) {
        sagaChapter(chapter) {
            targets(TargetFilter.CreatureYouControl, count = 2, optional = true)
            effect = Effects.ForEachTarget(
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
            ) then Effects.GainLife(2)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Ben Wootten"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acb35dcb-0bee-4deb-88bf-ffa1f110c67b.jpg?1783906365"
        ruling(
            "2025-06-06",
            "If Summon: Ixion leaves the battlefield before its first chapter ability resolves, the target " +
                "permanent won't be exiled."
        )
        ruling("2025-06-06", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling(
            "2025-06-06",
            "You don't have to choose any targets for Summon: Ixion's second or third chapter ability. However, " +
                "if you do and all of the targets are illegal when the ability tries to resolve, it won't " +
                "resolve and none of its effects will happen. You won't gain life."
        )
    }
}
