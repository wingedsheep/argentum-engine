package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Miracle Worker — "{T}: Destroy target Aura attached to a creature you control."
 *
 * Two restrictions to hold at once: the Aura is what dies (not its host — see the note in
 * [SavaenElvesScenarioTest]), and the host must be a creature *you* control, which is the half a
 * plain "attached to a creature" filter would silently drop.
 */
class MiracleWorkerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("destroys an Aura on your own creature, and the creature survives") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        val worker = driver.putCreatureOnBattlefield(active, "Miracle Worker")
        driver.removeSummoningSickness(worker)

        val bear = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        val aura = driver.putPermanentOnBattlefield(active, "Fishliver Oil")
        driver.replaceState(
            driver.state.updateEntity(aura) { c -> c.with(AttachedToComponent(bear)) }
        )

        val abilityId = driver.cardRegistry.getCard("Miracle Worker")!!
            .script.activatedAbilities.single().id
        val result = driver.submit(
            ActivateAbility(
                playerId = active,
                sourceId = worker,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(aura)),
            )
        )

        result.error shouldBe null
        driver.bothPass()

        withClue("the Aura is destroyed") {
            driver.state.getBattlefield(active).contains(aura) shouldBe false
        }
        withClue("the enchanted creature is not a target and must survive") {
            driver.state.getBattlefield(active).contains(bear) shouldBe true
        }
    }

    test("an Aura on a creature you don't control is not a legal target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != active }

        val worker = driver.putCreatureOnBattlefield(active, "Miracle Worker")
        driver.removeSummoningSickness(worker)

        // The host belongs to the opponent — "a creature you control" must exclude it, even
        // though the Aura itself is on our side of the table.
        val theirBear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val aura = driver.putPermanentOnBattlefield(active, "Fishliver Oil")
        driver.replaceState(
            driver.state.updateEntity(aura) { c -> c.with(AttachedToComponent(theirBear)) }
        )

        val abilityId = driver.cardRegistry.getCard("Miracle Worker")!!
            .script.activatedAbilities.single().id
        val result = driver.submit(
            ActivateAbility(
                playerId = active,
                sourceId = worker,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(aura)),
            )
        )

        withClue("the host's controller is what the filter reads, not the Aura's") {
            result.error shouldNotBe null
        }
    }
})
