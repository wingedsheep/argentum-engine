package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.RakshasaGravecaller
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rakshasa Gravecaller (DTK #114) — {4}{B} 3/6 Demon.
 *
 * "Exploit. When this creature exploits a creature, create two 2/2 black Zombie creature tokens."
 *
 * Pins that exploiting makes exactly two 2/2 black Zombies and that declining makes none.
 */
class RakshasaGravecallerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RakshasaGravecaller))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20, "Grizzly Bears" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castGravecaller(driver: GameTestDriver, me: EntityId): EntityId {
        val card = driver.putCardInHand(me, "Rakshasa Gravecaller")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 4)
        driver.castSpell(me, card).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the creature spell -> exploit trigger goes on the stack
        driver.bothPass() // resolve the exploit trigger -> optional sacrifice decision
        return card
    }

    fun zombieTokens(driver: GameTestDriver, player: EntityId): List<EntityId> =
        driver.getCreatures(player).filter { driver.getCardName(it) == "Zombie Token" }

    test("exploiting a creature creates two 2/2 black Zombie tokens") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        castGravecaller(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitCardSelection(me, listOf(fodder))
        driver.bothPass() // resolve the reflexive token payoff

        driver.getGraveyard(me) shouldContain fodder
        val zombies = zombieTokens(driver, me)
        zombies.size shouldBe 2
        zombies.forEach { zombie ->
            driver.state.projectedState.getPower(zombie) shouldBe 2
            driver.state.projectedState.getToughness(zombie) shouldBe 2
            driver.state.projectedState.getColors(zombie) shouldBe setOf(Color.BLACK.name)
        }
    }

    test("declining the exploit sacrifice creates no tokens") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        castGravecaller(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        zombieTokens(driver, me).size shouldBe 0
    }
})
