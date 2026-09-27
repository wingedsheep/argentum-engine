package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Phyrexian Censor — "Each player can't cast more than one non-Phyrexian spell each turn.
 * Non-Phyrexian creatures enter tapped."
 *
 * The Censor sits on the *opponent's* side, so every test also proves the cap is global.
 */
class PhyrexianCensorScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(2, "Phyrexian Censor")
        .withCardsInHand(1, "Grizzly Bears", 2)
        .withCardInHand(1, "Bilious Skulldweller")
        .withCardInHand(1, "Pestilent Syphoner")
        .withLandsOnBattlefield(1, "Forest", 4)
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.offersCast(name: String): Boolean =
        getLegalActions(1).any { info ->
            val cast = info.action as? CastSpell ?: return@any false
            state.getEntity(cast.cardId)?.get<CardComponent>()?.name == name
        }

    private fun TestGame.isTapped(name: String): Boolean =
        state.getEntity(findPermanent(name)!!)?.has<TappedComponent>() == true

    init {
        test("a second non-Phyrexian spell is blocked, and non-Phyrexian creatures enter tapped") {
            val game = board()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.isTapped("Grizzly Bears") shouldBe true

            game.offersCast("Grizzly Bears") shouldBe false
            game.castSpell(1, "Grizzly Bears").error shouldNotBe null
        }

        test("Phyrexian spells are neither capped nor counted, and enter untapped") {
            val game = board()
            game.castSpell(1, "Bilious Skulldweller").error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Pestilent Syphoner").error shouldBe null
            game.resolveStack()
            game.isTapped("Bilious Skulldweller") shouldBe false
            game.isTapped("Pestilent Syphoner") shouldBe false

            game.offersCast("Grizzly Bears") shouldBe true
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("a non-Phyrexian spell cast before the Censor entered still counts") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardsInHand(1, "Grizzly Bears", 2)
                .withCardInHand(1, "Phyrexian Censor")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Phyrexian Censor").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Phyrexian Censor") shouldBe true

            game.offersCast("Grizzly Bears") shouldBe false
        }

        test("a Phyrexian spell is still castable after the non-Phyrexian one") {
            val game = board()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.offersCast("Bilious Skulldweller") shouldBe true
            game.castSpell(1, "Bilious Skulldweller").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Bilious Skulldweller") shouldBe true
        }
    }
}
