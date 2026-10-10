package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * The Balrog of Moria (LTC #46) — {4}{B}{B}{R} Legendary Creature — Avatar Demon 8/8.
 *
 *   Trample, haste
 *   When The Balrog of Moria dies, you may exile it. When you do, for each opponent, exile up to
 *   one target creature that player controls.
 *   Cycling {3}{R}
 *   When you cycle this card, create two Treasure tokens.
 */
class TheBalrogOfMoriaScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.balrogBoard(): ScenarioBuilder = this
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "The Balrog of Moria")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Craw Wurm")
        .withCardInHand(1, "Murder")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    /** Kill the Balrog with Murder and walk its dies trigger. Returns the offered reflexive targets. */
    private fun TestGame.killBalrog(exile: Boolean, choose: (List<EntityId>) -> List<EntityId>): List<EntityId>? {
        val balrog = findPermanent("The Balrog of Moria")!!
        castSpell(1, "Murder", targetId = balrog).error shouldBe null
        var offered: List<EntityId>? = null
        var guard = 0
        while (guard++ < 40) {
            when (val decision = state.pendingDecision) {
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                is YesNoDecision -> answerYesNo(exile)
                is ChooseTargetsDecision -> {
                    offered = decision.legalTargets[0] ?: emptyList()
                    selectTargets(choose(offered))
                }
                null -> {
                    if (state.stack.isEmpty()) return offered
                    resolveStack()
                }
                else -> error("unexpected decision: $decision")
            }
        }
        error("decision loop did not settle")
    }

    init {
        test("dies, exile it — then exile a target creature the opponent controls") {
            val game = scenario().balrogBoard().build()
            val wurm = game.findPermanent("Craw Wurm")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            val offered = game.killBalrog(exile = true) { listOf(wurm) }

            withClue("only the opponent's creatures are offered") {
                offered!! shouldContain wurm
                offered shouldNotContain bears
            }
            game.isInExile(1, "The Balrog of Moria") shouldBe true
            game.isInExile(2, "Craw Wurm") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("up to one — choosing no target exiles nothing but the Balrog") {
            val game = scenario().balrogBoard().build()

            game.killBalrog(exile = true) { emptyList() }

            game.isInExile(1, "The Balrog of Moria") shouldBe true
            game.isOnBattlefield("Craw Wurm") shouldBe true
        }

        test("declining the exile leaves the Balrog in the graveyard and fires no reflexive trigger") {
            val game = scenario().balrogBoard().build()

            val offered = game.killBalrog(exile = false) { error("no reflexive target prompt expected") }

            offered shouldBe null
            game.isInGraveyard(1, "The Balrog of Moria") shouldBe true
            game.isOnBattlefield("Craw Wurm") shouldBe true
        }

        test("cycling draws a card and creates two Treasure tokens") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "The Balrog of Moria")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cycleCard(1, "The Balrog of Moria").error shouldBe null
            var guard = 0
            while (guard++ < 20) {
                when (game.state.pendingDecision) {
                    is SelectManaSourcesDecision -> game.submitManaSourcesAutoPay()
                    null -> if (game.state.stack.isEmpty()) break else game.resolveStack()
                    else -> error("unexpected decision: ${game.state.pendingDecision}")
                }
            }

            game.isInGraveyard(1, "The Balrog of Moria") shouldBe true
            game.isInHand(1, "Plains") shouldBe true
            game.findPermanents("Treasure").size shouldBe 2
        }
    }
}
