package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Invisible Woman, Sue Storm (MSH #17) — {4}{W} Legendary Creature — Human Hero,
 * 2/5, uncommon.
 *
 * "Lifelink
 *  Whenever you put one or more +1/+1 counters on one or more other Heroes you control, you may
 *  create a 0/4 colorless Wall creature token with defender."
 *
 * The headline is the **batch** counter-placement trigger, `CountersPlacedEvent(batch = true)`
 * (CR 603.2c): one effect that counters several of your Heroes at once makes one Wall, not one per
 * Hero.
 *
 * The driver is an inline "put a +1/+1 counter on each creature you control" sorcery. Its counters
 * land on Invisible Woman herself (excluded by "other"), on non-Heroes (excluded by the filter) and
 * on the Heroes (the batch) in one resolution, so every test exercises the narrowing too. A printed
 * card with the same text (Cathars' Crusade) would work equally well for the batch itself, but it
 * triggers again off the Wall entering, which loops with this card — the loop is real game
 * behaviour, not something these tests should be asserting through.
 */
class InvisibleWomanSueStormScenarioTest : ScenarioTestBase() {

    private val rally = card("Test Rallying Call") {
        manaCost = "{1}{W}"
        colorIdentity = "W"
        typeLine = "Sorcery"
        oracleText = "Put a +1/+1 counter on each creature you control."
        spell {
            effect = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl()),
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
            )
        }
    }

    init {
        cardRegistry.register(rally)

        context("Invisible Woman, Sue Storm") {

            test("counters landing on two other Heroes at once make exactly one Wall") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Invisible Woman, Sue Storm")
                    .withCardOnBattlefield(1, "Hero in Training")
                    .withCardOnBattlefield(1, "Hero in Training")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(1, "Test Rallying Call")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Rallying Call").error shouldBe null
                game.resolveStack()

                withClue("the may-create decision is raised for the batch") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("no second copy of the trigger is waiting behind the first") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("two Heroes countered by one effect make one Wall, not two") {
                    game.findPermanents("Wall Token").size shouldBe 1
                }
            }

            test("declining the may creates no Wall") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Invisible Woman, Sue Storm")
                    .withCardOnBattlefield(1, "Hero in Training")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(1, "Test Rallying Call")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Rallying Call").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("'you may create' declined leaves no token") {
                    game.findPermanents("Wall Token").size shouldBe 0
                }
            }

            test("counters on Invisible Woman and non-Heroes alone do not fire it") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Invisible Woman, Sue Storm")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(1, "Test Rallying Call")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // The only counters land on Invisible Woman ("other" excludes her) and on Grizzly
                // Bears (not a Hero).
                game.castSpell(1, "Test Rallying Call").error shouldBe null
                game.resolveStack()

                withClue("no matching recipient means no trigger and so no decision") {
                    game.hasPendingDecision() shouldBe false
                }
                game.findPermanents("Wall Token").size shouldBe 0
            }

            /**
             * A Hero that can't have counters put on it (Blossombind) receives none, so it never
             * enters the batch. With no other Hero on the board the trigger has nothing to fire on.
             *
             * This proves the *guarded* placement path only: `Test Rallying Call` is
             * `ForEachInGroup` + `AddCounters`, so it runs through `AddCountersExecutor`, which
             * checks `ProjectedState.canReceiveCounters` before emitting. That check is
             * shared with AddCountersToCollectionExecutor. This scenario still proves the
             * ForEachInGroup path only; other independent counter emitters are not certified here.
             */
            test("a Hero that can't have counters put on it is not in the batch") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Invisible Woman, Sue Storm")
                    .withCardOnBattlefield(1, "Hero in Training")
                    .withCardAttachedTo(1, "Blossombind", "Hero in Training")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(1, "Test Rallying Call")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Rallying Call").error shouldBe null
                game.resolveStack()

                withClue("the enchanted Hero got no counter, so the batch is empty") {
                    game.hasPendingDecision() shouldBe false
                }
                game.findPermanents("Wall Token").size shouldBe 0
            }

            test("two separate counter placements in one turn make two Walls") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Invisible Woman, Sue Storm")
                    .withCardOnBattlefield(1, "Hero in Training")
                    .withLandsOnBattlefield(1, "Plains", 8)
                    .withCardInHand(1, "Test Rallying Call")
                    .withCardInHand(1, "Test Rallying Call")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Rallying Call").error shouldBe null
                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Test Rallying Call").error shouldBe null
                game.resolveStack()
                withClue("the second placement is its own batch, so the trigger fires again") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.findPermanents("Wall Token").size shouldBe 2
            }
        }
    }
}
