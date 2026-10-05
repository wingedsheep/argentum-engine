package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Arcbound Condor (MH3 #81) — {2}{B}{B} Artifact Creature — Bird 0/0
 *
 *   Flying
 *   Modular 3 (This creature enters with three +1/+1 counters on it. When it dies, you may put its
 *   +1/+1 counters on target artifact creature.)
 *   Whenever another artifact you control enters, target creature an opponent controls gets -1/-1
 *   until end of turn.
 *
 * The first **modular** card in the corpus. `Keyword.MODULAR` is display-only vocabulary, so both
 * halves the reminder text spells out are lowered by hand on the card — an `EntersWithCounters`
 * replacement plus an *optional* dies trigger that reads
 * `ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT` (CR 603.10 / 608.2g: the counters are gone
 * by the time the trigger resolves). These tests pin that lowering end to end:
 *
 *  - the printed 0/0 only survives because the replacement gives it three +1/+1 counters,
 *  - dying moves exactly those counters onto a target artifact creature, and
 *  - the enters trigger is bound to *other* artifacts, so the Condor's own entry never fires it.
 */
class ArcboundCondorScenarioTest : ScenarioTestBase() {

    init {
        fun plusOne(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        context("Arcbound Condor — modular 3") {

            test("enters as a 3/3: a printed 0/0 plus three +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Arcbound Condor")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Arcbound Condor").error shouldBe null
                game.resolveStack()

                withClue("the Condor survived the 0/0 state-based action, so the counters arrived with it") {
                    game.isOnBattlefield("Arcbound Condor") shouldBe true
                }
                val condor = game.findPermanent("Arcbound Condor")!!
                withClue("modular 3 puts three +1/+1 counters on it as it enters") {
                    plusOne(game, condor) shouldBe 3
                }
                withClue("0/0 base plus three +1/+1 counters projects as a 3/3") {
                    game.state.projectedState.getPower(condor) shouldBe 3
                    game.state.projectedState.getToughness(condor) shouldBe 3
                }
            }

            test("when it dies, its +1/+1 counters move onto another artifact creature") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Arcbound Condor")
                    .withCardInHand(1, "Lightning Bolt")
                    // Ornithopter (0/2 artifact creature) is the only legal modular target once the
                    // Condor itself has left the battlefield.
                    .withCardOnBattlefield(1, "Ornithopter", summoningSickness = false)
                    // Generous and lopsided on purpose: the Condor's {2} generic can eat at most two
                    // Mountains, so a red source is always left over for the Bolt.
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Arcbound Condor").error shouldBe null
                game.resolveStack()

                val condor = game.findPermanent("Arcbound Condor")!!
                val thopter = game.findPermanent("Ornithopter")!!
                withClue("the Condor is a 3/3 built out of counters before it dies") {
                    plusOne(game, condor) shouldBe 3
                }
                withClue("Ornithopter starts with no counters") {
                    plusOne(game, thopter) shouldBe 0
                }

                // Three damage kills the 3/3.
                game.castSpell(1, "Lightning Bolt", condor).error shouldBe null
                game.resolveStack()

                withClue("the Condor died") {
                    game.findPermanent("Arcbound Condor") shouldBe null
                    game.isInGraveyard(1, "Arcbound Condor") shouldBe true
                }

                // Modular announces the artifact creature target before optional consent.
                game.selectTargets(listOf(thopter)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(true).error shouldBe null

                withClue("all three of the Condor's last-known +1/+1 counters moved to Ornithopter") {
                    plusOne(game, thopter) shouldBe 3
                }
                withClue("Ornithopter is a 0/2 grown to a 3/5") {
                    game.state.projectedState.getPower(thopter) shouldBe 3
                    game.state.projectedState.getToughness(thopter) shouldBe 5
                }
            }

            test("another artifact entering gives an opponent's creature -1/-1; its own entry does not") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Arcbound Condor")
                    .withCardInHand(1, "Ornithopter")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                // The Condor is itself an artifact, but the trigger is bound to OTHER artifacts.
                game.castSpell(1, "Arcbound Condor").error shouldBe null
                game.resolveStack()

                withClue("the Condor's own entry raised no targeting decision") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("Grizzly Bears is untouched by the Condor's own entry") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }

                // Ornithopter costs {0}, so no extra mana is needed.
                game.castSpell(1, "Ornithopter").error shouldBe null
                game.resolveStack()

                withClue("another artifact entering fires the trigger, which needs a target") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears got -1/-1 until end of turn") {
                    game.state.projectedState.getPower(bears) shouldBe 1
                    game.state.projectedState.getToughness(bears) shouldBe 1
                }
            }
        }
    }
}
