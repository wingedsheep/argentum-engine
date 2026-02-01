package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lorwyn.LorwynEclipsedSet
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for the Indestructible keyword.
 *
 * Indestructible (Rule 702.12):
 * - Permanents with indestructible can't be destroyed.
 * - Such permanents aren't destroyed by lethal damage (SBA 704.5g).
 * - They still receive damage, which can be relevant for effects that
 *   care about damage dealt.
 */
class IndestructibleScenarioTest : ScenarioTestBase() {

    init {
        // Register Lorwyn Eclipsed cards (includes Adept Watershaper that grants indestructible)
        cardRegistry.register(LorwynEclipsedSet.allCards)

        context("Indestructible prevents destruction by lethal damage") {

            test("Creature with indestructible survives lethal combat damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Adept Watershaper")  // Grants indestructible to other tapped creatures
                    .withCardOnBattlefield(1, "Devoted Hero")  // 1/2 that will receive indestructible
                    .withCardOnBattlefield(2, "Goblin Bully")  // 2/1 attacker
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val devotedHeroId = game.findPermanent("Devoted Hero")!!

                // Tap Devoted Hero to give it indestructible
                game.state = game.state.updateEntity(devotedHeroId) { container ->
                    container.with(TappedComponent)
                }

                // Simulate lethal damage (3 damage to a 1/2)
                game.state = game.state.updateEntity(devotedHeroId) { container ->
                    container.with(DamageComponent(3))
                }

                // Run state-based actions
                val sbaChecker = StateBasedActionChecker()
                val sbaResult = sbaChecker.checkAndApply(game.state)
                game.state = sbaResult.newState

                // Devoted Hero should survive because it has indestructible
                withClue("Indestructible creature should survive lethal damage") {
                    game.isOnBattlefield("Devoted Hero") shouldBe true
                }
            }

            test("Creature without indestructible dies to lethal damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Adept Watershaper")  // Grants indestructible to other tapped creatures
                    .withCardOnBattlefield(1, "Devoted Hero")  // 1/2 NOT tapped, so no indestructible
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val devotedHeroId = game.findPermanent("Devoted Hero")!!

                // Devoted Hero is NOT tapped, so it doesn't have indestructible

                // Deal lethal damage (3 damage to a 1/2)
                game.state = game.state.updateEntity(devotedHeroId) { container ->
                    container.with(DamageComponent(3))
                }

                // Run state-based actions
                val sbaChecker = StateBasedActionChecker()
                val sbaResult = sbaChecker.checkAndApply(game.state)
                game.state = sbaResult.newState

                // Devoted Hero should die because it doesn't have indestructible
                withClue("Creature without indestructible should die to lethal damage") {
                    game.isOnBattlefield("Devoted Hero") shouldBe false
                }
            }
        }

        context("Indestructible prevents destroy effects") {

            test("Creature with indestructible survives Path of Peace") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Adept Watershaper")  // Grants indestructible to other tapped creatures
                    .withCardOnBattlefield(1, "Devoted Hero", tapped = true)  // Tapped = has indestructible
                    .withCardInHand(2, "Path of Peace")  // Destroy target creature
                    .withLandsOnBattlefield(2, "Plains", 4)  // {3}{W} for Path of Peace
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val devotedHeroId = game.findPermanent("Devoted Hero")!!

                // Cast Path of Peace targeting Devoted Hero
                val castResult = game.castSpell(2, "Path of Peace", targetId = devotedHeroId)
                withClue("Casting Path of Peace should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Devoted Hero should survive because it has indestructible
                withClue("Indestructible creature should survive destroy effect") {
                    game.isOnBattlefield("Devoted Hero") shouldBe true
                }
            }

            test("Creature without indestructible is destroyed by Path of Peace") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Devoted Hero")  // No indestructible granter
                    .withCardInHand(2, "Path of Peace")  // Destroy target creature
                    .withLandsOnBattlefield(2, "Plains", 4)  // {3}{W} for Path of Peace
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val devotedHeroId = game.findPermanent("Devoted Hero")!!

                // Cast Path of Peace targeting Devoted Hero
                val castResult = game.castSpell(2, "Path of Peace", targetId = devotedHeroId)
                withClue("Casting Path of Peace should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Devoted Hero should be destroyed
                withClue("Creature without indestructible should be destroyed") {
                    game.isOnBattlefield("Devoted Hero") shouldBe false
                }
            }
        }

        context("Indestructible granting and removal") {

            test("Untapping creature removes indestructible and allows destruction") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Adept Watershaper")  // Grants indestructible to other tapped creatures
                    .withCardOnBattlefield(1, "Devoted Hero", tapped = true)  // Tapped = has indestructible
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val devotedHeroId = game.findPermanent("Devoted Hero")!!

                // Deal damage while tapped (has indestructible)
                game.state = game.state.updateEntity(devotedHeroId) { container ->
                    container.with(DamageComponent(3))
                }

                // Run SBA - should survive
                val sbaChecker = StateBasedActionChecker()
                var sbaResult = sbaChecker.checkAndApply(game.state)
                game.state = sbaResult.newState

                withClue("Should survive with indestructible while tapped") {
                    game.isOnBattlefield("Devoted Hero") shouldBe true
                }

                // Untap the creature (loses indestructible)
                game.state = game.state.updateEntity(devotedHeroId) { container ->
                    container.without<TappedComponent>()
                }

                // Run SBA again - should die now
                sbaResult = sbaChecker.checkAndApply(game.state)
                game.state = sbaResult.newState

                withClue("Should die after losing indestructible when untapped") {
                    game.isOnBattlefield("Devoted Hero") shouldBe false
                }
            }

            test("Indestructible is checked at time of destruction attempt") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Adept Watershaper")  // Grants indestructible to other tapped creatures
                    .withCardOnBattlefield(1, "Devoted Hero")  // Initially NOT tapped
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val devotedHeroId = game.findPermanent("Devoted Hero")!!

                // Deal lethal damage while NOT tapped (no indestructible yet)
                game.state = game.state.updateEntity(devotedHeroId) { container ->
                    container.with(DamageComponent(3))
                }

                // Now tap the creature before SBA are checked
                game.state = game.state.updateEntity(devotedHeroId) { container ->
                    container.with(TappedComponent)
                }

                // Run SBA - creature now has indestructible and should survive
                val sbaChecker = StateBasedActionChecker()
                val sbaResult = sbaChecker.checkAndApply(game.state)
                game.state = sbaResult.newState

                withClue("Tapping before SBA should grant indestructible and prevent death") {
                    game.isOnBattlefield("Devoted Hero") shouldBe true
                }
            }
        }

        context("Indestructible still receives damage") {

            test("Indestructible creature still has damage marked on it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Adept Watershaper")
                    .withCardOnBattlefield(1, "Devoted Hero", tapped = true)  // Has indestructible
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val devotedHeroId = game.findPermanent("Devoted Hero")!!

                // Deal damage
                game.state = game.state.updateEntity(devotedHeroId) { container ->
                    container.with(DamageComponent(5))
                }

                // Run SBA
                val sbaChecker = StateBasedActionChecker()
                val sbaResult = sbaChecker.checkAndApply(game.state)
                game.state = sbaResult.newState

                // Creature survives
                withClue("Indestructible creature should survive") {
                    game.isOnBattlefield("Devoted Hero") shouldBe true
                }

                // But damage is still marked
                val damageComponent = game.state.getEntity(devotedHeroId)?.get<DamageComponent>()
                withClue("Damage should still be marked on indestructible creature") {
                    damageComponent shouldNotBe null
                    damageComponent!!.amount shouldBe 5
                }
            }
        }
    }
}
