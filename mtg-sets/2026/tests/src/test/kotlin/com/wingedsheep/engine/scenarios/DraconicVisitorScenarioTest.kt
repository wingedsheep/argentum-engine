package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Draconic Visitor (FRA #80) — {3}{R}{R} Creature — Dragon 5/5.
 *   Flying
 *   If one or more artifact tokens would be created under your control, that many 5/5 red Dragon
 *   creature tokens with flying are created instead.
 *
 * Pins each artifact-token path the replacement reads — a predefined Treasure, an artifact
 * creature token (Thopter), and a token copy made an artifact by its copy exception (Molten
 * Duplication, whose end-step sacrifice rider follows the Dragon, per the ruling) — plus the two scopes
 * that must stay untouched: non-artifact tokens, and artifact tokens created under an opponent's
 * control.
 */
class DraconicVisitorScenarioTest : ScenarioTestBase() {

    private fun TestGame.tokens(name: String, controller: EntityId = player1Id): List<EntityId> =
        state.getBattlefield().filter { id ->
            val c = state.getEntity(id) ?: return@filter false
            c.has<TokenComponent>() && c.get<CardComponent>()?.name == name &&
                c.get<ControllerComponent>()?.playerId == controller
        }

    private fun TestGame.dragonTokens(controller: EntityId = player1Id): List<EntityId> =
        state.getBattlefield().filter { id ->
            val c = state.getEntity(id) ?: return@filter false
            c.has<TokenComponent>() && c.get<ControllerComponent>()?.playerId == controller &&
                state.projectedState.hasSubtype(id, "Dragon")
        }

    private fun TestGame.castAndResolve(player: Int, name: String, target: EntityId? = null) {
        val cast = castSpell(player, name, target)
        withClue("casting $name: ${cast.error}") { cast.error shouldBe null }
        if (hasPendingDecision()) submitManaSourcesAutoPay()
        resolveStack()
        while (hasPendingDecision()) resolveStack()
    }

    private fun base(): ScenarioBuilder = scenario().withPlayers()
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")

    init {
        test("a Treasure is replaced by a 5/5 red flying Dragon") {
            val game = base()
                .withCardOnBattlefield(1, "Draconic Visitor")
                .withCardInHand(1, "Brazen Freebooter")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .build()

            game.castAndResolve(1, "Brazen Freebooter")

            game.tokens("Treasure").size shouldBe 0
            val dragons = game.dragonTokens()
            dragons.size shouldBe 1
            val dragon = dragons.single()
            val projected = game.state.projectedState
            projected.getPower(dragon) shouldBe 5
            projected.getToughness(dragon) shouldBe 5
            projected.hasKeyword(dragon, Keyword.FLYING) shouldBe true
            projected.hasColor(dragon, Color.RED) shouldBe true
            withClue("the Dragon is not an artifact") { projected.hasType(dragon, "ARTIFACT") shouldBe false }
        }

        test("an artifact creature token (Thopter) is replaced by a Dragon") {
            val game = base()
                .withCardOnBattlefield(1, "Draconic Visitor")
                .withCardInHand(1, "Gadget Technician")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .build()

            game.castAndResolve(1, "Gadget Technician")

            game.tokens("Thopter").size shouldBe 0
            game.dragonTokens().size shouldBe 1
        }

        test("a token copy made an artifact becomes a Dragon, which keeps the copy's sacrifice rider") {
            val game = base()
                .withCardOnBattlefield(1, "Draconic Visitor")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Molten Duplication")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castAndResolve(1, "Molten Duplication", bears)

            game.tokens("Grizzly Bears").size shouldBe 0
            game.dragonTokens().size shouldBe 1

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            withClue("\"Sacrifice it at the beginning of the next end step\" still applies to the Dragon") {
                game.dragonTokens().size shouldBe 0
            }
        }

        test("non-artifact tokens are created normally") {
            val game = base()
                .withCardOnBattlefield(1, "Draconic Visitor")
                .withCardInHand(1, "Raise the Alarm")
                .withLandsOnBattlefield(1, "Plains", 2)
                .build()

            game.castAndResolve(1, "Raise the Alarm")

            game.dragonTokens().size shouldBe 0
            game.state.getBattlefield().count { id ->
                game.state.getEntity(id)?.has<TokenComponent>() == true
            } shouldBe 2
        }

        test("an opponent's Visitor doesn't replace your artifact tokens") {
            val game = base()
                .withCardOnBattlefield(2, "Draconic Visitor")
                .withCardInHand(1, "Brazen Freebooter")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .build()

            game.castAndResolve(1, "Brazen Freebooter")

            game.tokens("Treasure").size shouldBe 1
            game.dragonTokens().size shouldBe 0
            game.dragonTokens(game.player2Id).size shouldBe 0
        }
    }
}
