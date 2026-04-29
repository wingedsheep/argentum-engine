package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mirrormind Crown's replacement effect should fire when a predefined token (Treasure,
 * Food, Lander, etc.) would be created — these tokens go through CreatePredefinedTokenEffect,
 * not the generic CreateTokenEffect, and were previously skipping the replacement check.
 */
class MirrormindCrownTreasureScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.attachTo(attachmentName: String, creatureName: String) {
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
        context("Mirrormind Crown + Reckless Ransacking (Treasure)") {
            test("Mirrormind triggers when a Treasure would be created and replaces it with a copy of the equipped creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reckless Ransacking")
                    .withCardOnBattlefield(1, "Burdened Stoneback")
                    .withCardOnBattlefield(1, "Mirrormind Crown")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attachTo("Mirrormind Crown", "Burdened Stoneback")

                val burdenedId = game.findPermanent("Burdened Stoneback")!!
                val castResult = game.castSpell(1, "Reckless Ransacking", burdenedId)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolving Reckless Ransacking should pause when it tries to create the Treasure.
                game.resolveStack()

                val pending = game.state.pendingDecision
                withClue("Mirrormind Crown should pause Treasure creation for a yes/no decision") {
                    pending shouldNotBe null
                    pending!!.context.sourceName shouldBe "Mirrormind Crown"
                }

                game.answerYesNo(true)
                if (game.state.stack.isNotEmpty()) game.resolveStack()

                val burdenedStonebacks = game.state.getBattlefield().filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Burdened Stoneback"
                }
                withClue("Should have original Burdened Stoneback + 1 token copy from Mirrormind") {
                    burdenedStonebacks.size shouldBe 2
                }

                val treasures = game.state.getBattlefield().filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Treasure"
                }
                withClue("No Treasure should hit the battlefield when Mirrormind replaces the token creation") {
                    treasures shouldHaveSize 0
                }

                val tokenId = burdenedStonebacks.first { it != burdenedId }
                withClue("Replacement permanent should be a token") {
                    game.state.getEntity(tokenId)!!.get<TokenComponent>() shouldBe TokenComponent
                }
            }

            test("declining Mirrormind's replacement creates the original Treasure token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reckless Ransacking")
                    .withCardOnBattlefield(1, "Burdened Stoneback")
                    .withCardOnBattlefield(1, "Mirrormind Crown")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attachTo("Mirrormind Crown", "Burdened Stoneback")

                val burdenedId = game.findPermanent("Burdened Stoneback")!!
                game.castSpell(1, "Reckless Ransacking", burdenedId)
                game.resolveStack()

                game.state.pendingDecision shouldNotBe null
                game.answerYesNo(false)
                if (game.state.stack.isNotEmpty()) game.resolveStack()

                val treasures = game.state.getBattlefield().filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Treasure"
                }
                withClue("Treasure should be created normally when player declines Mirrormind's replacement") {
                    treasures shouldHaveSize 1
                }

                val burdenedStonebacks = game.state.getBattlefield().filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Burdened Stoneback"
                }
                withClue("Only the original Burdened Stoneback should be on the battlefield — no Mirrormind copy") {
                    burdenedStonebacks.size shouldBe 1
                    burdenedStonebacks.first() shouldBe burdenedId
                }
            }
        }
    }
}
