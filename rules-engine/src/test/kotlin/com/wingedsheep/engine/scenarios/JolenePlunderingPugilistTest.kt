package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.JolenePlunderingPugilist
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Jolene, Plundering Pugilist {1}{R}{G} — Human Mercenary 4/2.
 * "Whenever you attack with one or more creatures with power 4 or greater, create a Treasure token.
 *  {1}{R}, Sacrifice a Treasure: Jolene deals 1 damage to any target."
 */
class JolenePlunderingPugilistTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(JolenePlunderingPugilist, PredefinedTokens.Treasure))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30, "Forest" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.treasures(player: EntityId): List<EntityId> =
        getPermanents(player).filter { getCardName(it) == "Treasure" }

    test("attacking with a power-4 creature creates a Treasure") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val jolene = driver.putCreatureOnBattlefield(me, "Jolene, Plundering Pugilist")
        driver.removeSummoningSickness(jolene)

        driver.treasures(me).size shouldBe 0

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(jolene), opp)
        driver.bothPass() // resolve the attack trigger

        driver.treasures(me).size shouldBe 1
    }

    test("attacking with only a small creature does NOT create a Treasure") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        // A 1/1 — power < 4. Jolene is left at home.
        val lion = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        driver.removeSummoningSickness(lion)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(lion), opp)
        driver.bothPass()

        driver.treasures(me).size shouldBe 0
    }

    test("sacrifice a Treasure to deal 1 damage to any target (the opponent)") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val jolene = driver.putCreatureOnBattlefield(me, "Jolene, Plundering Pugilist")
        driver.removeSummoningSickness(jolene)
        val treasure = driver.putPermanentOnBattlefield(me, "Treasure")
        driver.giveMana(me, Color.RED, 1)
        driver.giveColorlessMana(me, 1)

        val oppLifeBefore = driver.getLifeTotal(opp)

        val abilityId = JolenePlunderingPugilist.activatedAbilities.first().id
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = jolene,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opp)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(treasure))
            )
        ).error shouldBe null
        driver.bothPass()

        // Treasure sacrificed, opponent took 1 damage.
        driver.treasures(me).size shouldBe 0
        driver.getLifeTotal(opp) shouldBe (oppLifeBefore - 1)
    }
})
