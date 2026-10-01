package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.TheMycosynthGardens
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Mycosynth Gardens (ONE #256) — Land — Sphere.
 *
 * "{T}: Add {C}.
 *  {1}, {T}: Add one mana of any color.
 *  {X}, {T}: This land becomes a copy of target nontoken artifact you control with mana value X."
 */
class TheMycosynthGardensScenarioTest : ScenarioTestBase() {

    private val copyAbility = TheMycosynthGardens.activatedAbilities[2].id

    private fun TestGame.gardens(): EntityId = findPermanent("The Mycosynth Gardens")!!

    private fun TestGame.activateCopy(gardens: EntityId, x: Int, target: EntityId) =
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = gardens,
                abilityId = copyAbility,
                targets = listOf(ChosenTarget.Permanent(target)),
                xValue = x,
            )
        ).also { if (it.error == null && getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay() }

    private fun builder(): ScenarioBuilder {
        var b = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "The Mycosynth Gardens")
            .withCardOnBattlefield(1, "Mind Stone")
            .withCardOnBattlefield(2, "Ornithopter")
            .withLandsOnBattlefield(1, "Plains", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) { b = b.withCardInLibrary(1, "Plains") }
        repeat(5) { b = b.withCardInLibrary(2, "Plains") }
        return b
    }

    init {
        test("X = 2 makes it a permanent copy of Mind Stone — an artifact, no longer a land") {
            val game = builder().build()
            val gardens = game.gardens()
            val stone = game.findPermanent("Mind Stone")!!

            game.activateCopy(gardens, 2, stone).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("the land takes the artifact's copiable values") {
                game.state.getEntity(gardens)?.get<CardComponent>()?.name shouldBe "Mind Stone"
                projected.hasType(gardens, "ARTIFACT") shouldBe true
                projected.hasType(gardens, "LAND") shouldBe false
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("no duration — the copy persists past end of turn") {
                game.state.getEntity(gardens)?.get<CardComponent>()?.name shouldBe "Mind Stone"
            }
        }

        test("X must equal the artifact's mana value") {
            val game = builder().build()
            val gardens = game.gardens()
            val stone = game.findPermanent("Mind Stone")!!

            game.activateCopy(gardens, 1, stone).error shouldNotBe null
            game.state.getEntity(gardens)?.get<CardComponent>()?.name shouldBe "The Mycosynth Gardens"
        }

        test("an opponent's artifact is not a legal target") {
            val game = builder().build()
            val gardens = game.gardens()
            val thopter = game.findPermanent("Ornithopter")!!

            game.activateCopy(gardens, 0, thopter).error shouldNotBe null
            game.state.getEntity(gardens)?.get<CardComponent>()?.name shouldBe "The Mycosynth Gardens"
        }
    }
}
