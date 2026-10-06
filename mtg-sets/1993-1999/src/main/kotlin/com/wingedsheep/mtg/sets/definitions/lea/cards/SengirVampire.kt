package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sengir Vampire — Limited Edition Alpha #127
 * {3}{B}{B} · Creature — Vampire · 4/4
 *
 * Flying
 * Whenever a creature dealt damage by this creature this turn dies, put a +1/+1 counter on this
 * creature.
 *
 * `Triggers.self.damagedCreatureDies()` is the dealt-damage-dies trigger (Soul Collector, Dread
 * Slaver). It looks back in time, so a combat trade still triggers; the counter then has nothing to
 * land on, matching the ruling that the ability "will do nothing when it resolves".
 */
val SengirVampire = card("Sengir Vampire") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    oracleText = "Flying (This creature can't be blocked except by creatures with flying or reach.)\n" +
        "Whenever a creature dealt damage by this creature this turn dies, put a +1/+1 counter on " +
        "this creature."
    power = 4
    toughness = 4
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.damagedCreatureDies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/510840f4-7c0e-4b47-8ebf-23c20cac4bd9.jpg?1783948692"
        ruling(
            "2011-09-22",
            "Each time a creature is put into a graveyard from the battlefield, check whether Sengir " +
                "Vampire had dealt any damage to it at any time during that turn. If so, Sengir Vampire's " +
                "ability will trigger. It doesn't matter who controlled the creature or whose graveyard it " +
                "was put into."
        )
        ruling(
            "2011-09-22",
            "If Sengir Vampire and a creature it dealt damage to are both put into a graveyard at the " +
                "same time, Sengir Vampire's ability will trigger, but it will do nothing when it resolves."
        )
        ruling(
            "2007-07-15",
            "If Sengir Vampire deals nonlethal damage to a creature and then a different effect or damage " +
                "source causes that creature to be put into a graveyard later in the turn, Sengir Vampire's " +
                "ability will trigger and it will get a +1/+1 counter."
        )
    }
}
