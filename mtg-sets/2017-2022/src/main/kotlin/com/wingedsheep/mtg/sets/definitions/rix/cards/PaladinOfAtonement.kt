package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Paladin of Atonement
 * {1}{W}
 * Creature — Vampire Knight
 * 1/1
 *
 * At the beginning of each upkeep, if you lost life last turn, put a +1/+1 counter on this creature.
 * When this creature dies, you gain life equal to its toughness.
 *
 * "Last turn" is the previous turn in the game, whoever's it was, and only *losing* life counts —
 * Feast on the Fallen's `TurnTracker.LIFE_LOST_LAST_TURN` read for you rather than an opponent.
 * The dies trigger's `sourceToughness()` falls back to the battlefield-exit snapshot, so it is the
 * toughness the Paladin last had on the battlefield; a toughness below 0 gains nothing.
 */
val PaladinOfAtonement = card("Paladin of Atonement") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Vampire Knight"
    power = 1
    toughness = 1
    oracleText = "At the beginning of each upkeep, if you lost life last turn, put a +1/+1 counter on this " +
        "creature.\nWhen this creature dies, you gain life equal to its toughness."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.YouLostLifeLastTurn
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.GainLife(DynamicAmounts.sourceToughness())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Lius Lasahido"
        flavorText = "For every wound there is a blessing; for every death, a reckoning."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0ad263e-7ff0-459b-b04b-1d03b09c48aa.jpg?1783935335"
        ruling(
            "2018-01-19",
            "Paladin of Atonement's first ability cares only whether you lost life last turn, even if Paladin " +
                "of Atonement wasn't on the battlefield when that happened. It doesn't care how much you lost, " +
                "whether you also gained life, or even whether you gained more life than you lost."
        )
        ruling(
            "2018-01-19",
            "To determine how much life you gain for the last ability, use Paladin of Atonement's toughness as " +
                "it last existed on the battlefield. If its toughness was less than 0, you won't gain life. " +
                "(You also won't lose life.)"
        )
    }
}
