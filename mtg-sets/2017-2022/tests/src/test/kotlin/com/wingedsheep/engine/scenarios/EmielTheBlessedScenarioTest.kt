package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Emiel the Blessed (JMP #3, reprinted J22 #180) — {2}{W}{W} Legendary Creature — Unicorn, 4/4.
 *
 *   {3}: Exile another target creature you control, then return it to the battlefield under its
 *   owner's control.
 *   Whenever another creature you control enters, you may pay {G/W}. If you do, put a +1/+1
 *   counter on it. If it's a Unicorn, put two +1/+1 counters on it instead.
 */
class EmielTheBlessedScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Answer the pay prompt with [pay], auto-pay its mana, and drain the stack. */
    private fun settle(game: TestGame, pay: Boolean) {
        var guard = 0
        while (guard++ < 30) {
            when (val decision = game.getPendingDecision()) {
                is YesNoDecision -> game.answerYesNo(pay)
                is SelectManaSourcesDecision -> game.submitManaSourcesAutoPay()
                null -> if (game.state.stack.isNotEmpty()) game.resolveStack() else return
                else -> error("unexpected decision $decision")
            }
        }
    }

    private fun boardWithInHand(cardInHand: String): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Emiel the Blessed")
        .withCardInHand(1, cardInHand)
        .withLandsOnBattlefield(1, "Plains", 4)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Emiel the Blessed") {

            test("paying {G/W} puts one +1/+1 counter on an entering non-Unicorn") {
                val game = boardWithInHand("Grizzly Bears")

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                settle(game, pay = true)

                val bears = game.findPermanent("Grizzly Bears")
                bears shouldNotBe null
                plusOnes(game, bears!!) shouldBe 1
            }

            test("an entering Unicorn gets two +1/+1 counters instead") {
                val game = boardWithInHand("Pearled Unicorn")

                game.castSpell(1, "Pearled Unicorn").error shouldBe null
                settle(game, pay = true)

                val unicorn = game.findPermanent("Pearled Unicorn")
                unicorn shouldNotBe null
                plusOnes(game, unicorn!!) shouldBe 2
            }

            test("declining to pay puts no counter, even on a Unicorn") {
                val game = boardWithInHand("Pearled Unicorn")

                game.castSpell(1, "Pearled Unicorn").error shouldBe null
                settle(game, pay = false)

                val unicorn = game.findPermanent("Pearled Unicorn")
                unicorn shouldNotBe null
                plusOnes(game, unicorn!!) shouldBe 0
            }

            test("an opponent's creature entering does not trigger Emiel") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Emiel the Blessed")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("no pay prompt for Emiel's controller") {
                    game.getPendingDecision() shouldBe null
                }
                plusOnes(game, game.findPermanent("Grizzly Bears")!!) shouldBe 0
            }

            test("{3}: blinks another creature, shedding its counters, and the re-entry retriggers") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Emiel the Blessed")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val emiel = game.findPermanent("Emiel the Blessed")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) { c ->
                    c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 3))
                }
                val abilityId = cardRegistry.requireCard("Emiel the Blessed").script.activatedAbilities.first().id

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = emiel,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                )
                withClue("Activation should succeed: ${activation.error}") { activation.error shouldBe null }
                settle(game, pay = true)

                val returned = game.findPermanent("Grizzly Bears")
                withClue("Grizzly Bears is back on the battlefield") { returned shouldNotBe null }
                withClue("old counters ceased to exist; the re-entry trigger added one") {
                    plusOnes(game, returned!!) shouldBe 1
                }
            }

            test("{3} cannot target Emiel itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Emiel the Blessed")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val emiel = game.findPermanent("Emiel the Blessed")!!
                val abilityId = cardRegistry.requireCard("Emiel the Blessed").script.activatedAbilities.first().id

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = emiel,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(emiel)),
                    )
                )
                withClue("Targeting itself must be rejected") { activation.error shouldNotBe null }
            }
        }
    }
}
