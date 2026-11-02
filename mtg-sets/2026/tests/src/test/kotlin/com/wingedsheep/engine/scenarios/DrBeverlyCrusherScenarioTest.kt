package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dr. Beverly Crusher: "If you gaining life causes a triggered ability of a permanent you control to
 * trigger, that ability triggers an additional time. Whenever you gain life, put a +1/+1 counter on
 * Dr. Crusher." ([com.wingedsheep.sdk.scripting.AdditionalLifeGainTriggers]).
 */
class DrBeverlyCrusherScenarioTest : ScenarioTestBase() {

    /** A permanent whose life-gain trigger watches every player, not just its controller. */
    private val watcher = card("Test Life Watcher") {
        manaCost = "{1}{W}"
        colorIdentity = "W"
        typeLine = "Creature — Human"
        oracleText = "Whenever a player gains life, put a +1/+1 counter on this creature."
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.anyPlayer.gainsLife()
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    private fun TestGame.plusOneCounters(name: String, playerNumber: Int = 1): Int {
        val id = findPermanents(name).firstOrNull { state.projectedState.getController(it) == playerId(playerNumber) }
            ?: error("$name not found for player $playerNumber")
        return state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    private fun playerId(n: Int): EntityId = EntityId.of("player-$n")

    /** Resolve the stack, aiming any "target opponent" trigger (Sanguine Bond) at player 2. */
    private fun TestGame.resolveAll() {
        repeat(20) {
            if (getPendingDecision() is ChooseTargetsDecision) {
                selectTargets(listOf(playerId(2))).error shouldBe null
            }
            if (state.stack.isEmpty() && getPendingDecision() == null) return
            resolveStack()
        }
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Dr. Beverly Crusher")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        cardRegistry.register(watcher)

        context("Dr. Beverly Crusher") {
            test("her own life-gain trigger triggers an additional time") {
                val game = board().withCardInHand(1, "Angel's Mercy").withLandsOnBattlefield(1, "Plains", 4).build()

                game.castSpell(1, "Angel's Mercy").error shouldBe null
                game.resolveAll()

                game.getLifeTotal(1) shouldBe 27
                withClue("one life-gain event, her trigger fires twice") {
                    game.plusOneCounters("Dr. Beverly Crusher") shouldBe 2
                }
            }

            test("another permanent you control's life-gain trigger triggers an additional time") {
                val game = board()
                    .withCardOnBattlefield(1, "Ajani's Pridemate")
                    .withCardOnBattlefield(1, "Sanguine Bond")
                    .withCardInHand(1, "Angel's Mercy").withLandsOnBattlefield(1, "Plains", 4)
                    .build()

                game.castSpell(1, "Angel's Mercy").error shouldBe null
                game.resolveAll()

                game.plusOneCounters("Ajani's Pridemate") shouldBe 2
                withClue("Sanguine Bond's 'loses that much life' resolves twice") {
                    game.getLifeTotal(2) shouldBe 20 - 14
                }
            }

            test("an opponent gaining life is not you gaining life") {
                val game = board()
                    .withCardOnBattlefield(2, "Ajani's Pridemate")
                    .withCardInHand(1, "Natural Spring").withLandsOnBattlefield(1, "Forest", 5)
                    .build()

                game.castSpellTargetingPlayer(1, "Natural Spring", 2).error shouldBe null
                game.resolveAll()

                game.getLifeTotal(2) shouldBe 28
                withClue("the opponent's own Pridemate triggers once, undoubled") {
                    game.plusOneCounters("Ajani's Pridemate", playerNumber = 2) shouldBe 1
                }
                game.plusOneCounters("Dr. Beverly Crusher") shouldBe 0
            }

            test("your 'whenever a player gains life' doubles only for your own gain in a simultaneous batch") {
                val game = board()
                    .withCardOnBattlefield(1, "Test Life Watcher")
                    .withCardInHand(1, "Centaur Peacemaker")
                    .withLandsOnBattlefield(1, "Plains", 2).withLandsOnBattlefield(1, "Forest", 1)
                    .build()

                game.castSpell(1, "Centaur Peacemaker").error shouldBe null
                game.resolveAll()

                game.getLifeTotal(1) shouldBe 24
                game.getLifeTotal(2) shouldBe 24
                withClue("your gain triggers it twice, the opponent's gain once") {
                    game.plusOneCounters("Test Life Watcher") shouldBe 3
                }
                game.plusOneCounters("Dr. Beverly Crusher") shouldBe 2
            }

            test("without Dr. Crusher a life-gain trigger fires once") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ajani's Pridemate")
                    .withCardInHand(1, "Angel's Mercy").withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Angel's Mercy").error shouldBe null
                game.resolveAll()

                game.plusOneCounters("Ajani's Pridemate") shouldBe 1
            }
        }
    }
}
