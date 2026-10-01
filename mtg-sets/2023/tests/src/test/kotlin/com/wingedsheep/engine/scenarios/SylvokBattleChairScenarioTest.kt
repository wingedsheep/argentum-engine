package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.SylvokBattleChair
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sylvok Battle-Chair (ONE #184) — For Mirrodin! makes a 2/2 red Rebel and attaches to it; the
 * equipped creature gets +4/+4 and has trample. Equip {5}{G}{G}.
 */
class SylvokBattleChairScenarioTest : ScenarioTestBase() {

    private val equipId = SylvokBattleChair.activatedAbilities.first { it.isEquipAbility }.id

    private fun GameTestDriver.advanceToPlayer1Main() {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.PRECOMBAT_MAIN)
            safety++
        }
    }

    init {
        context("Sylvok Battle-Chair") {
            test("For Mirrodin! makes a Rebel that becomes a 6/6 trampler") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Sylvok Battle-Chair")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Sylvok Battle-Chair")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Exactly one Rebel token") { game.findPermanents("Rebel Token").size shouldBe 1 }
                val rebel = game.findPermanent("Rebel Token")!!
                val chair = game.findPermanent("Sylvok Battle-Chair")!!
                withClue("Battle-Chair is attached to the Rebel") {
                    game.state.getEntity(chair)?.get<AttachedToComponent>()?.targetId shouldBe rebel
                }
                val projected = game.state.projectedState
                withClue("Equipped 2/2 Rebel is a 6/6 with trample") {
                    projected.getPower(rebel) shouldBe 6
                    projected.getToughness(rebel) shouldBe 6
                    projected.hasKeyword(rebel, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("Equip costs {5}{G}{G}; moving it strips the bonus from the old creature") {
                val driver = GameTestDriver()
                driver.registerCards(TestCards.all + listOf(SylvokBattleChair))
                driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
                val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
                val otherBear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
                val chair = driver.putPermanentOnBattlefield(driver.player1, "Sylvok Battle-Chair")
                driver.advanceToPlayer1Main()

                // {5}{G} is one short of the equip cost.
                driver.giveColorlessMana(driver.player1, 5)
                driver.giveMana(driver.player1, Color.GREEN)
                driver.submit(
                    ActivateAbility(driver.player1, chair, equipId, targets = listOf(ChosenTarget.Permanent(bear)))
                ).outcome shouldNotBe Outcome.Done

                driver.giveMana(driver.player1, Color.GREEN)
                driver.submit(
                    ActivateAbility(driver.player1, chair, equipId, targets = listOf(ChosenTarget.Permanent(bear)))
                ).outcome shouldBe Outcome.Done
                driver.bothPass()
                driver.state.getEntity(chair)?.get<AttachedToComponent>()?.targetId shouldBe bear
                var projected = StateProjector().project(driver.state)
                projected.getPower(bear) shouldBe 6
                projected.getToughness(bear) shouldBe 6
                projected.hasKeyword(bear, Keyword.TRAMPLE) shouldBe true
                projected.hasKeyword(otherBear, Keyword.TRAMPLE) shouldBe false

                driver.giveColorlessMana(driver.player1, 5)
                driver.giveMana(driver.player1, Color.GREEN)
                driver.giveMana(driver.player1, Color.GREEN)
                driver.submit(
                    ActivateAbility(driver.player1, chair, equipId, targets = listOf(ChosenTarget.Permanent(otherBear)))
                ).outcome shouldBe Outcome.Done
                driver.bothPass()
                projected = StateProjector().project(driver.state)
                withClue("The old bear is back to a vanilla 2/2") {
                    projected.getPower(bear) shouldBe 2
                    projected.getToughness(bear) shouldBe 2
                    projected.hasKeyword(bear, Keyword.TRAMPLE) shouldBe false
                }
                projected.getPower(otherBear) shouldBe 6
                projected.hasKeyword(otherBear, Keyword.TRAMPLE) shouldBe true
            }
        }
    }
}
