package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Crowd Favorites.
 *
 * Card reference:
 * - Crowd Favorites: {6}{W} Creature — Human Soldier 4/4
 *   {3}{W}: Tap target creature.
 *   {3}{W}: Crowd Favorites gets +0/+5 until end of turn.
 */
class CrowdFavoritesScenarioTest : ScenarioTestBase() {

    init {
        context("Crowd Favorites tap ability") {
            test("taps target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Crowd Favorites")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crowdFavorites = game.findPermanent("Crowd Favorites")!!
                val glorySeekerTarget = game.findPermanent("Glory Seeker")!!

                val cardDef = cardRegistry.getCard("Crowd Favorites")!!
                val tapAbility = cardDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crowdFavorites,
                        abilityId = tapAbility.id,
                        targets = listOf(ChosenTarget.Permanent(glorySeekerTarget))
                    )
                )

                withClue("Tap ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Glory Seeker should be tapped") {
                    game.state.getEntity(glorySeekerTarget)?.has<TappedComponent>() shouldBe true
                }
            }
        }

        context("Crowd Favorites pump ability") {
            test("gives +0/+5 to itself until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Crowd Favorites")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crowdFavorites = game.findPermanent("Crowd Favorites")!!

                val cardDef = cardRegistry.getCard("Crowd Favorites")!!
                val pumpAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crowdFavorites,
                        abilityId = pumpAbility.id,
                        targets = emptyList()
                    )
                )

                withClue("Pump ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                val clientState = game.getClientState(1)
                val cardInfo = clientState.cards[crowdFavorites]
                withClue("Crowd Favorites should be 4/9 after pump") {
                    cardInfo shouldNotBe null
                    cardInfo!!.power shouldBe 4
                    cardInfo.toughness shouldBe 9
                }
            }

            test("pump stacks multiple times") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Crowd Favorites")
                    .withLandsOnBattlefield(1, "Plains", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crowdFavorites = game.findPermanent("Crowd Favorites")!!

                val cardDef = cardRegistry.getCard("Crowd Favorites")!!
                val pumpAbility = cardDef.script.activatedAbilities[1]

                // Activate pump twice
                val result1 = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crowdFavorites,
                        abilityId = pumpAbility.id,
                        targets = emptyList()
                    )
                )
                withClue("First pump should activate: ${result1.error}") {
                    result1.error shouldBe null
                }

                game.resolveStack()

                val result2 = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crowdFavorites,
                        abilityId = pumpAbility.id,
                        targets = emptyList()
                    )
                )
                withClue("Second pump should activate: ${result2.error}") {
                    result2.error shouldBe null
                }

                game.resolveStack()

                val clientState = game.getClientState(1)
                val cardInfo = clientState.cards[crowdFavorites]
                withClue("Crowd Favorites should be 4/14 after double pump") {
                    cardInfo shouldNotBe null
                    cardInfo!!.power shouldBe 4
                    cardInfo.toughness shouldBe 14
                }
            }
        }
    }
}
