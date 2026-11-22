package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Wake the Reflections (DGM #10) — {W} Sorcery.
 * "Populate. (Create a token that's a copy of a creature token you control.)"
 *
 * Pins the populate choice: only creature tokens you control are offered (not a nontoken creature,
 * not an opponent's token), the chosen one is copied, and with no creature token the spell still
 * resolves and makes nothing.
 */
class WakeTheReflectionsScenarioTest : ScenarioTestBase() {

    private fun TestGame.tokens(name: String): List<EntityId> =
        findPermanents(name).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    init {
        test("offers only your creature tokens and copies the chosen one") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Wake the Reflections")
                .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                .withCardOnBattlefield(1, "Savannah Lions", isToken = true)
                .withCardOnBattlefield(1, "Centaur Courser") // nontoken: never a populate choice
                .withCardOnBattlefield(2, "Hill Giant", isToken = true) // opponent's token: not yours
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bearsToken = game.findPermanent("Grizzly Bears")!!
            val lionsToken = game.findPermanent("Savannah Lions")!!

            game.castSpell(1, "Wake the Reflections").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only player 1's creature tokens are populate choices") {
                decision.options shouldContainExactlyInAnyOrder listOf(bearsToken, lionsToken)
            }
            game.selectCards(listOf(lionsToken)).error shouldBe null
            game.resolveStack()

            withClue("the chosen Savannah Lions token was copied, nothing else") {
                game.tokens("Savannah Lions") shouldHaveSize 2
                game.tokens("Grizzly Bears") shouldHaveSize 1
                game.findPermanents("Centaur Courser") shouldHaveSize 1
                game.findPermanents("Hill Giant") shouldHaveSize 1
            }
            game.isInGraveyard(1, "Wake the Reflections") shouldBe true
        }

        test("no creature token you control — the spell resolves and makes nothing") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Wake the Reflections")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardOnBattlefield(2, "Hill Giant", isToken = true)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Wake the Reflections").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.findPermanents("Centaur Courser") shouldHaveSize 1
            game.findPermanents("Hill Giant") shouldHaveSize 1
            game.isInGraveyard(1, "Wake the Reflections") shouldBe true
        }
    }
}
