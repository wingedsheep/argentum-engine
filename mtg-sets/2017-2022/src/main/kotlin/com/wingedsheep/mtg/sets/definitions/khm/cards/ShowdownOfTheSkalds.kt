package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Showdown of the Skalds — Kaldheim #229
 * {2}{R}{W} · Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Exile the top four cards of your library. Until the end of your next turn, you may play those cards.
 * II, III — Whenever you cast a spell this turn, put a +1/+1 counter on target creature you control.
 *
 * Chapter I is the gather → exile → grant-play pipeline with [MayPlayExpiry.UntilEndOfNextTurn]; unplayed
 * cards stay exiled. Chapters II and III each install a turn-bounded delayed trigger (Rediscover the Way's
 * shape) that fires on every spell you cast this turn and picks its target each time it fires.
 */
val ShowdownOfTheSkalds = card("Showdown of the Skalds") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Exile the top four cards of your library. Until the end of your next turn, you may play those cards.\n" +
        "II, III — Whenever you cast a spell this turn, put a +1/+1 counter on target creature you control."

    sagaChapter(1) {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(4))
            exile(exiled)
            run(Effects.GrantMayPlayFromExile(from = exiled, expiry = MayPlayExpiry.UntilEndOfNextTurn))
        }
    }

    for (chapter in 2..3) {
        sagaChapter(chapter) {
            effect = Effects.CreateDelayedTrigger(
                trigger = Triggers.you.casts(GameObjectFilter.Any),
                fireOnce = false,
                expiry = DelayedTriggerExpiry.EndOfTurn
            ) {
                val creatureYouControl = target(TargetFilter.CreatureYouControl)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creatureYouControl)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d9d840e-1f13-44e3-a4de-903cfa58a346.jpg?1783928190"
        ruling("2021-02-05", "The cards you exile because of the chapter I ability are exiled face up.")
        ruling(
            "2021-02-05",
            "Playing a card this way follows the normal rules for playing the card. You must pay its costs, " +
                "and you must follow all applicable timing rules."
        )
        ruling(
            "2021-02-05",
            "Under normal circumstances, you can play a land from among the exiled cards only if you haven't " +
                "played a land yet that turn."
        )
        ruling("2021-02-05", "Any cards you don't play by the end of your next turn will remain exiled.")
    }
}
