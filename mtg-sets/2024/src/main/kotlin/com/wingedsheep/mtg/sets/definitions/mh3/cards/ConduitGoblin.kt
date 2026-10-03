package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Conduit Goblin
 * {R}{W}
 * Creature — Goblin Warrior
 * 2/2
 *
 * When this creature enters, you get {E}{E} (two energy counters).
 * At the beginning of combat on your turn, you may pay {E}. If you do, another target creature you
 * control gets +1/+0 and gains haste until end of turn.
 *
 * Unlike Guide of Souls' "when you do", this is an "if you do": the target is chosen as the trigger
 * goes on the stack, and the {E} payment is offered on resolution via [Effects.MayPay] — only when
 * the player actually has an energy counter to pay.
 */
val ConduitGoblin = card("Conduit Goblin") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Goblin Warrior"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "At the beginning of combat on your turn, you may pay {E}. If you do, another target creature " +
        "you control gets +1/+0 and gains haste until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 1),
            then = Effects.ModifyStats(1, 0, creature) then Effects.GrantKeyword(Keyword.HASTE, creature),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Bruno Biazotto"
        flavorText = "Warned against wearing metal in a lightning storm, Smurt took it as a challenge."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c9ad04d-c4d4-4d06-93bb-a881be733717.jpg?1783911254"
    }
}
