package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Kazuul's Fury // Kazuul's Cliffs (ZNR #146) — the first modal double-faced card whose front face
 * carries an additional cost: "As an additional cost to cast this spell, sacrifice a creature.
 * Kazuul's Fury deals damage equal to the sacrificed creature's power to any target."
 * Back face: "This land enters tapped. {T}: Add {R}."
 */
class KazuulsFuryScenarioTest : ScenarioTestBase() {

    private fun newGame() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Kazuul's Fury")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardOnBattlefield(1, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("casting the front face sacrifices a creature and deals damage equal to its power") {
            val game = newGame()
            val lifeBefore = game.getLifeTotal(2)

            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Kazuul's Fury").single(),
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    additionalCostPayment = AdditionalCostPayment(
                        sacrificedPermanents = listOf(game.findPermanent("Hill Giant")!!)
                    ),
                )
            ).error shouldBe null

            withClue("Hill Giant is sacrificed as the spell is cast") {
                game.isInGraveyard(1, "Hill Giant") shouldBe true
            }
            game.resolveStack()

            withClue("damage equals the sacrificed Hill Giant's power (3)") {
                game.getLifeTotal(2) shouldBe lifeBefore - 3
            }
            game.isInGraveyard(1, "Kazuul's Fury") shouldBe true
        }

        test("the back face is played as Kazuul's Cliffs and enters tapped") {
            val game = newGame()
            val cardId = game.findCardsInHand(1, "Kazuul's Fury").single()

            game.execute(PlayLand(game.player1Id, cardId, asBackFace = true)).error shouldBe null

            val land = game.findPermanent("Kazuul's Cliffs")
            land.shouldNotBeNull()
            withClue("Hill Giant is untouched: the land face has no additional cost") {
                game.findPermanent("Hill Giant").shouldNotBeNull()
            }
            game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
        }
    }
}
