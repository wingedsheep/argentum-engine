package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Thundercloud Elemental.
 *
 * Card reference:
 * - Thundercloud Elemental: {5}{U}{U}
 *   Creature — Elemental
 *   3/4
 *   Flying
 *   {3}{U}: Tap all creatures with toughness 2 or less.
 *   {3}{U}: All other creatures lose flying until end of turn.
 */
class ThundercloudElementalScenarioTest : ScenarioTestBase() {

    private fun isTapped(game: TestGame, entityId: EntityId): Boolean =
        game.state.getEntity(entityId)?.get<TappedComponent>() != null

    init {
        context("Thundercloud Elemental tap ability") {
            test("taps all creatures with toughness 2 or less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thundercloud Elemental")
                    .withCardOnBattlefield(1, "Glory Seeker")     // 2/2 — should be tapped
                    .withCardOnBattlefield(2, "Elvish Warrior")   // 2/3 — should NOT be tapped
                    .withCardOnBattlefield(2, "Storm Crow")       // 1/2 — should be tapped
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elemental = game.findPermanent("Thundercloud Elemental")!!
                val glorySeeker = game.findPermanent("Glory Seeker")!!
                val elvishWarrior = game.findPermanent("Elvish Warrior")!!
                val stormCrow = game.findPermanent("Storm Crow")!!

                val cardDef = cardRegistry.getCard("Thundercloud Elemental")!!
                val tapAbility = cardDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = elemental,
                        abilityId = tapAbility.id
                    )
                )

                withClue("Ability should activate successfully") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Glory Seeker (2/2) should be tapped") {
                    isTapped(game, glorySeeker) shouldBe true
                }
                withClue("Storm Crow (1/2) should be tapped") {
                    isTapped(game, stormCrow) shouldBe true
                }
                withClue("Elvish Warrior (2/3) should NOT be tapped") {
                    isTapped(game, elvishWarrior) shouldBe false
                }
                withClue("Thundercloud Elemental (3/4) should NOT be tapped by its own ability") {
                    isTapped(game, elemental) shouldBe false
                }
            }
        }

        context("Thundercloud Elemental remove flying ability") {
            test("all other creatures lose flying until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thundercloud Elemental")
                    .withCardOnBattlefield(2, "Wind Drake")       // 2/2 flying
                    .withCardOnBattlefield(1, "Glory Seeker")     // 2/2 no flying
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elemental = game.findPermanent("Thundercloud Elemental")!!
                val windDrake = game.findPermanent("Wind Drake")!!

                val cardDef = cardRegistry.getCard("Thundercloud Elemental")!!
                val removeFlyingAbility = cardDef.script.activatedAbilities[1]

                // Verify Wind Drake has flying before activation
                val clientStateBefore = game.getClientState(1)
                val drakeBefore = clientStateBefore.cards[windDrake]
                withClue("Wind Drake should have flying before ability") {
                    drakeBefore shouldNotBe null
                    drakeBefore!!.keywords.contains(Keyword.FLYING) shouldBe true
                }

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = elemental,
                        abilityId = removeFlyingAbility.id
                    )
                )

                withClue("Ability should activate successfully") {
                    result.error shouldBe null
                }

                game.resolveStack()

                // Wind Drake should lose flying
                val clientStateAfter = game.getClientState(1)
                val drakeAfter = clientStateAfter.cards[windDrake]
                withClue("Wind Drake should lose flying after ability") {
                    drakeAfter shouldNotBe null
                    drakeAfter!!.keywords.contains(Keyword.FLYING) shouldBe false
                }

                // Thundercloud Elemental should keep flying (excluded by "other")
                val elementalAfter = clientStateAfter.cards[elemental]
                withClue("Thundercloud Elemental should still have flying") {
                    elementalAfter shouldNotBe null
                    elementalAfter!!.keywords.contains(Keyword.FLYING) shouldBe true
                }
            }
        }
    }
}
