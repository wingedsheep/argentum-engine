package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Strict Proctor (STX #33) — {1}{W} Creature — Spirit Cleric, 1/3.
 *
 *   Flying
 *   Whenever a permanent entering causes a triggered ability to trigger, counter that ability
 *   unless its controller pays {2}.
 *
 * The ETB-caused ability goes on the stack first and Strict Proctor's trigger lands on top of it
 * (CR 603.3b), so the tax is asked while the ability waits. Triggers not caused by an entry
 * (prowess, a cast trigger) are left alone.
 */
class StrictProctorScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.stackSourceNames(): List<String?> = state.stack.map {
            state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()?.sourceName
        }

        context("Strict Proctor") {

            test("counters an enters trigger whose controller can't pay") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Strict Proctor")
                    .withCardInHand(1, "Elvish Visionary")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Elvish Visionary").error shouldBe null
                game.passPriority().error shouldBe null
                game.passPriority().error shouldBe null

                withClue("Strict Proctor's trigger goes on the stack above the ETB it saw") {
                    game.stackSourceNames() shouldBe listOf("Elvish Visionary", "Strict Proctor")
                }
                val handBefore = game.handSize(1)
                game.resolveStack()

                withClue("no mana left for {2}, so the draw was countered") {
                    game.state.stack.isEmpty() shouldBe true
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("the enters trigger resolves when its controller pays {2}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Strict Proctor")
                    .withCardInHand(1, "Elvish Visionary")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Elvish Visionary").error shouldBe null
                game.passPriority().error shouldBe null
                game.passPriority().error shouldBe null
                val handBefore = game.handSize(1)
                game.passPriority().error shouldBe null
                game.passPriority().error shouldBe null

                val ask = game.getPendingDecision()
                ask.shouldBeInstanceOf<YesNoDecision>()
                ask.playerId shouldBe game.player1Id
                game.submitDecision(YesNoResponse(ask.id, true)).error shouldBe null
                val window = game.getPendingDecision()
                window.shouldBeInstanceOf<SelectManaSourcesDecision>()
                game.submitDecision(ManaSourcesSelectedResponse(window.id, autoPay = true)).error shouldBe null
                game.resolveStack()

                withClue("the tax was paid, so Elvish Visionary's draw resolved") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("taxes another permanent's 'whenever a creature enters' trigger, whoever controls it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Strict Proctor")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Soul Warden")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("the opponent's Soul Warden trigger was countered: they had no mana to pay") {
                    game.state.stack.isEmpty() shouldBe true
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("ignores triggers an entry didn't cause") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Strict Proctor")
                    .withCardOnBattlefield(1, "Monastery Swiftspear")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                withClue("only prowess triggered; Strict Proctor stayed silent") {
                    game.stackSourceNames().filterNotNull() shouldBe listOf("Monastery Swiftspear")
                }
                game.resolveStack()

                withClue("prowess resolved untaxed") {
                    val swiftspear = game.findPermanent("Monastery Swiftspear")!!
                    game.state.projectedState.getPower(swiftspear) shouldBe 2
                    game.getLifeTotal(2) shouldBe 17
                }
            }
        }
    }
}
