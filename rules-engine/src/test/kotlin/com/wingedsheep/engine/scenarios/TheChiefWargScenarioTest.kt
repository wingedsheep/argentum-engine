package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The Chief Warg (HOB) — {2}{B}{G} Legendary Creature — Wolf 3/3.
 *
 * "Menace
 *  Ferocious — Whenever you attack while you control a creature with power 4 or greater,
 *  you draw a card and lose 1 life."
 *
 * Two things to pin down: the trigger is the "whenever you attack" *group* trigger — it fires once
 * per combat and does not require the Warg itself to attack — and the ferocious clause gates it.
 * The Chief Warg is a 3/3, so it never turns its own trigger on.
 */
class TheChiefWargScenarioTest : ScenarioTestBase() {

    init {
        context("The Chief Warg") {

            test("it is a 3/3 with menace") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Chief Warg")
                    .build()

                val warg = game.findPermanent("The Chief Warg")!!
                game.state.projectedState.getPower(warg) shouldBe 3
                game.state.projectedState.getToughness(warg) shouldBe 3
                game.state.projectedState.hasKeyword(warg, Keyword.MENACE) shouldBe true
            }

            test("attacking with a power-4 creature on board draws a card and loses 1 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Chief Warg")
                    // Serra Angel is a 4/4 — it turns ferocious on.
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("The Chief Warg" to 2)).error shouldBe null
                game.resolveStack()

                withClue("ferocious is satisfied by the 4/4 sitting at home") {
                    game.handSize(1) shouldBe handBefore + 1
                    game.getLifeTotal(1) shouldBe 19
                }
            }

            test("it fires once per combat, not once per attacker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Chief Warg")
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("The Chief Warg" to 2, "Serra Angel" to 2)).error shouldBe null
                game.resolveStack()

                withClue("two attackers, still exactly one card drawn and 1 life lost") {
                    game.handSize(1) shouldBe handBefore + 1
                    game.getLifeTotal(1) shouldBe 19
                }
            }

            test("without a power-4 creature the trigger does not fire") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Chief Warg")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("The Chief Warg" to 2)).error shouldBe null
                game.resolveStack()

                withClue("The Chief Warg's own 3 power doesn't satisfy ferocious") {
                    game.handSize(1) shouldBe handBefore
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("it does not need to attack itself — another creature swinging is enough") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Chief Warg")
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Serra Angel" to 2)).error shouldBe null
                game.resolveStack()

                withClue("'whenever you attack' doesn't care which creatures attacked") {
                    game.handSize(1) shouldBe handBefore + 1
                    game.getLifeTotal(1) shouldBe 19
                }
            }
        }
    }
}
