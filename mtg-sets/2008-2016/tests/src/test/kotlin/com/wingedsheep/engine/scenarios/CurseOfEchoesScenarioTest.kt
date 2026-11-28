package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dka.cards.CurseOfEchoes
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Curse of Echoes (DKA #34) — {4}{U} Enchantment — Aura Curse.
 *
 * "Enchant player. Whenever enchanted player casts an instant or sorcery spell, each other player
 * may copy that spell and may choose new targets for the copy they control."
 *
 * The may and the copy belong to each *other* player — here the Curse's controller — never to the
 * caster, and the copier picks the copy's targets.
 */
class CurseOfEchoesScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(CurseOfEchoes))

        /** The Curse on player 1's battlefield, enchanting player 2; each player holds a Bolt. */
        fun cursed(activePlayer: Int = 2): TestGame {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Curse of Echoes")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(activePlayer)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val curse = game.findPermanent("Curse of Echoes")!!
            game.state = game.state.updateEntity(curse) { it.with(AttachedToComponent(game.player2Id)) }
            return game
        }

        fun TestGame.cast(playerNumber: Int, target: Int) {
            castSpellTargetingPlayer(playerNumber, "Lightning Bolt", target).error shouldBe null
            if (getPendingDecision() != null && getPendingDecision() !is YesNoDecision) submitManaSourcesAutoPay()
            resolveStack()
        }

        test("the other player may copy the enchanted player's spell and aim the copy back at them") {
            val game = cursed()
            game.cast(2, target = 1)

            val ask = game.getPendingDecision()
            withClue("the Curse's controller, not the caster, is asked whether to copy") {
                (ask is YesNoDecision) shouldBe true
                ask!!.playerId shouldBe game.player1Id
            }
            game.answerYesNo(true).error shouldBe null

            val retarget = game.getPendingDecision()
            withClue("the copier chooses the copy's new targets") {
                (retarget is ChooseTargetsDecision) shouldBe true
                retarget!!.playerId shouldBe game.player1Id
            }
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            withClue("the copy hit the caster and the original hit the Curse's controller") {
                game.getLifeTotal(2) shouldBe 17
                game.getLifeTotal(1) shouldBe 17
            }
        }

        test("declining makes no copy") {
            val game = cursed()
            game.cast(2, target = 1)
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 17
            game.getLifeTotal(2) shouldBe 20
        }

        test("spells cast by a player the Curse does not enchant are not copied") {
            val game = cursed(activePlayer = 1)
            game.cast(1, target = 2)

            withClue("no copy prompt for anyone") {
                game.getPendingDecision() shouldBe null
            }
            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
