package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nazar, the Velvet Fang — Foundations Jumpstart #43 (canonical printing)
 * {3}{B} · Legendary Creature — Vampire Warlock · 3/3
 *
 * Menace
 * Whenever you gain life, put a feeding counter on Nazar.
 * Whenever Nazar attacks, you may remove three feeding counters from it. If you do, you draw three
 * cards and you lose 3 life.
 *
 * Removing the counters is the cost of an "if you do" gate: `Gate.MayPay` offers it only while
 * Nazar carries all three, so with fewer (or once Nazar has left the battlefield) there is no
 * prompt and nothing is drawn or lost.
 */
val NazarTheVelvetFang = card("Nazar, the Velvet Fang") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Vampire Warlock"
    power = 3
    toughness = 3
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever you gain life, put a feeding counter on Nazar.\n" +
        "Whenever Nazar attacks, you may remove three feeding counters from it. If you do, you draw " +
        "three cards and you lose 3 life."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.AddCounters(CounterType.FEEDING, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = Effects.RemoveCounters(CounterType.FEEDING, 3, EffectTarget.Self),
            then = Effects.DrawCards(3) then Effects.LoseLife(3, EffectTarget.PlayerRef(Player.You))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "KiKi"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/743f4b46-c04d-4431-9510-bc84726d7fd2.jpg?1783908857"
        ruling(
            "2024-11-08",
            "Nazar's second ability triggers just once for each life-gaining event, whether it's 1 life from " +
                "Dazzling Angel or 4 life from Apothecary Stomper.",
        )
        ruling(
            "2024-11-08",
            "If Nazar isn't on the battlefield as its last ability resolves, you can't remove three feeding " +
                "counters from it, even if it had three or more feeding counters on it before it left the " +
                "battlefield. You won't draw cards or lose life.",
        )
    }
}
