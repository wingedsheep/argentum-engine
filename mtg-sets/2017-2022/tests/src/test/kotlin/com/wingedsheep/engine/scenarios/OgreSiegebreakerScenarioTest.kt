package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m20.cards.OgreSiegebreaker
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Ogre Siegebreaker (M20 #215) — {2}{B}{R} Creature — Ogre Berserker, 4/3.
 *
 *   {2}{B}{R}: Destroy target creature that was dealt damage this turn.
 */
class OgreSiegebreakerScenarioTest : ScenarioTestBase() {

    private val abilityId = OgreSiegebreaker.activatedAbilities.first().id

    init {
        context("Ogre Siegebreaker") {

            test("destroys a creature that was dealt damage this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ogre Siegebreaker")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ogre = game.findPermanent("Ogre Siegebreaker")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Shock", giant).error shouldBe null
                game.resolveStack()
                withClue("2 damage doesn't kill the 3/3 on its own") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }

                game.execute(
                    ActivateAbility(game.player1Id, ogre, abilityId, listOf(ChosenTarget.Permanent(giant)))
                ).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }

            test("can't target a creature that wasn't dealt damage this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ogre Siegebreaker")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ogre = game.findPermanent("Ogre Siegebreaker")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.execute(
                    ActivateAbility(game.player1Id, ogre, abilityId, listOf(ChosenTarget.Permanent(giant)))
                ).error.shouldNotBeNull()
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }
    }
}
