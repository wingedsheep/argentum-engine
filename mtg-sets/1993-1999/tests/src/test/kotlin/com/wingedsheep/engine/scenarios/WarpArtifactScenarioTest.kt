package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Warp Artifact.
 *
 * Enchant artifact. At the beginning of the upkeep of enchanted artifact's controller, this Aura
 * deals 1 damage to that player.
 *
 * Put on an opponent's artifact, the trigger fires on *their* upkeep and hurts *them*, while the
 * Aura's controller still controls the ability — and it does not fire on the Aura controller's own upkeep.
 */
class WarpArtifactScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun attach(driver: GameTestDriver, auraId: EntityId, hostId: EntityId) {
        driver.addComponent(auraId, AttachedToComponent(hostId))
        val existing = driver.state.getEntity(hostId)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        driver.addComponent(hostId, AttachmentsComponent(existing + auraId))
    }

    test("fires on the enchanted artifact's controller's upkeep, controlled by the Aura's controller") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val victim = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val fountain = driver.putPermanentOnBattlefield(victim, "Fountain of Youth")
        val warpArtifact = driver.putPermanentOnBattlefield(me, "Warp Artifact")
        attach(driver, warpArtifact, fountain)

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe victim

        withClue("the trigger is on the stack, and it is mine, not the victim's") {
            driver.state.stack.size shouldBe 1
            val trigger = driver.state.getEntity(driver.state.stack.last())
                ?.get<TriggeredAbilityOnStackComponent>()
            trigger?.controllerId shouldBe me
        }

        driver.bothPass()

        withClue("that player — the artifact's controller — takes the 1 damage") {
            driver.getLifeTotal(victim) shouldBe 19
            driver.getLifeTotal(me) shouldBe 20
        }
    }

    test("does not fire on the Aura controller's own upkeep") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val victim = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val fountain = driver.putPermanentOnBattlefield(victim, "Fountain of Youth")
        val warpArtifact = driver.putPermanentOnBattlefield(me, "Warp Artifact")
        attach(driver, warpArtifact, fountain)

        // Through the victim's turn (taking the hit) and into my next upkeep.
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.bothPass()
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe me

        withClue("my upkeep: nothing triggers, nobody else is hurt") {
            driver.state.stack.size shouldBe 0
            driver.getLifeTotal(me) shouldBe 20
            driver.getLifeTotal(victim) shouldBe 19
        }
    }
})
