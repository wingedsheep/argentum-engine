package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rootborn Defenses (RTR #19) — {2}{W} Instant.
 * "Populate. Creatures you control gain indestructible until end of turn."
 *
 * Pins the ordering: the indestructible grant gathers its creatures only after the populate choice
 * (a paused decision) resolves, so the freshly created token copy gains indestructible too.
 */
class RootbornDefensesScenarioTest : ScenarioTestBase() {

    private fun TestGame.tokens(name: String): List<EntityId> =
        findPermanents(name).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    init {
        test("populates, then every creature you control — the new token included — gains indestructible") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Rootborn Defenses")
                .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                .withCardOnBattlefield(1, "Savannah Lions", isToken = true)
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lionsToken = game.findPermanent("Savannah Lions")!!

            game.castSpell(1, "Rootborn Defenses").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(lionsToken)).error shouldBe null
            game.resolveStack()

            val lions = game.tokens("Savannah Lions")
            lions shouldHaveSize 2
            val projected = game.state.projectedState
            withClue("both Lions tokens, including the populated copy, are indestructible") {
                lions.forEach { projected.hasKeyword(it, Keyword.INDESTRUCTIBLE) shouldBe true }
            }
            projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.INDESTRUCTIBLE) shouldBe true
            projected.hasKeyword(game.findPermanent("Centaur Courser")!!, Keyword.INDESTRUCTIBLE) shouldBe true
            withClue("an opponent's creature is untouched") {
                projected.hasKeyword(game.findPermanent("Hill Giant")!!, Keyword.INDESTRUCTIBLE) shouldBe false
            }
            game.isInGraveyard(1, "Rootborn Defenses") shouldBe true
        }
    }
}
