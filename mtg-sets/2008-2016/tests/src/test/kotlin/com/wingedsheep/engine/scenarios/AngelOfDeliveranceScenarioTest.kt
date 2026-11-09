package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Angel of Deliverance (SOI #2) — {6}{W}{W} 6/6 Angel with flying.
 *
 *   Delirium — Whenever this creature deals damage, if there are four or more card types among
 *   cards in your graveyard, exile target creature an opponent controls.
 */
class AngelOfDeliveranceScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(driver, player, driver.getOpponent(player))
    }

    /** Attack the opponent with [angel] and play combat out, aiming any trigger at [target]. */
    fun attack(driver: GameTestDriver, player: EntityId, opponent: EntityId, angel: EntityId, target: EntityId) {
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(angel), opponent)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        var guard = 0
        while (guard++ < 30) {
            when {
                driver.pendingDecision is ChooseTargetsDecision ->
                    driver.submitTargetSelection(player, listOf(target)).error shouldBe null
                driver.pendingDecision != null -> driver.autoResolveDecision()
                driver.state.stack.isNotEmpty() -> driver.bothPass()
                else -> break
            }
        }
    }

    test("with delirium, dealing damage exiles target creature an opponent controls") {
        val (driver, player, opponent) = newGame()
        val angel = driver.putCreatureOnBattlefield(player, "Angel of Deliverance")
        driver.removeSummoningSickness(angel)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.putCardInGraveyard(player, "Grizzly Bears")   // creature
        driver.putCardInGraveyard(player, "Lightning Bolt")  // instant
        driver.putCardInGraveyard(player, "Lava Spike")      // sorcery
        driver.putCardInGraveyard(player, "Ornithopter")     // artifact

        attack(driver, player, opponent, angel, bears)

        driver.getLifeTotal(opponent) shouldBe 14
        driver.findPermanent(opponent, "Grizzly Bears").shouldBeNull()
        driver.getExile(opponent) shouldContain bears
    }

    test("without delirium, dealing damage exiles nothing") {
        val (driver, player, opponent) = newGame()
        val angel = driver.putCreatureOnBattlefield(player, "Angel of Deliverance")
        driver.removeSummoningSickness(angel)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.putCardInGraveyard(player, "Grizzly Bears")   // creature
        driver.putCardInGraveyard(player, "Lightning Bolt")  // instant
        driver.putCardInGraveyard(player, "Lava Spike")      // sorcery

        attack(driver, player, opponent, angel, bears)

        driver.getLifeTotal(opponent) shouldBe 14
        driver.findPermanent(opponent, "Grizzly Bears").shouldNotBeNull()
    }
})
