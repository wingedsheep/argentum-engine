package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Solitary Sanctuary (WOE #30) — {2}{W} Enchantment.
 *
 *   When this enchantment enters, tap target creature an opponent controls and put a stun counter
 *   on it.
 *   Whenever you tap an untapped creature an opponent controls, put a +1/+1 counter on target
 *   creature you control.
 *
 * The point of interest is the second ability's **attribution**: it is not "whenever a creature an
 * opponent controls becomes tapped". These tests pin all four quadrants of (who tapped) × (whose
 * creature), plus the "untapped" transition rule (CR 603.2f):
 *
 *  - you tap an opponent's creature → fires (both via this card's own entry trigger and via a
 *    separate tapper you control),
 *  - the opponent taps their *own* creature → does not fire,
 *  - you tap your *own* creature → does not fire (filter),
 *  - you "tap" an already-tapped creature of theirs → does not fire (no transition, no event).
 */
class SolitarySanctuaryScenarioTest : ScenarioTestBase() {

    /**
     * Answer the engine's decisions until the stack is empty, feeding [targets] to each
     * `ChooseTargetsDecision` in the order they are asked and auto-paying mana. Returns the number
     * of target decisions that were answered — which is how many times a targeting trigger fired.
     */
    private fun TestGame.drain(targets: List<EntityId> = emptyList()): Int {
        var asked = 0
        var guard = 0
        while (guard++ < 40) {
            when (state.pendingDecision) {
                is ChooseTargetsDecision -> {
                    val pick = targets.getOrNull(asked)
                        ?: error("unexpected extra ChooseTargetsDecision (#${asked + 1})")
                    asked++
                    selectTargets(listOf(pick))
                }
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                null -> {
                    if (state.stack.isEmpty()) return asked
                    resolveStack()
                }
                else -> error("unexpected decision: ${state.pendingDecision}")
            }
        }
        error("decision loop did not settle")
    }

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.isTapped(id: EntityId): Boolean =
        state.getEntity(id)?.has<TappedComponent>() == true

    init {
        context("Solitary Sanctuary") {

            test("the entry trigger taps and stuns, and that tap is a tap you made — so the payoff fires") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Solitary Sanctuary")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Solitary Sanctuary").error shouldBe null
                // Entry trigger targets the Hill Giant; its tap then triggers the payoff, which
                // targets the Bears.
                val asked = game.drain(listOf(giant, bears))

                withClue("both the entry trigger and the payoff asked for a target") {
                    asked shouldBe 2
                }
                withClue("the opposing creature is tapped with a stun counter") {
                    game.isTapped(giant) shouldBe true
                    game.state.getEntity(giant)?.get<CountersComponent>()
                        ?.getCount(CounterType.STUN) shouldBe 1
                }
                withClue("the enchantment's own entry tap is a tap *you* made, so it pays off") {
                    game.plusOneCounters(bears) shouldBe 1
                }
            }

            test("a separate tapper you control also fires the payoff") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Solitary Sanctuary")
                    .withCardOnBattlefield(1, "Hylda's Crown of Winter")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crown = game.findPermanent("Hylda's Crown of Winter")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crown,
                        abilityId = cardRegistry.getCard("Hylda's Crown of Winter")!!
                            .activatedAbilities[0].id,
                        targets = listOf(ChosenTarget.Permanent(giant)),
                    )
                ).error shouldBe null

                game.drain(listOf(bears)) shouldBe 1
                withClue("your Crown tapped their creature, so the Sanctuary pays off") {
                    game.isTapped(giant) shouldBe true
                    game.plusOneCounters(bears) shouldBe 1
                }
            }

            test("an opponent tapping their own creature does not fire the payoff") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Solitary Sanctuary")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    // The opponent owns the tapper *and* the creature being tapped.
                    .withCardOnBattlefield(2, "Hylda's Crown of Winter")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crown = game.findPermanent("Hylda's Crown of Winter")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = crown,
                        abilityId = cardRegistry.getCard("Hylda's Crown of Winter")!!
                            .activatedAbilities[0].id,
                        targets = listOf(ChosenTarget.Permanent(giant)),
                    )
                ).error shouldBe null

                withClue("the tapped creature is still 'a creature an opponent controls' from your side, but *they* tapped it") {
                    game.drain() shouldBe 0
                    game.isTapped(giant) shouldBe true
                    game.plusOneCounters(bears) shouldBe 0
                }
            }

            test("tapping your own creature does not fire the payoff") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Solitary Sanctuary")
                    .withCardOnBattlefield(1, "Hylda's Crown of Winter")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Air Elemental", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crown = game.findPermanent("Hylda's Crown of Winter")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val elemental = game.findPermanent("Air Elemental")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crown,
                        abilityId = cardRegistry.getCard("Hylda's Crown of Winter")!!
                            .activatedAbilities[0].id,
                        targets = listOf(ChosenTarget.Permanent(elemental)),
                    )
                ).error shouldBe null

                withClue("you tapped it, but it isn't a creature an opponent controls") {
                    game.drain() shouldBe 0
                    game.isTapped(elemental) shouldBe true
                    game.plusOneCounters(bears) shouldBe 0
                }
            }

            test("targeting an already-tapped creature of theirs does not fire the payoff (CR 603.2f)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Solitary Sanctuary")
                    .withCardOnBattlefield(1, "Hylda's Crown of Winter")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crown = game.findPermanent("Hylda's Crown of Winter")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crown,
                        abilityId = cardRegistry.getCard("Hylda's Crown of Winter")!!
                            .activatedAbilities[0].id,
                        targets = listOf(ChosenTarget.Permanent(giant)),
                    )
                ).error shouldBe null

                withClue("an already-tapped permanent never *becomes* tapped, so there is no tap to pay off") {
                    game.drain() shouldBe 0
                    game.plusOneCounters(bears) shouldBe 0
                }
            }
        }
    }
}
