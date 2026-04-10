package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * For the Common Good — {X}{X}{G}, Sorcery
 * Create X tokens that are copies of target token you control.
 */
class ForTheCommonGoodScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.createBeastTokenOnBattlefield(
        playerNumber: Int
    ): EntityId {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        val tokenId = EntityId.generate()
        val tokenCard = CardComponent(
            cardDefinitionId = "token:Beast",
            name = "Beast Token",
            manaCost = ManaCost.ZERO,
            typeLine = TypeLine.parse("Creature - Beast"),
            baseStats = CreatureStats(3, 3),
            colors = setOf(Color.GREEN),
            ownerId = playerId
        )
        val container = ComponentContainer.of(
            tokenCard,
            TokenComponent,
            ControllerComponent(playerId),
            SummoningSicknessComponent
        )
        state = state
            .withEntity(tokenId, container)
            .addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), tokenId)
        return tokenId
    }

    private fun ScenarioTestBase.TestGame.attachTo(
        attachmentName: String,
        creatureName: String
    ) {
        val attachmentId = findPermanent(attachmentName)!!
        val creatureId = findPermanent(creatureName)!!
        var attachmentContainer = state.getEntity(attachmentId)!!
        attachmentContainer = attachmentContainer.with(
            com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(creatureId)
        )
        state = state.withEntity(attachmentId, attachmentContainer)
        var creatureContainer = state.getEntity(creatureId)!!
        val existing = creatureContainer.get<
            com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
        >()
        val newAttachments = (existing?.attachedIds ?: emptyList()) + attachmentId
        creatureContainer = creatureContainer.with(
            com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent(newAttachments)
        )
        state = state.withEntity(creatureId, creatureContainer)
    }

    init {
        test("creates X copies that share the target token's power/toughness") {
            val game = scenario()
                .withPlayers("Player1", "Opponent")
                .withCardInHand(1, "For the Common Good")
                .withLandsOnBattlefield(1, "Forest", 5) // {X}{X}{G} with X=2 → {G}{G}{G}{G}{G}
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val beastTokenId = game.createBeastTokenOnBattlefield(1)

            // Before casting: one 3/3 Beast Token exists.
            game.findAllPermanents("Beast Token").size shouldBe 1

            val result = game.castXSpell(1, "For the Common Good", xValue = 2, targetId = beastTokenId)
            result.error shouldBe null
            game.resolveStack()

            // After resolving: the original + 2 new Beast Tokens, all 3/3 Beasts.
            val beasts = game.findAllPermanents("Beast Token")
            beasts.size shouldBe 3
            val newCopies = beasts.filter { it != beastTokenId }
            newCopies.size shouldBe 2
            newCopies.forEach { id ->
                val card = game.state.getEntity(id)!!.get<CardComponent>()!!
                card.baseStats shouldBe CreatureStats(3, 3)
                card.name shouldBe "Beast Token"
                game.state.getEntity(id)!!.has<TokenComponent>() shouldBe true
            }
        }

        test("copies an in-game Elemental Token created by Valduk") {
            val game = scenario()
                .withPlayers("Player1", "Opponent")
                .withCardInHand(1, "For the Common Good")
                .withCardOnBattlefield(1, "Valduk, Keeper of the Flame")
                .withCardOnBattlefield(1, "Short Sword")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.attachTo("Short Sword", "Valduk, Keeper of the Flame")

            // Advance to beginning of combat, triggering Valduk to create an Elemental Token
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()
            game.findAllPermanents("Elemental Token").size shouldBe 1
            val elementalId = game.findPermanent("Elemental Token")!!

            // Move to postcombat main phase to cast the sorcery
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            val result = game.castXSpell(
                playerNumber = 1,
                spellName = "For the Common Good",
                xValue = 2,
                targetId = elementalId
            )
            result.error shouldBe null
            game.resolveStack()

            val elementals = game.findAllPermanents("Elemental Token")
            elementals.size shouldBe 3
            val newCopies = elementals.filter { it != elementalId }
            newCopies.size shouldBe 2
            newCopies.forEach { id ->
                val card = game.state.getEntity(id)!!.get<CardComponent>()!!
                card.baseStats shouldBe CreatureStats(3, 1)
                card.name shouldBe "Elemental Token"
                game.state.getEntity(id)!!.has<TokenComponent>() shouldBe true
            }
        }
    }
}
