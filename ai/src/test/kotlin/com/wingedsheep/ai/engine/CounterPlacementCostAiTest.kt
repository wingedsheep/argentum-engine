package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.LethalSting
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * "As an additional cost to cast this spell, put a -1/-1 counter on a creature you control"
 * (Lethal Sting) is a `PutCounters` selection cost: the AI fills
 * `AdditionalCostPayment.counterPlacementTargets` with the creature worth least to it, the same
 * price the sacrifice and tap costs use, rather than whichever creature the enumerator lists first.
 */
class CounterPlacementCostAiTest : FunSpec({
    val cards = TestCards.all + listOf(LethalSting)

    test("Lethal Sting's counter goes on the cheaper creature, and the cast names it") {
        val driver = GameTestDriver().apply {
            registerCards(cards)
            initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = driver.player1
        val giant = driver.putCreatureOnBattlefield(player, "Hill Giant")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(driver.player2, "Hill Giant")
        val sting = driver.putCardInHand(player, "Lethal Sting")
        driver.giveMana(player, Color.BLACK, 3)

        val registry = CardRegistry().apply { register(cards) }
        val cast = GameSimulator(registry).getLegalActions(driver.state, player)
            .first { (it.action as? CastSpell)?.cardId == sting && it.actionType == "CastSpell" }
        cast.additionalCostInfo!!.costType shouldBe "PutCounters"
        cast.additionalCostInfo!!.validCounterPlacementTargets shouldContainExactlyInAnyOrder listOf(giant, bears)

        val chosen = AIPlayer.create(registry, player)
            .chooseFrom(driver.state, listOf(cast)).action as CastSpell
        chosen.additionalCostPayment?.counterPlacementTargets shouldBe listOf(bears)
    }
})
