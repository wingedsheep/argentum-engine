package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Taeko, the Patient Avalanche (J25 #39):
 *   Taeko enters tapped.
 *   Whenever another creature you control leaves the battlefield, if it didn't die, scry 1 and put
 *   a +1/+1 counter on Taeko.
 *   Whenever Taeko attacks, you may pay {U/B}. When you do, target attacking creature can't be
 *   blocked this turn.
 *
 * Pins the two compositions no other card combines: a per-creature "another … leaves, but not to a
 * graveyard" trigger (a bounce counts, a death doesn't), and a reflexive pay-then-target ability
 * whose target is an *attacking* creature chosen only after the hybrid mana is paid.
 */
class TaekoThePatientAvalancheScenarioTest : ScenarioTestBase() {

    private fun TestGame.taekoCounters(): Int = findPermanent("Taeko, the Patient Avalanche")
        ?.let { state.getEntity(it)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) }
        ?: 0

    init {
        context("Taeko, the Patient Avalanche") {

            test("it enters tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Taeko, the Patient Avalanche")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Taeko, the Patient Avalanche").error shouldBe null
                game.resolveStack()

                val taeko = game.findPermanent("Taeko, the Patient Avalanche")!!
                game.state.getEntity(taeko)!!.get<TappedComponent>().shouldNotBeNull()
            }

            test("another creature you control bounced: scry 1, then a +1/+1 counter on Taeko") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Taeko, the Patient Avalanche")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Unsummon")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Unsummon", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                withClue("the leave trigger resolves into a scry prompt") {
                    game.hasPendingDecision() shouldBe true
                }
                game.skipSelection().error shouldBe null
                game.resolveStack()

                withClue("the Bears left without dying, so Taeko grew to 5/6") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.taekoCounters() shouldBe 1
                    val taeko = game.findPermanent("Taeko, the Patient Avalanche")!!
                    game.state.projectedState.getPower(taeko) shouldBe 5
                    game.state.projectedState.getToughness(taeko) shouldBe 6
                }
            }

            test("another creature you control dying does not trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Taeko, the Patient Avalanche")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()
                game.resolveStack()

                withClue("the Bears died, so nothing triggered") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.hasPendingDecision() shouldBe false
                    game.taekoCounters() shouldBe 0
                }
            }

            test("paying {U/B} on attack makes the chosen attacking creature unblockable this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Taeko, the Patient Avalanche")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val taeko = game.findPermanent("Taeko, the Patient Avalanche")!!

                game.declareAttackers(
                    mapOf("Taeko, the Patient Avalanche" to 2, "Grizzly Bears" to 2)
                ).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                if (game.state.pendingDecision is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay().error shouldBe null
                }
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("the Bears can't be blocked; Taeko itself wasn't chosen") {
                    game.state.projectedState.hasKeyword(bears, "CANT_BE_BLOCKED") shouldBe true
                    game.state.projectedState.hasKeyword(taeko, "CANT_BE_BLOCKED") shouldBe false
                }
            }

            test("declining to pay grants nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Taeko, the Patient Avalanche")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val taeko = game.findPermanent("Taeko, the Patient Avalanche")!!

                game.declareAttackers(mapOf("Taeko, the Patient Avalanche" to 2)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.state.projectedState.hasKeyword(taeko, "CANT_BE_BLOCKED") shouldBe false
            }
        }
    }
}
