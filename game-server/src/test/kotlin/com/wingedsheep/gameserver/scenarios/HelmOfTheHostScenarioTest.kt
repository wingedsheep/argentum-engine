package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Helm of the Host.
 *
 * Card reference:
 * - Helm of the Host ({4}): Legendary Artifact — Equipment
 *   At the beginning of combat on your turn, create a token that's a copy of equipped
 *   creature, except the token isn't legendary. That token gains haste.
 *   Equip {5}
 */
class HelmOfTheHostScenarioTest : ScenarioTestBase() {

    /**
     * Attach an equipment that's already on the battlefield to a creature.
     */
    private fun TestGame.attachTo(attachmentName: String, creatureName: String) {
        val attachmentId = findPermanent(attachmentName)!!
        val creatureId = findPermanent(creatureName)!!

        var attachmentContainer = state.getEntity(attachmentId)!!
        attachmentContainer = attachmentContainer.with(AttachedToComponent(creatureId))
        state = state.withEntity(attachmentId, attachmentContainer)

        var creatureContainer = state.getEntity(creatureId)!!
        val existing = creatureContainer.get<AttachmentsComponent>()
        val newAttachments = (existing?.attachedIds ?: emptyList()) + attachmentId
        creatureContainer = creatureContainer.with(AttachmentsComponent(newAttachments))
        state = state.withEntity(creatureId, creatureContainer)
    }

    init {
        context("Helm of the Host") {

            test("creates a token copy of equipped creature at beginning of combat") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Helm of the Host")
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Manually attach Helm to Serra Angel
                game.attachTo("Helm of the Host", "Serra Angel")

                // Advance to beginning of combat
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                // Resolve the triggered ability
                game.resolveStack()

                // Check for token copy
                val p1Battlefield = game.state.getBattlefield(game.player1Id)
                val serraAngels = p1Battlefield.filter { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Serra Angel"
                }
                withClue("Should have original + token copy of Serra Angel") {
                    serraAngels.size shouldBe 2
                }

                // The token should be marked as a token
                val tokenAngel = serraAngels.find { entityId ->
                    game.state.getEntity(entityId)?.has<TokenComponent>() == true
                }
                tokenAngel shouldNotBe null

                // The token should have haste
                val tokenCard = game.state.getEntity(tokenAngel!!)?.get<CardComponent>()
                tokenCard?.baseKeywords?.contains(Keyword.HASTE) shouldBe true
            }

            test("token copy is not legendary") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Helm of the Host")
                    .withCardOnBattlefield(1, "Lyra Dawnbringer") // Legendary creature
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Attach Helm to Lyra
                game.attachTo("Helm of the Host", "Lyra Dawnbringer")

                // Advance to beginning of combat
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                // Check for token copy
                val p1Battlefield = game.state.getBattlefield(game.player1Id)
                val lyras = p1Battlefield.filter { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Lyra Dawnbringer"
                }
                withClue("Should have original + non-legendary token copy") {
                    lyras.size shouldBe 2
                }

                // The token should NOT be legendary
                val tokenLyra = lyras.find { entityId ->
                    game.state.getEntity(entityId)?.has<TokenComponent>() == true
                }
                tokenLyra shouldNotBe null
                val tokenCard = game.state.getEntity(tokenLyra!!)?.get<CardComponent>()
                tokenCard?.typeLine?.isLegendary shouldBe false
            }

            test("does nothing if Helm is not equipped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Helm of the Host") // Not equipped
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Pass to combat — Helm triggers but does nothing
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                // Should only have original Serra Angel
                val p1Battlefield = game.state.getBattlefield(game.player1Id)
                val serraAngels = p1Battlefield.filter { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Serra Angel"
                }
                serraAngels.size shouldBe 1
            }
        }
    }
}
