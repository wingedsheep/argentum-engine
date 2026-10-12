package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Surge Engine (BRO #81).
 *
 *   Defender
 *   {U}: loses defender and gains "This creature can't be blocked."
 *   {2}{U}: becomes blue, base P/T 5/4. Activate only if it doesn't have defender.
 *   {4}{U}{U}: Draw three cards. Activate only if it is blue and only once.
 *
 * The gates read the source's projected keywords and colour, and the permanent-duration changes
 * from the earlier abilities are what open them.
 */
class SurgeEngineScenarioTest : ScenarioTestBase() {

    private val abilities = cardRegistry.getCard("Surge Engine")!!.activatedAbilities
    private val loseDefender = abilities[0].id
    private val becomeBlue = abilities[1].id
    private val drawThree = abilities[2].id

    private fun TestGame.activate(abilityId: com.wingedsheep.sdk.scripting.AbilityId) = execute(
        ActivateAbility(playerId = player1Id, sourceId = findPermanent("Surge Engine")!!, abilityId = abilityId)
    )

    init {
        context("Surge Engine") {

            test("each ability unlocks the next, and the draw is once only") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Surge Engine")
                    .withLandsOnBattlefield(1, "Island", 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) { builder.withCardInLibrary(1, "Island") }
                repeat(5) { builder.withCardInLibrary(2, "Island") }
                val game = builder.build()
                val engine = game.findPermanent("Surge Engine")!!

                withClue("with defender, the second and third abilities can't be activated") {
                    game.activate(becomeBlue).error shouldNotBe null
                    game.activate(drawThree).error shouldNotBe null
                }

                game.activate(loseDefender).error shouldBe null
                game.resolveStack()
                game.state.projectedState.hasKeyword(engine, Keyword.DEFENDER) shouldBe false
                game.state.projectedState.hasKeyword(engine, AbilityFlag.CANT_BE_BLOCKED) shouldBe true

                withClue("still colorless, so the draw is locked") {
                    game.activate(drawThree).error shouldNotBe null
                }

                game.activate(becomeBlue).error shouldBe null
                game.resolveStack()
                game.state.projectedState.hasColor(engine, Color.BLUE) shouldBe true
                game.state.projectedState.getPower(engine) shouldBe 5
                game.state.projectedState.getToughness(engine) shouldBe 4

                val handBefore = game.handSize(1)
                game.activate(drawThree).error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe handBefore + 3

                withClue("only once") {
                    game.activate(drawThree).error shouldNotBe null
                }
            }
        }
    }
}
