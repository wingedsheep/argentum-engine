package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The Motherlode, Excavator (PIP #62) — ETB energy equal to target opponent's nonbasic lands;
 * on attack, pay {E}{E}{E}{E} for a reflexive trigger that destroys a nonbasic land defending
 * player controls and stops that player's creatures without flying from blocking this turn.
 * Pins the player being fixed when the reflexive ability resolves: the restriction holds after
 * The Motherlode itself leaves combat, when "defending player" can no longer be read off it.
 */
class TheMotherlodeExcavatorScenarioTest : ScenarioTestBase() {

    private val motherlode = "The Motherlode, Excavator"

    private fun TestGame.energy(): Int =
        state.getEntity(player1Id)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    private fun TestGame.seedEnergy(amount: Int) {
        state = state.updateEntity(player1Id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.ENERGY, amount))
        }
    }

    private fun combatGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, motherlode)
        .withCardOnBattlefield(1, "Centaur Courser")
        .withCardInHand(1, "Unsummon")
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Wind Drake")
        .withCardOnBattlefield(2, "Tainted Isle")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { it.seedEnergy(4) }

    /** Attack with both creatures, answer the payment, and (if paid) aim the reflexive trigger at the Isle. */
    private fun TestGame.attackAndPay(pay: Boolean) {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf(motherlode to 2, "Centaur Courser" to 2)).error shouldBe null
        resolveStack()
        getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        answerYesNo(pay).error shouldBe null
        if (pay) {
            val decision = getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(findPermanent("Tainted Isle")!!))))
                .error shouldBe null
        }
        resolveStack()
    }

    init {
        context("The Motherlode, Excavator") {

            test("entering gives energy equal to the target opponent's nonbasic lands") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, motherlode)
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(2, "Tainted Isle")
                    .withCardOnBattlefield(2, "Mariposa Military Base")
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, motherlode).error shouldBe null
                game.resolveStack()
                (game.getPendingDecision() as? ChooseTargetsDecision)?.let {
                    game.submitDecision(TargetsResponse(it.id, mapOf(0 to listOf(game.player2Id)))).error shouldBe null
                }
                game.resolveStack()

                game.energy() shouldBe 2
            }

            test("paying destroys the nonbasic land and stops that player's nonflyers from blocking") {
                val game = combatGame()
                game.attackAndPay(pay = true)

                game.energy() shouldBe 0
                game.isOnBattlefield("Tainted Isle") shouldBe false
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("a creature without flying can't block") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf(motherlode))).error shouldNotBe null
                }
                withClue("a flyer still blocks") {
                    game.declareBlockers(mapOf("Wind Drake" to listOf(motherlode))).error shouldBe null
                }
            }

            test("the restriction outlives The Motherlode leaving combat") {
                val game = combatGame()
                game.attackAndPay(pay = true)

                game.castSpell(1, "Unsummon", game.findPermanent(motherlode)).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield(motherlode) shouldBe false

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("defending player was fixed when the reflexive ability resolved") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Centaur Courser"))).error shouldNotBe null
                }
            }

            test("declining keeps the energy and the land, and blocks stay legal") {
                val game = combatGame()
                game.attackAndPay(pay = false)

                game.energy() shouldBe 4
                game.isOnBattlefield("Tainted Isle") shouldBe true
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf(motherlode))).error shouldBe null
            }
        }
    }
}
