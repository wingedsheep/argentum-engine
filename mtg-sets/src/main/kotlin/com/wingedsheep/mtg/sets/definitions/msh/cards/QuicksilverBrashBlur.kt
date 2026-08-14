package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Quicksilver, Brash Blur — Marvel Super Heroes #148 (rare)
 * {R} · Legendary Creature — Mutant Hero · 1/1
 *
 * If Quicksilver, Brash Blur is in your opening hand, you may begin the game with him on the
 * battlefield.
 * Haste
 * Power-up — {4}{R}: Put a +1/+1 counter and a double strike counter on Quicksilver. (Activate
 * each power-up ability only once. Reduce the cost by his mana cost if he entered this turn.)
 *
 * Note the interaction the opening-hand clause creates with power-up: a Quicksilver who *starts*
 * on the battlefield never "entered this turn" on any turn thereafter, so he gets **no** discount
 * — `{4}{R}` in full. Cast him from hand instead and the same ability costs `{4}` that turn.
 * That falls out of the rules rather than needing a special case here: the discount reads the
 * engine's entered-this-turn marker, which a game-start permanent is never stamped with.
 *
 * Both counters are keyword/stat counters placed by the same [Effects.Composite]; double strike
 * is [Counters.DOUBLE_STRIKE] (CR 122.1d) rather than a granted keyword.
 */
val QuicksilverBrashBlur = card("Quicksilver, Brash Blur") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Mutant Hero"
    oracleText = "If Quicksilver, Brash Blur is in your opening hand, you may begin the game with " +
        "him on the battlefield.\n" +
        "Haste\n" +
        "Power-up — {4}{R}: Put a +1/+1 counter and a double strike counter on Quicksilver. " +
        "(Activate each power-up ability only once. Reduce the cost by his mana cost if he " +
        "entered this turn.)"
    power = 1
    toughness = 1

    mayBeginGameOnBattlefield()

    keywords(Keyword.HASTE)

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{4}{R}")
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.AddCounters(Counters.DOUBLE_STRIKE, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Michael MacRae"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d5819ca-165d-4f4c-9500-3ac206994880.jpg?1783902925"
    }
}
