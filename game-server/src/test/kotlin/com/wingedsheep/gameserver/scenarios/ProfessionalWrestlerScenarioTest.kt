package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Professional Wrestler.
 *
 * Card reference:
 * - Professional Wrestler ({3}{G}): Creature — Human Warrior Performer, 4/4
 *   "When this creature enters, create a Treasure token."
 *   "This creature can't be blocked by more than one creature."
 */
class ProfessionalWrestlerScenarioTest : ScenarioTestBase() {

    init {
        context("Professional Wrestler — cast and ETB") {

            test("enters the battlefield as a 4/4 Human Warrior Performer when cast for {3}{G}") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Professional Wrestler")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Professional Wrestler")
                withClue("Casting Professional Wrestler with {3}{G} should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Professional Wrestler should be on the battlefield") {
                    game.isOnBattlefield("Professional Wrestler") shouldBe true
                }

                val permanentId = game.findPermanent("Professional Wrestler")!!
                val card = game.state.getEntity(permanentId)!!.get<CardComponent>()!!

                withClue("Professional Wrestler should be a 4/4") {
                    card.baseStats?.basePower shouldBe 4
                    card.baseStats?.baseToughness shouldBe 4
                }

                withClue("Professional Wrestler should be a Human") {
                    card.typeLine.hasSubtype(Subtype.HUMAN) shouldBe true
                }
                withClue("Professional Wrestler should be a Warrior") {
                    card.typeLine.hasSubtype(Subtype.WARRIOR) shouldBe true
                }
                withClue("Professional Wrestler should be a Performer") {
                    card.typeLine.hasSubtype(Subtype("Performer")) shouldBe true
                }

                withClue("Professional Wrestler should be green") {
                    card.colors shouldBe setOf(Color.GREEN)
                }

                withClue("Professional Wrestler should no longer be in the active player's hand") {
                    game.isInHand(1, "Professional Wrestler") shouldBe false
                }

                withClue("All four lands should be tapped after paying {3}{G}") {
                    val untappedLands = game.state.getBattlefield().count { entityId ->
                        val container = game.state.getEntity(entityId) ?: return@count false
                        val c = container.get<CardComponent>()
                        val isLand = c?.typeLine?.isLand == true
                        isLand && !container.has<TappedComponent>()
                    }
                    untappedLands shouldBe 0
                }

                withClue("ETB trigger should have created exactly one Treasure token") {
                    game.findAllPermanents("Treasure").size shouldBe 1
                }
            }
        }
    }
}
