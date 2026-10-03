package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Avalanche Caller (KHM #45) — "{2}: Target snow land you control becomes a 4/4 Elemental creature
 * with hexproof and haste until end of turn. It's still a land."
 *
 * Only a *snow* land you control is a legal target; the animated land keeps its land type and its
 * snow supertype.
 */
class AvalancheCallerScenarioTest : ScenarioTestBase() {

    private val abilityId = cardRegistry.getCard("Avalanche Caller")!!.activatedAbilities.first().id

    private fun TestGame.activateOn(target: com.wingedsheep.sdk.model.EntityId) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Avalanche Caller")!!,
            abilityId = abilityId,
            targets = listOf(ChosenTarget.Permanent(target)),
        )
    )

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Avalanche Caller")
        .withLandsOnBattlefield(1, "Snow-Covered Island", 1)
        .withLandsOnBattlefield(1, "Island", 2)
        .withLandsOnBattlefield(2, "Snow-Covered Forest", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Avalanche Caller") {

            test("a snow land you control becomes a 4/4 Elemental with hexproof and haste, still a snow land") {
                val game = board()
                val snowIsland = game.findPermanent("Snow-Covered Island")!!

                game.activateOn(snowIsland).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("it is a 4/4 Elemental creature") {
                    projected.isCreature(snowIsland) shouldBe true
                    projected.getPower(snowIsland) shouldBe 4
                    projected.getToughness(snowIsland) shouldBe 4
                    projected.hasSubtype(snowIsland, "Elemental") shouldBe true
                }
                withClue("it has hexproof and haste") {
                    projected.hasKeyword(snowIsland, Keyword.HEXPROOF) shouldBe true
                    projected.hasKeyword(snowIsland, Keyword.HASTE) shouldBe true
                }
                withClue("it's still a snow land") {
                    projected.hasType(snowIsland, "LAND") shouldBe true
                    projected.hasType(snowIsland, "SNOW") shouldBe true
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("it reverts at end of turn") {
                    game.state.projectedState.isCreature(snowIsland) shouldBe false
                }
            }

            test("a nonsnow land can't be targeted") {
                val game = board()
                val island = game.findPermanents("Island").first()
                game.activateOn(island).error shouldNotBe null
            }

            test("an opponent's snow land can't be targeted") {
                val game = board()
                val theirs = game.findPermanent("Snow-Covered Forest")!!
                game.activateOn(theirs).error shouldNotBe null
            }
        }
    }
}
