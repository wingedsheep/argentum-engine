package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Icewrought Sentry (WOE #55) — {2}{U} Creature — Elemental Soldier 2/3.
 *
 *   Vigilance
 *   Whenever this creature attacks, you may pay {1}{U}. When you do, tap target creature an
 *   opponent controls.
 *   Whenever you tap an untapped creature an opponent controls, this creature gets +2/+1 until end
 *   of turn.
 *
 * Two abilities that chain into each other: the reflexive tap is itself "a tap you made", so paying
 * for it also pumps the Sentry. The pump is per-tap and not restricted to its own tap — any tapper
 * you control feeds it — and it must not fire off the opponent's own taps.
 */
class IcewroughtSentryScenarioTest : ScenarioTestBase() {

    private fun TestGame.power(id: EntityId): Int =
        state.projectedState.getPower(id) ?: error("no projected power for $id")

    private fun TestGame.toughness(id: EntityId): Int =
        state.projectedState.getToughness(id) ?: error("no projected toughness for $id")

    private fun TestGame.isTapped(id: EntityId): Boolean =
        state.getEntity(id)?.has<TappedComponent>() == true

    /** Answer decisions until the stack is empty, paying every optional cost and picking [target]. */
    private fun TestGame.drainPaying(target: EntityId?) {
        var guard = 0
        while (guard++ < 40) {
            when (state.pendingDecision) {
                is YesNoDecision -> answerYesNo(true)
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                is ChooseTargetsDecision -> selectTargets(listOfNotNull(target))
                null -> {
                    if (state.stack.isEmpty()) return
                    resolveStack()
                }
                else -> error("unexpected decision: ${state.pendingDecision}")
            }
        }
        error("decision loop did not settle")
    }

    init {
        context("Icewrought Sentry") {

            test("paying the attack trigger taps an opposing creature and pumps the Sentry to 4/4") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Icewrought Sentry", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .withTurnNumber(3)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sentry = game.findPermanent("Icewrought Sentry")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Icewrought Sentry" to 2)).error shouldBe null
                game.drainPaying(giant)

                withClue("the reflexive tap resolved") {
                    game.isTapped(giant) shouldBe true
                }
                withClue("vigilance means attacking didn't tap the Sentry itself") {
                    game.isTapped(sentry) shouldBe false
                }
                withClue("its own reflexive tap is a tap *you* made, so the Sentry pumps: 2/3 -> 4/4") {
                    game.power(sentry) shouldBe 4
                    game.toughness(sentry) shouldBe 4
                }
            }

            test("declining the payment taps nothing and leaves the Sentry a 2/3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Icewrought Sentry", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .withTurnNumber(3)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sentry = game.findPermanent("Icewrought Sentry")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Icewrought Sentry" to 2)).error shouldBe null

                var guard = 0
                var declined = false
                while (guard++ < 20 && !declined) {
                    when (game.state.pendingDecision) {
                        is YesNoDecision -> {
                            game.answerYesNo(false); declined = true
                        }
                        null -> game.resolveStack()
                        else -> error("unexpected decision: ${game.state.pendingDecision}")
                    }
                }
                withClue("the optional payment was genuinely offered") { declined shouldBe true }

                withClue("no tap happened, so no pump") {
                    game.isTapped(giant) shouldBe false
                    game.power(sentry) shouldBe 2
                    game.toughness(sentry) shouldBe 3
                }
            }

            test("two separate taps of opposing creatures pump it twice — it is not once per turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Icewrought Sentry", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hylda's Crown of Winter")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sentry = game.findPermanent("Icewrought Sentry")!!
                val crown = game.findPermanent("Hylda's Crown of Winter")!!
                val tapAbility = cardRegistry.getCard("Hylda's Crown of Winter")!!
                    .activatedAbilities[0].id

                // The Crown's {T} is part of its cost, so untap it between activations.
                for (victimName in listOf("Hill Giant", "Grizzly Bears")) {
                    val victim = game.findPermanent(victimName)!!
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = crown,
                            abilityId = tapAbility,
                            targets = listOf(ChosenTarget.Permanent(victim)),
                        )
                    ).error shouldBe null
                    game.drainPaying(null)
                    game.state = game.state.updateEntity(crown) { it.without<TappedComponent>() }
                }

                withClue("two taps, two pumps: 2/3 -> 6/5") {
                    game.power(sentry) shouldBe 6
                    game.toughness(sentry) shouldBe 5
                }
            }

            test("an opponent tapping their own creature does not pump it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Icewrought Sentry", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hylda's Crown of Winter")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sentry = game.findPermanent("Icewrought Sentry")!!
                val crown = game.findPermanent("Hylda's Crown of Winter")!!
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
                game.drainPaying(null)

                withClue("their tap of their own creature is not a tap you made") {
                    game.isTapped(giant) shouldBe true
                    game.power(sentry) shouldBe 2
                    game.toughness(sentry) shouldBe 3
                }
            }
        }
    }
}
