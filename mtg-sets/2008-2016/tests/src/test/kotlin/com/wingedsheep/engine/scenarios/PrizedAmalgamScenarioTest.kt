package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Prized Amalgam (SOI #249): "Whenever a creature enters, if it entered from your graveyard or you
 * cast it from your graveyard, return this card from your graveyard to the battlefield tapped at
 * the beginning of the next end step."
 *
 * Exercises the graveyard-zone trigger on both provenance branches (reanimated / cast from the
 * graveyard), the owner-not-controller case (an opponent reanimating your creature card), the
 * negative cases (cast from hand, reanimated from an opponent's graveyard), and the delayed return
 * waiting for the end step and arriving tapped.
 */
class PrizedAmalgamScenarioTest : ScenarioTestBase() {

    private val amalgam = "Prized Amalgam"

    private fun TestGame.amalgamOnBattlefieldTapped(): Boolean {
        val id = findPermanent(amalgam) ?: return false
        return state.getEntity(id)?.has<TappedComponent>() == true
    }

    private fun TestGame.castVatEmergence(casterNumber: Int, graveyardOwnerNumber: Int) {
        val casterId = if (casterNumber == 1) player1Id else player2Id
        val ownerId = if (graveyardOwnerNumber == 1) player1Id else player2Id
        val bears = findCardsInGraveyard(graveyardOwnerNumber, "Grizzly Bears").single()
        val spell = state.getHand(casterId).single {
            state.getEntity(it)?.get<CardComponent>()?.name == "Vat Emergence"
        }
        execute(CastSpell(casterId, spell, listOf(ChosenTarget.Card(bears, ownerId, Zone.GRAVEYARD)))).error shouldBe null
        resolveStack()
    }

    init {
        test("a creature returned from your graveyard brings the Amalgam back tapped at the next end step") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInGraveyard(1, amalgam)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInHand(1, "Unearth")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.castSpellTargetingGraveyardCard(1, "Unearth", listOf(bears)).error shouldBe null
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldNotBe null
            withClue("the return waits for the end step") { game.isInGraveyard(1, amalgam) shouldBe true }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("the Amalgam returned to the battlefield tapped") { game.amalgamOnBattlefieldTapped() shouldBe true }
            game.isInGraveyard(1, amalgam) shouldBe false
        }

        test("a creature you cast from your graveyard brings the Amalgam back") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInGraveyard(1, amalgam)
                .withCardInGraveyard(1, "Squee, the Immortal")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpellFromGraveyard(1, "Squee, the Immortal").error shouldBe null
            game.resolveStack()

            game.findPermanent("Squee, the Immortal") shouldNotBe null
            game.isInGraveyard(1, amalgam) shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.amalgamOnBattlefieldTapped() shouldBe true
        }

        test("an opponent reanimating your creature card still counts: it entered from your graveyard") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInGraveyard(1, amalgam)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInHand(2, "Vat Emergence")
                .withLandsOnBattlefield(2, "Swamp", 5)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castVatEmergence(casterNumber = 2, graveyardOwnerNumber = 1)

            game.state.getBattlefield(game.player2Id).any {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
            } shouldBe true
            game.isInGraveyard(1, amalgam) shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("Alice's Amalgam returns under Alice's control") {
                val id = game.findPermanent(amalgam)
                id shouldNotBe null
                game.state.getBattlefield(game.player1Id).contains(id) shouldBe true
            }
            game.amalgamOnBattlefieldTapped() shouldBe true
        }

        test("a creature cast from hand doesn't trigger the Amalgam") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInGraveyard(1, amalgam)
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.state.stack.isEmpty() shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.findPermanent(amalgam) shouldBe null
            game.isInGraveyard(1, amalgam) shouldBe true
        }

        test("a creature reanimated from an opponent's graveyard doesn't trigger the Amalgam") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInGraveyard(1, amalgam)
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInHand(1, "Vat Emergence")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castVatEmergence(casterNumber = 1, graveyardOwnerNumber = 2)

            game.findPermanent("Grizzly Bears") shouldNotBe null

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("the Bears came from Bob's graveyard, not Alice's") {
                game.findPermanent(amalgam) shouldBe null
                game.isInGraveyard(1, amalgam) shouldBe true
            }
        }
    }
}
