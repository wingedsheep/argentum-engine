package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.Ornithopter
import com.wingedsheep.mtg.sets.definitions.rtr.cards.CentaurHealer
import com.wingedsheep.mtg.sets.definitions.rtr.cards.CivicSaber
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Civic Saber (RTR) — "Equipped creature gets +1/+0 for each of its colors."
 *
 * The static bonus reads the color count of the *equipped creature* through the Equipment's
 * attachment link, so it varies with whatever the Saber is attached to.
 */
class CivicSaberScenarioTest : FunSpec({

    val projector = StateProjector()

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CivicSaber, CentaurHealer, Ornithopter))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun drain(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 20 && (driver.state.stack.isNotEmpty() || driver.isPaused)) {
            if (driver.isPaused) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    fun equip(driver: GameTestDriver, player: EntityId, saber: EntityId, creature: EntityId) {
        driver.giveColorlessMana(player, 1)
        val equipId = CivicSaber.activatedAbilities.first { it.isEquipAbility }.id
        driver.submitSuccess(
            ActivateAbility(
                playerId = player,
                sourceId = saber,
                abilityId = equipId,
                targets = listOf(ChosenTarget.Permanent(creature)),
            )
        )
        drain(driver)
    }

    test("a two-color creature gets +2/+0") {
        val (driver, you) = newGame()
        val healer = driver.putCreatureOnBattlefield(you, "Centaur Healer")
        val saber = driver.putPermanentOnBattlefield(you, "Civic Saber")
        equip(driver, you, saber, healer)

        val projected = projector.project(driver.state)
        projected.getPower(healer) shouldBe 5
        projected.getToughness(healer) shouldBe 3
    }

    test("a mono-colored creature gets +1/+0, and the bonus follows the Saber when re-equipped") {
        val (driver, you) = newGame()
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val healer = driver.putCreatureOnBattlefield(you, "Centaur Healer")
        val saber = driver.putPermanentOnBattlefield(you, "Civic Saber")
        equip(driver, you, saber, courser)

        projector.project(driver.state).getPower(courser) shouldBe 4

        equip(driver, you, saber, healer)
        val projected = projector.project(driver.state)
        projected.getPower(courser) shouldBe 3
        projected.getPower(healer) shouldBe 5
    }

    test("a colorless creature gets +0/+0") {
        val (driver, you) = newGame()
        val thopter = driver.putCreatureOnBattlefield(you, "Ornithopter")
        val saber = driver.putPermanentOnBattlefield(you, "Civic Saber")
        equip(driver, you, saber, thopter)

        val projected = projector.project(driver.state)
        projected.getPower(thopter) shouldBe 0
        projected.getToughness(thopter) shouldBe 2
    }
})
