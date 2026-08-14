package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Crowd-Control Warden — Murders at Karlov Manor #193
 * {3}{G}{W} · Creature — Centaur Soldier · 4/4
 *
 * As this creature enters or is turned face up, put X +1/+1 counters on it, where X is the number
 * of other creatures you control.
 * Disguise {3}{G/W}{G/W}
 *
 * The two halves are two *different* rules constructs that happen to share a sentence, and each
 * needs its own count.
 *
 * **Entering** is a CR 614.1c enters-with-counters replacement ([EntersWithDynamicCounters]). The
 * Warden isn't on the battlefield yet when the count is evaluated, so a plain "creatures you
 * control" tally is already "other creatures you control" — the same evaluation timing that makes
 * Sheriff of Safe Passage work. That is also why the official ruling says creatures entering
 * *simultaneously* with the Warden don't count: they aren't there yet either.
 *
 * **Turning face up** is a replacement riding the turn-up special action, not a triggered ability —
 * `disguiseFaceUpEffect`, the same slot [BubbleSmuggler] uses. It doesn't use the stack, so an
 * opponent never gets a window against the 2/2 body before the counters land. Here the Warden *is*
 * on the battlefield when the count runs, so this half must ask for `excludeSelf = true`; without
 * it the Warden would count itself and arrive one counter too big.
 *
 * Only one half ever applies to a given copy: cast for {3}{G}{W} it enters face up and takes the
 * replacement; cast face down for {3} it enters as a nameless 2/2 (no abilities at all, CR 708.2)
 * and only the flip pays out.
 */
val CrowdControlWarden = card("Crowd-Control Warden") {
    manaCost = "{3}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Centaur Soldier"
    oracleText = "As this creature enters or is turned face up, put X +1/+1 counters on it, where " +
        "X is the number of other creatures you control.\n" +
        "Disguise {3}{G/W}{G/W} (You may cast this card face down for {3} as a 2/2 creature with " +
        "ward {2}. Turn it face up any time for its disguise cost.)"
    power = 4
    toughness = 4

    // Entering: the Warden is not yet on the battlefield, so "creatures you control" is already
    // "other creatures you control".
    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature)
        )
    )

    disguise = "{3}{G/W}{G/W}"
    // Turning face up: the Warden IS on the battlefield, so it must be excluded from its own count.
    disguiseFaceUpEffect = Effects.AddDynamicCounters(
        Counters.PLUS_ONE_PLUS_ONE,
        DynamicAmount.AggregateBattlefield(
            Player.You,
            GameObjectFilter.Creature,
            excludeSelf = true
        ),
        EffectTarget.Self
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "193"
        artist = "Diego Gisbert"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdf0578f-4966-4ecd-81e1-83ae13126f13.jpg?1783912856"

        ruling(
            "2024-02-02",
            "If Crowd-Control Warden enters the battlefield at the same time as one or more " +
                "creatures, those creatures won't count for the purposes of Crowd-Control Warden's " +
                "first ability."
        )
        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to. Only a face-down permanent can " +
                "be turned face up this way; a face-down spell cannot."
        )
        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face " +
                "up, turning a permanent face up doesn't cause any enters-the-battlefield " +
                "abilities to trigger."
        )
    }
}
