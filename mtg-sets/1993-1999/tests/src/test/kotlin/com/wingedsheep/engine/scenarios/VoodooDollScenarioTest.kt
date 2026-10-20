package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Voodoo Doll (LEG #298) — "At the beginning of your upkeep, put a pin counter on this artifact.
 * At the beginning of your end step, if this artifact is untapped, destroy this artifact and it
 * deals damage to you equal to the number of pin counters on it. {X}{X}, {T}: This artifact deals
 * damage equal to the number of pin counters on it to any target. X is the number of pin counters
 * on this artifact."
 *
 * The end-step tests pin the card's ruling: if the Doll is gone by the time the trigger resolves,
 * "if it is untapped" is answered from its last existence on the battlefield — so a Doll destroyed
 * untapped in response still hurts its controller, and one tapped (by its own ability) before it
 * was destroyed does nothing.
 */
class VoodooDollScenarioTest : ScenarioTestBase() {

    init {
        context("Voodoo Doll") {

            fun board(activePlayer: Int = 1, tapped: Boolean = false, pins: Int = 0) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Voodoo Doll", tapped = tapped)
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardInHand(1, "Shatter")
                .apply {
                    repeat(6) {
                        withCardInLibrary(1, "Mountain")
                        withCardInLibrary(2, "Mountain")
                    }
                }
                .withActivePlayer(activePlayer)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
                .also { game ->
                    if (pins > 0) {
                        val doll = game.findPermanent("Voodoo Doll")!!
                        game.state = game.state.updateEntity(doll) {
                            it.with(CountersComponent().withAdded(CounterType.PIN, pins))
                        }
                    }
                }

            fun TestGame.doll() = findPermanent("Voodoo Doll")
            fun TestGame.pins(): Int =
                state.getEntity(doll()!!)?.get<CountersComponent>()?.getCount(CounterType.PIN) ?: 0

            fun TestGame.activateDoll(): com.wingedsheep.engine.core.ExecutionResult {
                val abilityId = cardRegistry.getCard("Voodoo Doll")!!.activatedAbilities.first().id
                return execute(
                    ActivateAbility(player1Id, doll()!!, abilityId, listOf(ChosenTarget.Player(player2Id)))
                )
            }

            test("puts a pin counter on itself at the beginning of your upkeep") {
                val game = board(activePlayer = 2)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()
                game.pins() shouldBe 1
            }

            test("untapped at your end step: destroys itself and deals damage equal to its pin counters to you") {
                val game = board(pins = 3)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the Doll is destroyed") { game.doll() shouldBe null }
                withClue("3 pin counters, 3 damage to its controller") { game.getLifeTotal(1) shouldBe 17 }
            }

            test("tapped at your end step: the ability doesn't trigger") {
                val game = board(tapped = true, pins = 3)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.doll() shouldNotBe null
                game.getLifeTotal(1) shouldBe 20
            }

            test("destroyed untapped in response: last-known untapped, so it still deals its last-known pin count to you") {
                val game = board(pins = 2)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.state.stack.size shouldBe 1

                game.castSpell(1, "Shatter", game.doll()!!).error shouldBe null
                game.resolveStack()

                game.doll() shouldBe null
                withClue("CR 608.2h: the intervening-if reads the Doll as it last existed (untapped)") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }

            test("tapped by its own ability, then destroyed in response: last-known tapped, so the trigger does nothing") {
                val game = board(pins = 2)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.state.stack.size shouldBe 1

                game.activateDoll().error shouldBe null
                game.castSpell(1, "Shatter", game.doll()!!).error shouldBe null
                game.resolveStack()

                game.doll() shouldBe null
                withClue("the activated ability still deals its last-known 2 to the opponent") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("the Doll was tapped when it left, so the end-step trigger did nothing") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("{X}{X}, {T}: X is its pin count, and it deals that much damage to any target") {
                val game = board(pins = 3)
                val doll = game.doll()!!

                game.activateDoll().error shouldBe null
                game.resolveStack()

                withClue("X = 3 pin counters, so the ability cost {6}") {
                    game.state.getBattlefield(game.player1Id).count {
                        game.state.getEntity(it)?.has<TappedComponent>() == true
                    } shouldBe 7
                }
                game.getLifeTotal(2) shouldBe 17
                game.state.getEntity(doll)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
