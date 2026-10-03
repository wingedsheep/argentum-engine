package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Muster the Departed — {2}{W} Enchantment.
 * When this enchantment enters, create a 1/1 white Spirit creature token with flying.
 * Morbid — At the beginning of your end step, if a creature died this turn, populate.
 */
class MusterTheDepartedScenarioTest : ScenarioTestBase() {

    private fun TestGame.tokens(): List<EntityId> =
        state.getBattlefield().filter { state.getEntity(it)?.has<TokenComponent>() == true }

    private fun TestGame.assertIsFlyingSpirit(token: EntityId) {
        val projected = state.projectedState
        projected.isCreature(token) shouldBe true
        projected.hasSubtype(token, "Spirit") shouldBe true
        projected.getPower(token) shouldBe 1
        projected.getToughness(token) shouldBe 1
        projected.hasColor(token, Color.WHITE) shouldBe true
        projected.hasKeyword(token, Keyword.FLYING) shouldBe true
    }

    init {
        test("enters and makes a Spirit; a creature dying lets the end step populate it") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Muster the Departed")
                .withCardInHand(1, "Lightning Bolt")
                .withCardOnBattlefield(1, "Grizzly Bears") // nontoken creature: never a populate choice
                .withCardOnBattlefield(2, "Savannah Lions") // an opponent's creature dying still counts
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Muster the Departed").error shouldBe null
            game.resolveStack()

            withClue("the enters trigger makes one 1/1 flying Spirit") {
                game.tokens() shouldHaveSize 1
                game.assertIsFlyingSpirit(game.tokens().single())
            }

            val lions = game.findPermanent("Savannah Lions")!!
            game.castSpell(1, "Lightning Bolt", lions).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Savannah Lions") shouldBe false

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("populate copied the Spirit token, not the Grizzly Bears") {
                val tokens = game.tokens()
                tokens shouldHaveSize 2
                tokens.forEach { game.assertIsFlyingSpirit(it) }
                game.findPermanents("Grizzly Bears") shouldHaveSize 1
            }
        }

        test("no creature died this turn — the morbid trigger does nothing") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Muster the Departed")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Muster the Departed").error shouldBe null
            game.resolveStack()
            game.tokens() shouldHaveSize 1

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.tokens() shouldHaveSize 1
        }

        test("a creature died but you control no creature token — populate makes nothing") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Muster the Departed") // placed directly: no enters trigger
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lions = game.findPermanent("Savannah Lions")!!
            game.castSpell(1, "Lightning Bolt", lions).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.tokens() shouldHaveSize 0
            game.findPermanents("Grizzly Bears") shouldHaveSize 1
        }
    }
}
