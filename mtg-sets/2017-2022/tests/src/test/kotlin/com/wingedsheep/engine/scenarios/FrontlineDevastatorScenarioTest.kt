package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.FrontlineDevastator
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Frontline Devastator (HOU #93) — {3}{R} 3/3.
 *
 *   Afflict 2 (Whenever this creature becomes blocked, defending player loses 2 life.)
 *   {1}{R}: This creature gets +1/+0 until end of turn.
 *
 * The printed card wires afflict through the keyword alone: blocked by two creatures, the
 * defending player loses 2 life once; the firebreathing pump works alongside it.
 */
class FrontlineDevastatorScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(FrontlineDevastator))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
    }

    test("becoming blocked makes the defending player lose 2 life, once") {
        val driver = driver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val devastator = driver.putCreatureOnBattlefield(me, "Frontline Devastator")
        driver.removeSummoningSickness(devastator)
        val bears = List(2) { driver.putCreatureOnBattlefield(opponent, "Grizzly Bears") }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(devastator), opponent).error shouldBe null
        driver.bothPass()
        driver.declareBlockers(opponent, bears.associateWith { listOf(devastator) }).error shouldBe null
        driver.state.stack.size shouldBe 1
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("{1}{R} pumps it +1/+0 until end of turn") {
        val driver = driver()
        val me = driver.activePlayer!!
        val devastator = driver.putCreatureOnBattlefield(me, "Frontline Devastator")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(me, Color.RED, 2)

        val abilityId = FrontlineDevastator.activatedAbilities.single().id
        driver.submit(ActivateAbility(playerId = me, sourceId = devastator, abilityId = abilityId)).error shouldBe null
        driver.bothPass()

        driver.state.projectedState.getPower(devastator) shouldBe 4
        driver.state.projectedState.getToughness(devastator) shouldBe 3
    }
})
