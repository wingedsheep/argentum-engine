package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Beckett Mariner, Impetuous Ensign
 * {1}{R}
 * Legendary Creature — Human Officer
 * 2/1
 * First strike
 * Reckless Behavior — Whenever Beckett Mariner becomes tapped, put a promotion counter on her. Then if
 * there are three or more promotion counters on her, remove those counters and she deals 2 damage to
 * each opponent.
 *
 *  - "Reckless Behavior" is an ability word with no rules meaning.
 *  - `becomesTapped()` is cause-agnostic: attacking, crewing or an opponent's tap effect all count.
 *  - The three-counter check is a plain "then if" read on resolution, not an intervening "if", so the
 *    counter this trigger adds counts toward it. "Those counters" are all her promotion counters.
 *  - She is the damage source, which is the trigger's source already.
 */
val BeckettMarinerImpetuousEnsign = card("Beckett Mariner, Impetuous Ensign") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Officer"
    power = 2
    toughness = 1
    oracleText = "First strike\n" +
        "Reckless Behavior — Whenever Beckett Mariner becomes tapped, put a promotion counter on her. Then if " +
        "there are three or more promotion counters on her, remove those counters and she deals 2 damage to " +
        "each opponent."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.AddCounters(CounterType.PROMOTION, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtLeast(CounterType.PROMOTION, 3),
                then = Effects.RemoveAllCountersOfType(CounterType.PROMOTION, EffectTarget.Self) then
                    Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)),
            )
        description = "Reckless Behavior — Whenever Beckett Mariner becomes tapped, put a promotion counter on " +
            "her. Then if there are three or more promotion counters on her, remove those counters and she deals " +
            "2 damage to each opponent."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Lucas Durham"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2001bbd-63ee-4a6e-8e01-3a91aaf1e3b9.jpg?1784065783"
    }
}
