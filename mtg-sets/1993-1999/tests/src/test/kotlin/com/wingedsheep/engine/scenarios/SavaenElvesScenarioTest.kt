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
 * Scenario tests for Savaen Elves — "{G}{G}, {T}: Destroy target Aura attached to a land."
 *
 * The thing worth proving is which permanent dies. The clause reads as though it were about the
 * land, and the neighbouring Devout Harpist (ULG) is scripted to destroy the *creature* its Aura is
 * attached to rather than the Aura, so these assert both halves: the Aura goes to the graveyard and
 * the land it enchanted is untouched.
 */
class SavaenElvesScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("destroys the Aura attached to a land, and leaves the land alone") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        val elves = driver.putCreatureOnBattlefield(active, "Savaen Elves")
        driver.removeSummoningSickness(elves)
        // Two Forests to pay {G}{G} — the Elves' own tap is the rest of the cost.
        repeat(2) { driver.putLandOnBattlefield(active, "Forest") }

        val enchantedLand = driver.putLandOnBattlefield(active, "Forest")
        val aura = driver.putPermanentOnBattlefield(active, "Fishliver Oil")
        driver.replaceState(
            driver.state.updateEntity(aura) { c -> c.with(AttachedToComponent(enchantedLand)) }
        )

        val abilityId = driver.cardRegistry.getCard("Savaen Elves")!!
            .script.activatedAbilities.single().id
        val result = driver.submit(
            ActivateAbility(
                playerId = active,
                sourceId = elves,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(aura)),
            )
        )

        result.error shouldBe null
        driver.bothPass()

        withClue("the Aura is what gets destroyed") {
            driver.state.getBattlefield(active).contains(aura) shouldBe false
        }
        withClue("the land it enchanted is not a target and must survive") {
            driver.state.getEntity(enchantedLand) shouldNotBe null
            driver.state.getBattlefield(active).contains(enchantedLand) shouldBe true
        }
    }

    test("an Aura attached to a creature is not a legal target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        val elves = driver.putCreatureOnBattlefield(active, "Savaen Elves")
        driver.removeSummoningSickness(elves)
        repeat(2) { driver.putLandOnBattlefield(active, "Forest") }

        val bear = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        val aura = driver.putPermanentOnBattlefield(active, "Fishliver Oil")
        driver.replaceState(
            driver.state.updateEntity(aura) { c -> c.with(AttachedToComponent(bear)) }
        )

        val abilityId = driver.cardRegistry.getCard("Savaen Elves")!!
            .script.activatedAbilities.single().id
        val result = driver.submit(
            ActivateAbility(
                playerId = active,
                sourceId = elves,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(aura)),
            )
        )

        withClue("\"attached to a land\" excludes an Aura on a creature") {
            result.error shouldNotBe null
        }
    }
})
