package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Solemn Recruit — Aether Revolt #22
 * {1}{W}{W}
 * Creature — Dwarf Warrior
 * 2/2
 *
 * Double strike
 * Revolt — At the beginning of your end step, if a permanent left the battlefield under your
 * control this turn, put a +1/+1 counter on this creature.
 *
 * Revolt is a CR 603.4 intervening-if, riding on `interveningIf` with the per-player
 * [Conditions.YouHadPermanentLeaveBattlefieldThisTurn] tracker (same shape as Call for Unity).
 *
 * Aether Revolt is the earliest printing; the C20 and CLB reprints are `Printing` rows in those sets.
 */
val SolemnRecruit = card("Solemn Recruit") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Warrior"
    power = 2
    toughness = 2
    oracleText = "Double strike\n" +
        "Revolt — At the beginning of your end step, if a permanent left the battlefield under your " +
        "control this turn, put a +1/+1 counter on this creature."

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Revolt — At the beginning of your end step, if a permanent left the battlefield " +
            "under your control this turn, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Eric Deschamps"
        flavorText = "\"I can no longer sit in my workshop and ignore the events outside. My hammers strike for the people of Ghirapur.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/9/5990c2f1-94d7-4d2e-b1ad-6406c25b91aa.jpg?1788982109"
        ruling("2017-02-09", "Revolt abilities check only whether a permanent you controlled left the battlefield this turn or not. They don't apply multiple times if more than one permanent you controlled left the battlefield. They don't check whether the permanent that left the battlefield is still in the zone it moved to.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. They're equally satisfied by an artifact you sacrificed to pay a cost, a creature you controlled that was destroyed by Murder, or an enchantment you returned to your hand with Leave in the Dust.")
        ruling("2017-02-09", "Tokens that leave the battlefield will satisfy a revolt ability.")
        ruling("2017-02-09", "Energy counters aren't permanents. Paying {E} won't satisfy a revolt ability.")
        ruling("2017-02-09", "All cards in the Aether Revolt set with triggered revolt abilities use an intervening \"if\" clause. A permanent you controlled must have left the battlefield earlier in the turn in order for these abilities to trigger; otherwise they do nothing. In other words, there's no way to have the ability trigger if no permanent you controlled has left the battlefield that turn, even if you intend to have one do so in response to the triggered ability.")
    }
}
