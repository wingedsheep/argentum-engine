package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Corpse Cobble (MID #214) — "As an additional cost to cast this spell, sacrifice any number of
 * creatures. Create an X/X blue and black Zombie creature token with menace, where X is the total
 * power of the sacrificed creatures. Flashback {3}{U}{B}"
 */
class CorpseCobbleScenarioTest : ScenarioTestBase() {

    private fun TestGame.castCobble(sacrificed: List<EntityId>) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, "Corpse Cobble").single(),
            additionalCostPayment = AdditionalCostPayment(variableCostPermanents = sacrificed),
        )
    )

    private fun board() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Corpse Cobble")
        .withLandsOnBattlefield(1, "Island", 1)
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Corpse Cobble") {

            test("the token's size is the total power of the sacrificed creatures") {
                val game = board()
                val sacrificed = listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Hill Giant")!!)
                game.castCobble(sacrificed).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                val zombie = game.findPermanent("Zombie Token").shouldNotBeNull()
                val projected = game.state.projectedState
                projected.getPower(zombie) shouldBe 5
                projected.getToughness(zombie) shouldBe 5
                projected.hasKeyword(zombie, Keyword.MENACE) shouldBe true
            }

            test("sacrificing nothing makes a 0/0 that dies") {
                val game = board()
                game.castCobble(emptyList()).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.findPermanent("Zombie Token") shouldBe null
            }
        }
    }
}
