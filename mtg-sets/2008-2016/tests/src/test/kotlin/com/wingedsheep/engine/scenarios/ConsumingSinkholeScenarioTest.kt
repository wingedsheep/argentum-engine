package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Consuming Sinkhole (OGW #94) — "Exile target land creature" / "deals 4 damage to target player or
 * planeswalker".
 *
 * Pins what the script can't show by itself: the land-creature target is the conjunction of both
 * types read off projected state, so a Mountain animated by Embodiment of Fury's landfall (a layer 4
 * type change) is a legal target, while a plain land or a plain creature is not.
 */
class ConsumingSinkholeScenarioTest : ScenarioTestBase() {

    private fun base() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Consuming Sinkhole")
        .withCardOnBattlefield(1, "Embodiment of Fury")
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withCardInHand(1, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun ScenarioTestBase.TestGame.castSinkhole(mode: Int, target: ChosenTarget) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = state.getHand(player1Id).first {
                state.getEntity(it)?.get<CardComponent>()?.name == "Consuming Sinkhole"
            },
            targets = listOf(target),
            chosenModes = listOf(mode),
            modeTargetsOrdered = listOf(listOf(target)),
        )
    )

    /** Plays the Mountain from hand and lets Embodiment of Fury animate [land]. */
    private fun ScenarioTestBase.TestGame.animate(land: EntityId) {
        val played = findCardsInHand(1, "Mountain").single()
        execute(PlayLand(player1Id, played)).error shouldBe null
        selectTargets(listOf(land)).error shouldBe null
        resolveStack()
        answerYesNo(true).error shouldBe null
        resolveStack()
        state.projectedState.isCreature(land) shouldBe true
    }

    init {
        test("exiles a land that is a creature only through an animation effect") {
            val game = base().build()
            val land = game.findPermanents("Mountain").first()
            game.animate(land)

            val cast = game.castSinkhole(0, ChosenTarget.Permanent(land))
            withClue("an animated land is a land creature: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("the animated Mountain is exiled") {
                game.state.getBattlefield().contains(land) shouldBe false
                game.isInExile(1, "Mountain") shouldBe true
            }
        }

        test("a land that isn't a creature is not a legal target") {
            val game = base().build()
            val land = game.findPermanents("Mountain").first()

            game.castSinkhole(0, ChosenTarget.Permanent(land)).error shouldNotBe null
        }

        test("a creature that isn't a land is not a legal target") {
            val game = base().build()
            val embodiment = game.findPermanent("Embodiment of Fury")!!

            game.castSinkhole(0, ChosenTarget.Permanent(embodiment)).error shouldNotBe null
        }

        test("the second mode deals 4 damage to target player") {
            val game = base().build()

            game.castSinkhole(1, ChosenTarget.Player(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 16
        }
    }
}
