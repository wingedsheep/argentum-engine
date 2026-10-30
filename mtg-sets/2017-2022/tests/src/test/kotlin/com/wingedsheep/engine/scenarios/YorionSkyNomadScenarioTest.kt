package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.iko.cards.YorionSkyNomad
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Yorion, Sky Nomad — when it enters, exile any number of other nonland permanents you own and
 * control; return those cards at the beginning of the next end step.
 */
class YorionSkyNomadScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(YorionSkyNomad)
        driver.initMirrorMatch(Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("offers only other nonland permanents you own and control; chosen ones return at the next end step") {
        val driver = newDriver()
        val me = driver.player1
        val mine = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val keptHome = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val land = driver.putPermanentOnBattlefield(me, "Plains")
        // Controlled but not owned, and owned but not controlled — neither may be exiled (ruling).
        val stolen = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val lent = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.replaceState(
            driver.state
                .updateEntity(stolen) { it.with(ControllerComponent(me)) }
                .updateEntity(lent) { it.with(ControllerComponent(driver.player2)) }
        )

        val yorion = driver.putCardInHand(me, "Yorion, Sky Nomad")
        driver.giveMana(me, Color.WHITE, 5)
        driver.castSpell(me, yorion)
        driver.bothPass() // Yorion resolves; its enters trigger goes on the stack
        driver.bothPass() // trigger resolves

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContainExactlyInAnyOrder listOf(mine, keptHome)
        decision.options shouldNotContain yorion
        decision.options shouldNotContain land
        decision.minSelections shouldBe 0

        driver.submitCardSelection(me, listOf(mine))

        driver.getExile(me) shouldContain mine
        driver.getPermanents(me) shouldContain keptHome

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.getPermanents(me) shouldContain mine
        driver.getController(mine) shouldBe me
        driver.isTapped(mine) shouldBe false
    }

    test("choosing nothing exiles nothing") {
        val driver = newDriver()
        val me = driver.player1
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val yorion = driver.putCardInHand(me, "Yorion, Sky Nomad")
        driver.giveMana(me, Color.WHITE, 5)
        driver.castSpell(me, yorion)
        driver.bothPass()
        driver.bothPass()

        driver.submitCardSelection(me, emptyList())

        driver.getPermanents(me) shouldContain bears
        driver.getExile(me) shouldNotContain bears
    }
})
