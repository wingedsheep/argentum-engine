package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.StingBilbosSword
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Sting, Bilbo's Sword (HOB #178) — {2} Legendary Artifact — Equipment, Flash.
 *
 *   When Sting enters, put a hone counter on Sting for each creature target opponent controls.
 *   Attach Sting to up to one target creature you control.
 *   Equip {3}
 *
 * Two target slots on one ETB trigger — slot 0 is the required opponent whose creatures are counted,
 * slot 1 is the *optional* creature to attach to. The pump itself is not on the card at all: it
 * comes from CR 122.1j via the hone counters, so these tests read projected power to prove the
 * counters actually did something (see `HoneCounterTest` for the rule in isolation).
 */
class StingBilbosSwordScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)
    private fun toughness(game: TestGame, id: EntityId): Int? =
        game.state.projectedState.getToughness(id)

    private fun honeCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.HONE) ?: 0

    init {
        cardRegistry.register(StingBilbosSword)

        context("Sting, Bilbo's Sword") {

            test("counts the opponent's creatures, attaches, and pumps by that many") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sting, Bilbo's Sword")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myBears = game.findPermanents("Grizzly Bears")
                    .single { game.state.projectedState.getController(it) == game.player1Id }

                game.castSpell(1, "Sting, Bilbo's Sword").error shouldBe null
                game.resolveStack()

                val sting = game.findPermanent("Sting, Bilbo's Sword")!!
                val td = game.getPendingDecision()!!
                game.submitDecision(
                    TargetsResponse(td.id, mapOf(0 to listOf(game.player2Id), 1 to listOf(myBears)))
                ).error shouldBe null
                game.resolveStack()

                withClue("the opponent controls two creatures, so Sting gets two hone counters") {
                    honeCounters(game, sting) shouldBe 2
                }
                withClue("Sting attached to the chosen creature") {
                    game.state.getEntity(sting)?.get<AttachedToComponent>()?.targetId shouldBe myBears
                }
                withClue("CR 122.1j: +1/+0 per hone counter, toughness untouched") {
                    power(game, myBears) shouldBe 4 // 2 + 2 hone
                    toughness(game, myBears) shouldBe 2
                }
            }

            test("the creature target is optional — declining leaves Sting unattached but honed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sting, Bilbo's Sword")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myBears = game.findPermanents("Grizzly Bears")
                    .single { game.state.projectedState.getController(it) == game.player1Id }

                game.castSpell(1, "Sting, Bilbo's Sword").error shouldBe null
                game.resolveStack()

                val sting = game.findPermanent("Sting, Bilbo's Sword")!!
                val td = game.getPendingDecision()!!
                game.submitDecision(
                    TargetsResponse(td.id, mapOf(0 to listOf(game.player2Id), 1 to emptyList()))
                ).error shouldBe null
                game.resolveStack()

                withClue("the counters still land — only the attach half was declined") {
                    honeCounters(game, sting) shouldBe 1
                }
                withClue("nothing is equipped") {
                    game.state.getEntity(sting)?.get<AttachedToComponent>().shouldBeNull()
                }
                withClue("an unattached honed Equipment pumps nobody") {
                    power(game, myBears) shouldBe 2
                }
            }

            test("an opponent with no creatures means no hone counters, but Sting still attaches") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sting, Bilbo's Sword")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myBears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Sting, Bilbo's Sword").error shouldBe null
                game.resolveStack()

                val sting = game.findPermanent("Sting, Bilbo's Sword")!!
                val td = game.getPendingDecision()!!
                game.submitDecision(
                    TargetsResponse(td.id, mapOf(0 to listOf(game.player2Id), 1 to listOf(myBears)))
                ).error shouldBe null
                game.resolveStack()

                withClue("zero creatures counted → zero counters, and the effect no-ops cleanly") {
                    honeCounters(game, sting) shouldBe 0
                }
                withClue("the attach half still happens") {
                    game.state.getEntity(sting)?.get<AttachedToComponent>()?.targetId shouldBe myBears
                }
                withClue("no counters, no bonus") {
                    power(game, myBears) shouldBe 2
                }
            }
        }
    }
}
