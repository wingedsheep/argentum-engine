package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.emn.cards.EmrakulsEvangel
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Emrakul's Evangel (EMN #156) — "{T}, Sacrifice this creature and any number of other
 * non-Eldrazi creatures: Create a 3/2 colorless Eldrazi Horror creature token for each creature
 * sacrificed this way."
 *
 * The token count is cost-linked and counts the Evangel itself (ruling 2016-07-13), which is paid
 * by a separate self-sacrifice cost from the variable "other creatures" one.
 */
class EmrakulsEvangelScenarioTest : ScenarioTestBase() {

    init {
        val abilityId = EmrakulsEvangel.activatedAbilities.single().id

        fun board() = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Emrakul's Evangel")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(1, "Lashweed Lurker")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.activate(picks: List<EntityId>) = execute(
            ActivateAbility(
                player1Id, findPermanent("Emrakul's Evangel")!!, abilityId,
                costPayment = AdditionalCostPayment(variableCostPermanents = picks)
            )
        )

        test("one 3/2 Eldrazi Horror per creature sacrificed, counting the Evangel") {
            val game = board()
            val result = game.activate(listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Hill Giant")!!))
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(1, "Emrakul's Evangel") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.findPermanent("Lashweed Lurker") shouldNotBe null

            val tokens = game.findAllPermanents("Eldrazi Horror Token")
            tokens.size shouldBe 3
            tokens.forEach {
                game.state.projectedState.getPower(it) shouldBe 3
                game.state.projectedState.getToughness(it) shouldBe 2
            }
        }

        test("sacrificing no other creatures still makes one token for the Evangel itself") {
            val game = board()
            val result = game.activate(emptyList())
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(1, "Emrakul's Evangel") shouldBe true
            game.findPermanent("Grizzly Bears") shouldNotBe null
            game.findAllPermanents("Eldrazi Horror Token").size shouldBe 1
        }

        test("an Eldrazi creature can't be sacrificed to the cost") {
            val game = board()
            val result = game.activate(listOf(game.findPermanent("Lashweed Lurker")!!))
            result.error shouldNotBe null
            game.findPermanent("Emrakul's Evangel") shouldNotBe null
            game.findPermanent("Lashweed Lurker") shouldNotBe null
        }
    }
}
