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
 * Scenario tests for Devout Harpist (ULG) — "{T}: Destroy target Aura attached to a creature."
 *
 * The Aura is what dies, not its host, and the creature itself is never a legal target — the
 * card's earlier script destroyed "target creature", which made a one-drop Aura answer into
 * repeatable creature removal.
 */
class DevoutHarpistScenarioTest : FunSpec({

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

        val worker = driver.putCreatureOnBattlefield(active, "Devout Harpist")
        driver.removeSummoningSickness(worker)

        val bear = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        val aura = driver.putPermanentOnBattlefield(active, "Fishliver Oil")
        driver.replaceState(
            driver.state.updateEntity(aura) { c -> c.with(AttachedToComponent(bear)) }
        )

        val abilityId = driver.cardRegistry.getCard("Devout Harpist")!!
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

    test("an Aura on an opponent's creature is a legal target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != active }

        val harpist = driver.putCreatureOnBattlefield(active, "Devout Harpist")
        driver.removeSummoningSickness(harpist)
        val theirBear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val aura = driver.putPermanentOnBattlefield(opponent, "Fishliver Oil")
        driver.replaceState(
            driver.state.updateEntity(aura) { c -> c.with(AttachedToComponent(theirBear)) }
        )

        val abilityId = driver.cardRegistry.getCard("Devout Harpist")!!
            .script.activatedAbilities.single().id
        val result = driver.submit(
            ActivateAbility(
                playerId = active,
                sourceId = harpist,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(aura)),
            )
        )
        result.error shouldBe null
        driver.bothPass()

        withClue("any creature's Aura may be destroyed") {
            driver.state.getBattlefield(opponent).contains(aura) shouldBe false
            driver.state.getBattlefield(opponent).contains(theirBear) shouldBe true
        }
    }

    test("a creature is not a legal target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != active }

        val harpist = driver.putCreatureOnBattlefield(active, "Devout Harpist")
        driver.removeSummoningSickness(harpist)
        val theirBear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val abilityId = driver.cardRegistry.getCard("Devout Harpist")!!
            .script.activatedAbilities.single().id
        val result = driver.submit(
            ActivateAbility(
                playerId = active,
                sourceId = harpist,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(theirBear)),
            )
        )
        withClue("Devout Harpist destroys Auras, never creatures") {
            result.error shouldNotBe null
        }
    }
})
