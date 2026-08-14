package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sabotage Strategist — Aetherdrift #59
 * {2}{U}{U} · Creature — Vedalken Ranger · 2/2
 *
 * Flying, vigilance
 * Whenever one or more creatures attack you, those creatures get -1/-0 until end of turn.
 * Exhaust — {5}{U}{U}: Put three +1/+1 counters on this creature.
 *
 * [Triggers.CreaturesAttackYou] already applies the CR 509.1b scoping the trigger needs: it only
 * fires for attackers declared against the controller themself, not against a planeswalker they
 * control. "Those creatures" is then resolved the way the rest of the codebase models "creatures
 * attacking you" (Blessed Reversal, `DynamicAmounts.creaturesAttackingYou`) — attacking creatures
 * an opponent controls, evaluated at resolution. That's exact in a two-player game; in multiplayer
 * it would also catch attackers aimed at a different player, which the SDK has no
 * defender-scoped group filter to express yet.
 *
 * The exhaust ability is a plain [activatedAbility] with `isExhaust = true`; the DSL adds the
 * once-per-object activation restriction (CR 702.177a) and the "Exhaust — " rendering.
 */
val SabotageStrategist = card("Sabotage Strategist") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Ranger"
    oracleText = "Flying, vigilance\n" +
        "Whenever one or more creatures attack you, those creatures get -1/-0 until end of turn.\n" +
        "Exhaust — {5}{U}{U}: Put three +1/+1 counters on this creature. " +
        "(Activate each exhaust ability only once.)"
    power = 2
    toughness = 2

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.CreaturesAttackYou
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.attacking().opponentControls()),
            Effects.ModifyStats(-1, 0, EffectTarget.Self)
        )
        description = "Whenever one or more creatures attack you, those creatures get -1/-0 until " +
            "end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{5}{U}{U}")
        isExhaust = true
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Darren Tan"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8bb15e2-e1ad-4645-aab0-df4a1a68563d.jpg?1783907905"
        ruling("2025-02-07", "Exhaust abilities can be activated any time you could normally activate an ability.")
        ruling(
            "2025-02-07",
            "If an exhaust ability of a permanent is activated, and then that permanent leaves the " +
                "battlefield and returns to the battlefield, it becomes a new object so its exhaust " +
                "ability can be activated again."
        )
    }
}
