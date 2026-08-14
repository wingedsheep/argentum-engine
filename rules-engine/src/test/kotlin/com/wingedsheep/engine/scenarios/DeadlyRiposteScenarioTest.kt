package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Deadly Riposte (BRO #5, reprinted in FDN) — {1}{W} Instant.
 *
 *   Deadly Riposte deals 3 damage to target tapped creature and you gain 2 life.
 *
 * The damage clause and the life clause resolve from the same spell, so the case that matters is
 * that *both* land: a regression where the target failed to resolve showed up as "the caster
 * gained life but the creature took no damage".
 */
class DeadlyRiposteScenarioTest : ScenarioTestBase() {

    init {
        context("Deadly Riposte") {

            test("deals 3 damage to the tapped creature and the caster gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Deadly Riposte")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true) // 3/3, tapped
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val lifeBefore = game.getLifeTotal(1)

                game.castSpell(1, "Deadly Riposte", giant).error shouldBe null
                game.resolveStack()

                withClue("3 damage kills the tapped 3/3") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
                withClue("the caster gains exactly 2 life") {
                    game.getLifeTotal(1) shouldBe lifeBefore + 2
                }
            }

            test("a tapped creature that survives the damage stays on the battlefield") {
                // A 4/4 takes 3 and lives — proves the damage is 3, not lethal-by-accident.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Deadly Riposte")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Serra Angel", tapped = true) // 4/4, tapped
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val angel = game.findPermanent("Serra Angel")!!

                game.castSpell(1, "Deadly Riposte", angel).error shouldBe null
                game.resolveStack()

                withClue("a 4/4 survives 3 damage") {
                    game.isOnBattlefield("Serra Angel") shouldBe true
                }
                withClue("but it is marked with 3 damage") {
                    game.state.getEntity(angel)?.get<DamageComponent>()?.amount shouldBe 3
                }
            }

            test("kills an attacking creature tapped by attacking") {
                // The real-game shape: the defender answers an attacker, which is tapped because
                // it attacked. Damage and life gain both have to land on this path too.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Deadly Riposte")
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withCardOnBattlefield(1, "Hill Giant", tapped = false, summoningSickness = false)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null

                val giant = game.findPermanent("Hill Giant")!!
                val lifeBefore = game.getLifeTotal(2)

                withClue("attacking tapped the Giant") {
                    game.state.getEntity(giant)?.get<TappedComponent>() shouldNotBe null
                }

                game.passPriority() // active player passes, defender gets priority
                game.castSpell(2, "Deadly Riposte", giant).error shouldBe null
                game.resolveStack()

                withClue("3 damage kills the attacking 3/3") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("the caster gains exactly 2 life") {
                    game.getLifeTotal(2) shouldBe lifeBefore + 2
                }
            }

            test("an untapped creature is not a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Deadly Riposte")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Hill Giant", tapped = false)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                withClue("'target tapped creature' rejects an untapped creature") {
                    game.castSpell(1, "Deadly Riposte", giant).error shouldNotBe null
                }
            }
        }
    }
}
