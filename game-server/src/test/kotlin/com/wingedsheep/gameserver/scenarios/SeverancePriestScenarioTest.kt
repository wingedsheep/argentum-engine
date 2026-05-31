package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Severance Priest.
 *
 * Severance Priest: {W}{B}{G}
 * Creature — Djinn Cleric (3/3)
 * Deathtouch
 * When this creature enters, target opponent reveals their hand. You may choose a
 * nonland card from it. If you do, exile that card.
 * When this creature leaves the battlefield, the exiled card's owner creates an X/X
 * white Spirit creature token, where X is the mana value of the exiled card.
 *
 * The card is a pure pipeline composition — these tests pin the novel behavior:
 * the chosen card stays exiled (it is never returned), and the LTB payoff reads the
 * exiled card's mana value + owner to build the Spirit.
 */
class SeverancePriestScenarioTest : ScenarioTestBase() {

    init {
        context("Severance Priest") {

            test("ETB exiles a chosen nonland card from target opponent's hand") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Severance Priest")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(2, "Hill Giant") // {3}{R} → mana value 4
                    .withCardInHand(2, "Forest")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Severance Priest")
                game.resolveStack() // priest enters → ETB triggers
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack() // resolve ETB → reveal hand → pause for select

                // Controller picks the nonland Hill Giant to exile
                val giantId = game.findCardsInHand(2, "Hill Giant").first()
                game.selectCards(listOf(giantId))

                // Hill Giant should be in opponent's exile, linked to the priest
                game.state.getExile(game.player2Id).any { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Hill Giant"
                } shouldBe true
                game.isInHand(2, "Hill Giant") shouldBe false

                val priestId = game.findPermanent("Severance Priest")!!
                val linked = game.state.getEntity(priestId)?.get<LinkedExileComponent>()
                linked shouldNotBe null
                linked!!.exiledIds shouldHaveSize 1
                linked.exiledIds.first() shouldBe giantId

                // Forest (a land) must stay in opponent's hand — only nonland cards are eligible
                game.isInHand(2, "Forest") shouldBe true
            }

            test("LTB creates an X/X Spirit for the exiled card's owner, X = its mana value") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Severance Priest")
                    .withCardInHand(1, "End Hostilities") // {3}{W}{W} board wipe to kill the priest
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(2, "Hill Giant") // {3}{R} → mana value 4
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Severance Priest")
                game.resolveStack()
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                val giantId = game.findCardsInHand(2, "Hill Giant").first()
                game.selectCards(listOf(giantId))
                game.isInHand(2, "Hill Giant") shouldBe false

                // Player1 wipes the board, killing their own priest
                game.castSpell(1, "End Hostilities").error shouldBe null
                game.resolveStack() // End Hostilities resolves → priest dies → LTB → create Spirit

                game.isOnBattlefield("Severance Priest") shouldBe false

                // The exiled card stays exiled — Severance Priest never returns it
                game.state.getExile(game.player2Id).any { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Hill Giant"
                } shouldBe true

                // The Spirit token belongs to the exiled card's owner (the opponent)
                val tokenId = game.findPermanent("Spirit Token")
                tokenId.shouldNotBeNull()
                val token = game.state.getEntity(tokenId)!!
                val tokenCard = token.get<CardComponent>()!!
                tokenCard.colors shouldBe setOf(Color.WHITE)
                // X = Hill Giant's mana value (4)
                tokenCard.baseStats?.basePower shouldBe 4
                tokenCard.baseStats?.baseToughness shouldBe 4
                tokenCard.ownerId shouldBe game.player2Id
                token.get<ControllerComponent>()?.playerId shouldBe game.player2Id
                game.state.getBattlefield(game.player2Id).contains(tokenId) shouldBe true
            }

            test("no token is created when the controller exiles nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Severance Priest")
                    .withCardInHand(1, "End Hostilities")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(2, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Severance Priest")
                game.resolveStack()
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                // Decline by exiling no card
                game.selectCards(emptyList())
                game.isInHand(2, "Hill Giant") shouldBe true
                game.state.getExile(game.player2Id) shouldHaveSize 0

                game.castSpell(1, "End Hostilities").error shouldBe null
                game.resolveStack() // priest dies, but nothing was exiled

                game.isOnBattlefield("Severance Priest") shouldBe false
                game.findPermanent("Spirit Token") shouldBe null
            }
        }
    }
}
