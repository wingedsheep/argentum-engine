package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.Deck
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
        test("three players: each other player copies in turn order from the active player, and the copies resolve first") {
            val d = GameTestDriver()
            fun GameTestDriver.topSpellController() =
                state.getEntity(getTopOfStack()!!)?.get<SpellOnStackComponent>()?.casterId
            d.registerCards(TestCards.all + listOf(CurseOfEchoes))
            // p[1] is the enchanted player and the active player; p[0] controls the Curse.
            val p = d.initMultiplayer(List(3) { Deck.of("Mountain" to 40) }, startingPlayer = 1)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            d.activePlayer shouldBe p[1]
            val curse = d.putPermanentOnBattlefield(p[0], "Curse of Echoes")
            d.replaceState(d.state.updateEntity(curse) { it.with(AttachedToComponent(p[1])) })

            val bolt = d.putCardInHand(p[1], "Lightning Bolt")
            d.giveMana(p[1], Color.RED, 1)
            d.castSpell(p[1], bolt, targets = listOf(p[0])).error shouldBe null
            var guard = 0
            while (d.pendingDecision == null && d.state.stack.size > 1 && guard++ < 20) {
                d.passPriority(d.state.priorityPlayerId!!)
            }

            // APNAP from the active player p[1], skipping the caster: p[2] first, then p[0].
            for (copier in listOf(p[2], p[0])) {
                val ask = d.pendingDecision
                withClue("the next may-copy prompt belongs to ${'$'}copier") {
                    (ask is YesNoDecision) shouldBe true
                    ask!!.playerId shouldBe copier
                }
                d.submitYesNo(copier, true).error shouldBe null
                val retarget = d.pendingDecision
                withClue("the copier picks the copy's targets") {
                    (retarget is ChooseTargetsDecision) shouldBe true
                    retarget!!.playerId shouldBe copier
                }
                d.submitTargetSelection(copier, listOf(p[1])).error shouldBe null
            }
            d.pendingDecision shouldBe null

            withClue("two copies above the original; the last copier's copy is on top") {
                d.state.stack.size shouldBe 3
                d.topSpellController() shouldBe p[0]
            }

            fun resolveTop() {
                val size = d.state.stack.size
                var g = 0
                while (d.state.stack.size == size && g++ < 10) d.passPriority(d.state.priorityPlayerId!!)
            }

            resolveTop()
            withClue("p[0]'s copy resolves first, hitting the caster") {
                d.getLifeTotal(p[1]) shouldBe 17
                d.getLifeTotal(p[0]) shouldBe 20
                d.topSpellController() shouldBe p[2]
            }
            resolveTop()
            withClue("p[2]'s copy resolves next; the original still waits") {
                d.getLifeTotal(p[1]) shouldBe 14
                d.getLifeTotal(p[0]) shouldBe 20
                d.topSpellController() shouldBe p[1]
            }
            resolveTop()
            withClue("the original resolves last") {
                d.state.stack.size shouldBe 0
                d.getLifeTotal(p[0]) shouldBe 17
                d.getLifeTotal(p[2]) shouldBe 20
            }
        }
    }
}
