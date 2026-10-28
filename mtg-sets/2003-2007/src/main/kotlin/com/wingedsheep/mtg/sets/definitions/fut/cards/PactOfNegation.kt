package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pact of Negation — Future Sight #42 (canonical / earliest real printing, 2007)
 * {0} · Instant · blue color indicator
 *
 * Counter target spell.
 * At the beginning of your next upkeep, pay {3}{U}{U}. If you don't, you lose the game.
 *
 * Originally printed with a color-defining ability; Oracle now gives it a blue color indicator
 * (2013-06-07 ruling).
 *
 * The delayed trigger is a step trigger on your own upkeep (`fireOnPlayer = You`) from the next
 * turn on (`NEXT_TURN` — the current turn's upkeep has always already begun by the time an
 * instant resolves). It is scheduled by the spell's own resolution, so a Pact that's countered or
 * fizzles creates none, while one that resolves against an uncounterable spell still does (rulings).
 * On that upkeep `PayOrSuffer` offers {3}{U}{U}; declining or being unable to pay loses the game.
 */
val PactOfNegation = card("Pact of Negation") {
    manaCost = "{0}"
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell.\n" +
        "At the beginning of your next upkeep, pay {3}{U}{U}. If you don't, you lose the game."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell() then Effects.CreateDelayedTrigger(
            step = Step.UPKEEP,
            timing = DelayedTriggerTiming.NEXT_TURN,
            fireOnPlayer = EffectTarget.PlayerRef(Player.You),
            effect = Effects.PayOrSuffer(
                cost = Costs.pay.Mana("{3}{U}{U}"),
                suffer = Effects.LoseGame(),
                consequenceDescription = "lose the game",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Jason Chan"
        flavorText = "Those who expect betrayal at every turn are seldom disappointed."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cca467a2-a2b3-4bdf-9d60-62979f675347.jpg?1783943120"
        ruling(
            "2021-03-19",
            "If Pact of Negation is countered or otherwise doesn't resolve (perhaps because its target became " +
                "illegal), the delayed triggered ability that threatens you with a game loss won't trigger at the " +
                "beginning of your next upkeep."
        )
        ruling(
            "2021-03-19",
            "If Pact of Negation resolves with a legal target but fails to counter that spell (most likely " +
                "because that spell can't be countered), the delayed triggered ability will still trigger at the " +
                "beginning of your next upkeep and you will have to pay. Or else."
        )
        ruling(
            "2013-06-07",
            "Although originally printed with a characteristic-defining ability that defined its color, this " +
                "card now has a color indicator. This color indicator can't be affected by text-changing effects " +
                "(such as the one created by Crystal Spray), although color-changing effects can still overwrite it."
        )
    }
}
