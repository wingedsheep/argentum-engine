package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class IndebtedSpiritScenarioTest : ScenarioTestBase() {

    private fun TestGame.spiritTokens(): List<EntityId> =
        state.getBattlefield().filter { state.getEntity(it)?.has<TokenComponent>() == true }

    private fun TestGame.assertIsAfterlifeSpirit(token: EntityId) {
        val projected = state.projectedState
        withClue("1/1") {
            projected.getPower(token) shouldBe 1
            projected.getToughness(token) shouldBe 1
        }
        withClue("white and black") {
            projected.getColors(token) shouldHaveSize 2
            projected.hasColor(token, Color.WHITE) shouldBe true
            projected.hasColor(token, Color.BLACK) shouldBe true
        }
        withClue("flying") { projected.hasKeyword(token, Keyword.FLYING) shouldBe true }
        withClue("Spirit creature") {
            projected.isCreature(token) shouldBe true
            projected.hasSubtype(token, "Spirit") shouldBe true
        }
    }

    private fun TestGame.bestowOnto(host: EntityId) {
        execute(
            CastSpell(
                playerId = player1Id,
                cardId = findCardsInHand(1, "Indebted Spirit").single(),
                targets = listOf(ChosenTarget.Permanent(host)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.BESTOW
            )
        ).error shouldBe null
        resolveStack()
    }

    private fun bestowBoard(vararg extraHand: String, extraLands: Map<String, Int> = emptyMap()): TestGame {
        var builder = scenario().withPlayers()
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInHand(1, "Indebted Spirit")
            .withLandsOnBattlefield(1, "Plains", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        extraHand.forEach { builder = builder.withCardInHand(1, it) }
        extraLands.forEach { (land, count) -> builder = builder.withLandsOnBattlefield(1, land, count) }
        return builder.build()
    }

    init {
        test("cast as a creature for {W}, it dies and its afterlife makes one Spirit") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Indebted Spirit")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Indebted Spirit").error shouldBe null
            game.resolveStack()
            val spirit = game.findPermanent("Indebted Spirit")!!
            game.state.projectedState.isCreature(spirit) shouldBe true
            game.state.projectedState.getPower(spirit) shouldBe 1

            game.castSpell(1, "Lightning Bolt", spirit).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Indebted Spirit") shouldBe true
            val tokens = game.spiritTokens()
            tokens shouldHaveSize 1
            game.assertIsAfterlifeSpirit(tokens.single())
        }

        test("bestowed, the host gets +1/+1; the host dying to lethal damage makes one Spirit and the Aura becomes a creature") {
            val game = bestowBoard("Lightning Bolt", extraLands = mapOf("Mountain" to 1))
            val bears = game.findPermanent("Grizzly Bears")!!
            game.bestowOnto(bears)
            val spirit = game.findPermanent("Indebted Spirit")!!

            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
            game.state.projectedState.isCreature(spirit) shouldBe false

            // 3 damage to a 3/3 — it dies as a state-based action, not to a destroy effect.
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            val tokens = game.spiritTokens()
            withClue("only the granted afterlife fires — Indebted Spirit never left") { tokens shouldHaveSize 1 }
            game.assertIsAfterlifeSpirit(tokens.single())

            game.isOnBattlefield("Indebted Spirit") shouldBe true
            val projected = game.state.projectedState
            projected.isCreature(spirit) shouldBe true
            projected.getPower(spirit) shouldBe 1
            projected.getToughness(spirit) shouldBe 1
        }

        test("bestowed, the host destroyed by a destroy spell makes one Spirit and the Aura becomes a creature") {
            val game = bestowBoard("Murder", extraLands = mapOf("Swamp" to 3))
            val bears = game.findPermanent("Grizzly Bears")!!
            game.bestowOnto(bears)

            game.castSpell(1, "Murder", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            val tokens = game.spiritTokens()
            tokens shouldHaveSize 1
            game.assertIsAfterlifeSpirit(tokens.single())

            val spirit = game.findPermanent("Indebted Spirit")!!
            game.state.projectedState.isCreature(spirit) shouldBe true
        }

        test("bestowed, the host and Indebted Spirit destroyed together each fire an afterlife — two Spirits") {
            val game = bestowBoard("Akroma's Vengeance", extraLands = mapOf("Plains" to 6))
            val bears = game.findPermanent("Grizzly Bears")!!
            game.bestowOnto(bears)

            game.castSpell(1, "Akroma's Vengeance").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Indebted Spirit") shouldBe true
            val tokens = game.spiritTokens()
            tokens shouldHaveSize 2
            tokens.forEach { game.assertIsAfterlifeSpirit(it) }
        }

        test("bestowed, Indebted Spirit itself destroyed as an Aura fires its own afterlife and the host loses the bonus") {
            val game = bestowBoard("Disenchant", extraLands = mapOf("Plains" to 2))
            val bears = game.findPermanent("Grizzly Bears")!!
            game.bestowOnto(bears)
            val spirit = game.findPermanent("Indebted Spirit")!!
            game.state.projectedState.getPower(bears) shouldBe 3

            game.castSpell(1, "Disenchant", spirit).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Indebted Spirit") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            val tokens = game.spiritTokens()
            tokens shouldHaveSize 1
            game.assertIsAfterlifeSpirit(tokens.single())

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 2
            projected.getToughness(bears) shouldBe 2
        }
    }
}
