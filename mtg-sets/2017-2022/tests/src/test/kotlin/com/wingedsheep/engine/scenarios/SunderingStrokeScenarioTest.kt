package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Sundering Stroke (ELD #144) — "deals 7 damage divided as you choose among one, two, or three
 * targets. If at least seven red mana was spent to cast this spell, instead Sundering Stroke deals 7
 * damage to each of those permanents and/or players."
 *
 * The division is announced as the spell is cast either way (it sits in the gate's `otherwise`
 * branch); with seven red spent the announced split is ignored and each target takes 7.
 */
class SunderingStrokeScenarioTest : ScenarioTestBase() {

    private fun game(secondLand: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Sundering Stroke")
        .withLandsOnBattlefield(1, "Mountain", 6)
        .withLandsOnBattlefield(1, secondLand, 1)
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.stroke(): EntityId =
        state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Sundering Stroke" }

    private fun TestGame.damage(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    private fun TestGame.castAtAllThree() {
        val giant = findPermanent("Hill Giant").shouldNotBeNull()
        val bears = findPermanent("Grizzly Bears").shouldNotBeNull()

        val offer = getLegalActions(1).first { it.description == "Cast Sundering Stroke" }
        offer.totalDamageToDistribute shouldBe 7

        val cast = execute(
            CastSpell(
                player1Id, stroke(),
                targets = listOf(
                    ChosenTarget.Player(player2Id), ChosenTarget.Permanent(giant), ChosenTarget.Permanent(bears),
                ),
                damageDistribution = mapOf(player2Id to 3, giant to 2, bears to 2),
            )
        )
        withClue("cast: ${cast.error}") { cast.error shouldBe null }
        resolveStack()
    }

    init {
        context("Sundering Stroke") {

            test("six red spent: deals the 7 damage as divided") {
                val game = game("Swamp")
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                game.castAtAllThree()

                game.getLifeTotal(2) shouldBe 17
                game.damage(giant) shouldBe 2
                game.isOnBattlefield("Hill Giant") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            test("seven red spent: the division is ignored and each target takes 7") {
                val game = game("Mountain")
                game.castAtAllThree()

                game.getLifeTotal(2) shouldBe 13
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
        }
    }
}
