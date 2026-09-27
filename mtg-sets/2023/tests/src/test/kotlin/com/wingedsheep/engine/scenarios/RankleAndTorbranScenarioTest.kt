package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rankle and Torbran (MOM #252) — "Flying, first strike, haste. Whenever Rankle and Torbran deals
 * combat damage to a player or battle, choose any number — • Each player creates a Treasure token.
 * • Each player sacrifices a creature of their choice. • If a source would deal damage to a player
 * or battle this turn, it deals that much damage plus 2 instead."
 *
 * Rankle and Torbran has first strike, so its trigger resolves before regular combat damage: a
 * Grizzly Bears attacking alongside it is the first source the third mode amplifies.
 */
class RankleAndTorbranScenarioTest : ScenarioTestBase() {

    private fun board(vararg extra: Pair<Int, String>) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Rankle and Torbran", summoningSickness = false)
        .apply { extra.forEach { (player, name) -> withCardOnBattlefield(player, name, summoningSickness = false) } }
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /**
     * Picks the modes whose description contains any of [modes], then declines the rest. A
     * sacrifice choice is answered with [sacrifice] for the player who has it among the options.
     */
    private fun TestGame.resolveTrigger(modes: List<String>, sacrifice: String? = null) {
        val remaining = modes.toMutableList()
        var guard = 0
        while (guard++ < 30) {
            val decision = state.pendingDecision
            when {
                decision is ChooseOptionDecision -> {
                    val wanted = remaining.firstOrNull { m -> decision.options.any { it.contains(m) } }
                    val index = if (wanted != null) {
                        remaining.remove(wanted)
                        decision.options.indexOfFirst { it.contains(wanted) }
                    } else {
                        decision.options.indexOfFirst { it.contains("Don't choose", ignoreCase = true) }
                    }
                    submitDecision(OptionChosenResponse(decision.id, optionIndex = index)).error shouldBe null
                }
                decision is SelectCardsDecision -> {
                    val pick = decision.options.filter {
                        state.getEntity(it)?.get<CardComponent>()?.name == sacrifice
                    }.ifEmpty { decision.options.take(decision.minSelections) }
                    selectCards(pick).error shouldBe null
                }
                decision != null -> error("unexpected decision: $decision")
                state.stack.isNotEmpty() -> resolveStack()
                else -> return
            }
        }
        error("did not settle")
    }

    private fun TestGame.attackAndHitWithRankle(attackers: Map<String, Int>) {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(attackers).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.FIRST_STRIKE_COMBAT_DAMAGE)
    }

    private fun TestGame.treasuresControlledBy(playerNumber: Int): Int {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return findAllPermanents("Treasure").count {
            state.getEntity(it)?.get<ControllerComponent>()?.playerId == playerId
        }
    }

    init {
        test("the damage mode adds 2 to later damage dealt to a player this turn") {
            val game = board(1 to "Grizzly Bears")
            game.attackAndHitWithRankle(mapOf("Rankle and Torbran" to 2, "Grizzly Bears" to 2))
            withClue("first-strike damage lands before the trigger") { game.getLifeTotal(2) shouldBe 17 }

            game.resolveTrigger(listOf("plus 2"))
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            withClue("Grizzly Bears deals 2 + 2") { game.getLifeTotal(2) shouldBe 13 }
            withClue("no other mode was chosen") {
                game.findAllPermanents("Treasure").size shouldBe 0
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        test("the damage mode also adds 2 to damage dealt to a battle") {
            val game = board(1 to "Grizzly Bears", 1 to "Invasion of Innistrad")
            game.checkStateBasedActions()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                playerAttackers = mapOf("Rankle and Torbran" to 2),
                permanentAttackers = mapOf("Grizzly Bears" to "Invasion of Innistrad"),
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.FIRST_STRIKE_COMBAT_DAMAGE)
            game.resolveTrigger(listOf("plus 2"))
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            val siege = game.findPermanent("Invasion of Innistrad")!!
            withClue("Grizzly Bears removed 2 + 2 of the Siege's 5 defense counters") {
                game.state.getEntity(siege)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE) shouldBe 1
            }
        }

        test("choosing no modes does nothing") {
            val game = board(1 to "Grizzly Bears", 2 to "Hill Giant")
            game.attackAndHitWithRankle(mapOf("Rankle and Torbran" to 2, "Grizzly Bears" to 2))
            game.resolveTrigger(emptyList())
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 15
            game.findAllPermanents("Treasure").size shouldBe 0
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("treasure and sacrifice modes affect each player") {
            val game = board(1 to "Grizzly Bears", 2 to "Hill Giant")
            game.attackAndHitWithRankle(mapOf("Rankle and Torbran" to 2))
            game.resolveTrigger(listOf("Treasure", "sacrifices"), sacrifice = "Grizzly Bears")

            withClue("each player created a Treasure") {
                game.treasuresControlledBy(1) shouldBe 1
                game.treasuresControlledBy(2) shouldBe 1
            }
            withClue("each player sacrificed a creature of their choice") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Rankle and Torbran") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe false
            }
        }
    }
}
