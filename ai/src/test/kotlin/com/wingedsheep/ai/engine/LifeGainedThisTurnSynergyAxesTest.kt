package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.emn.cards.LoneRider
import com.wingedsheep.mtg.sets.definitions.m14.cards.AngelicAccord
import com.wingedsheep.mtg.sets.definitions.vow.cards.CourierBat
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

/**
 * "If you gained life this turn" / "if you gained 3 or more life this turn" as a trigger's
 * intervening-if (Lone Rider, Angelic Accord, Courier Bat) pays off what the lifegain axis feeds —
 * a lifelinker, a gain-life spell — so the deckbuilder and the in-game evaluator see the card's
 * fuel beside it, as they do for a "whenever you gain life" trigger.
 */
class LifeGainedThisTurnSynergyAxesTest : FunSpec({

    test("a trigger gated on the life gained this turn rewards the lifegain axis") {
        for (card in listOf(LoneRider, AngelicAccord, CourierBat)) {
            withClue(card.name) { SynergyAxes.of(card).rewards shouldContain SynergyAxis.LIFEGAIN }
        }
    }
})
