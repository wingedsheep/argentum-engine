package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Skyclave Apparition — the ETB exiles a *target permanent* linked to the Apparition, and the
 * LTB reads that linked card back for the Illusion's size (its mana value) and controller (its
 * owner — even when someone else controlled the permanent it was exiled from).
 */
class SkyclaveApparitionScenarioTest : ScenarioTestBase() {

    init {
        context("Skyclave Apparition") {

            fun setup() = scenario()
                .withPlayers("Player1", "Opponent")
                .withCardInHand(1, "Skyclave Apparition")
                .withCardInHand(1, "End Hostilities")
                .withLandsOnBattlefield(1, "Plains", 8)
                .withCardOnBattlefield(2, "Hill Giant") // {3}{R} -> mana value 4
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("exiles the target, then gives its owner an X/X blue Illusion when the Apparition leaves") {
                val game = setup()
                val giantId = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Skyclave Apparition")
                game.resolveStack()
                game.selectTargets(listOf(giantId))
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe false
                game.state.getExile(game.player2Id).contains(giantId) shouldBe true
                val apparitionId = game.findPermanent("Skyclave Apparition")!!
                game.state.getEntity(apparitionId)?.get<LinkedExileComponent>()?.exiledIds shouldBe listOf(giantId)

                game.castSpell(1, "End Hostilities").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Skyclave Apparition") shouldBe false
                // The exiled card stays in exile.
                game.state.getExile(game.player2Id).contains(giantId) shouldBe true

                val tokenId = game.findPermanent("Illusion Token")
                tokenId.shouldNotBeNull()
                val token = game.state.getEntity(tokenId)!!
                val tokenCard = token.get<CardComponent>()!!
                tokenCard.colors shouldBe setOf(Color.BLUE)
                tokenCard.baseStats?.basePower shouldBe 4
                tokenCard.baseStats?.baseToughness shouldBe 4
                token.get<ControllerComponent>()?.playerId shouldBe game.player2Id
            }

            test("a stolen permanent's Illusion goes to its owner, not the player who controlled it") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Skyclave Apparition")
                    .withCardInHand(1, "End Hostilities")
                    .withLandsOnBattlefield(1, "Plains", 8)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giantId = game.findPermanent("Hill Giant")!!
                // Player1 owns the Giant; the opponent has stolen it.
                game.state = game.state.updateEntity(giantId) { it.with(ControllerComponent(game.player2Id)) }

                game.castSpell(1, "Skyclave Apparition")
                game.resolveStack()
                game.selectTargets(listOf(giantId))
                game.resolveStack()
                game.state.getExile(game.player1Id).contains(giantId) shouldBe true

                game.castSpell(1, "End Hostilities").error shouldBe null
                game.resolveStack()

                val tokenId = game.findPermanent("Illusion Token")
                tokenId.shouldNotBeNull()
                game.state.getEntity(tokenId)!!.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            }

            test("choosing no target exiles nothing and makes no token") {
                val game = setup()

                game.castSpell(1, "Skyclave Apparition")
                game.resolveStack()
                game.selectTargets(emptyList())
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe true

                game.castSpell(1, "End Hostilities").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Skyclave Apparition") shouldBe false
                game.findPermanent("Illusion Token") shouldBe null
            }
        }
    }
}
