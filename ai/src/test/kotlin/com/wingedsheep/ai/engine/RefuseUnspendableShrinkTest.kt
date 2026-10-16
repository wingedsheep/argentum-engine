package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The activation half of [AiProfile.refuseWastedPayoffs], as [AiProfile.LIVE] ships it: an
 * end-of-turn shrink on a creature that has no fight left this turn.
 *
 * Game `3f847e6d` (2026-10-15), turn 8: Proft, Sinister Mastermind ("{B}, Discard this card: Target
 * creature gets -3/-1 until end of turn") discarded from hand after the AI declared no attack, to
 * shrink an opposing 2/4 that the cleanup step restored.
 */
class RefuseUnspendableShrinkTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private val proftAbility by lazy {
        intents.definition("Proft, Sinister Mastermind")!!.script.activatedAbilities.single().id
    }

    private fun TestGame.choice(profile: AiProfile = AiProfile.LIVE): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.proftInHand() = state.getZone(player1Id, Zone.HAND).first {
        state.getEntity(it)?.get<CardComponent>()?.name == "Proft, Sinister Mastermind"
    }

    private fun TestGame.shrinkVerdict(target: String, refuseWastedPayoffs: Boolean = true): TimingVerdict =
        HoldPolicy(intents, refuseWastedPayoffs = refuseWastedPayoffs).verdictFor(
            state, player1Id, "Proft, Sinister Mastermind",
            activation = ActivateAbility(
                player1Id, proftInHand(), proftAbility,
                targets = listOf(ChosenTarget.Permanent(findPermanent(target)!!)),
            ),
        )

    private fun position(activePlayer: Int, step: Step, theirCreature: String = "Serra Angel") = scenario()
        .withPlayers()
        .withTurnNumber(8)
        .withActivePlayer(activePlayer)
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInHand(1, "Proft, Sinister Mastermind")
        .withCardOnBattlefield(2, theirCreature)
        .build()
        .advanceToPriority(1, step)

    init {
        test("T8: after our combat, a shrink that kills nothing has no fight to change") {
            val game = position(activePlayer = 1, step = Step.POSTCOMBAT_MAIN)

            game.shrinkVerdict("Serra Angel") shouldBe TimingVerdict.NoWindow
            game.shrinkVerdict("Serra Angel", refuseWastedPayoffs = false) shouldBe TimingVerdict.Neutral
            (game.choice() as? ActivateAbility)?.sourceId shouldNotBe game.proftInHand()
        }

        test("a shrink that kills its target is removal, and stays the leaf's call") {
            val game = position(activePlayer = 1, step = Step.POSTCOMBAT_MAIN, theirCreature = "Raging Goblin")

            game.shrinkVerdict("Raging Goblin") shouldNotBe TimingVerdict.NoWindow
        }

        test("before their attack, a creature that can still attack is a fight to shrink") {
            val game = position(activePlayer = 2, step = Step.PRECOMBAT_MAIN)

            game.shrinkVerdict("Serra Angel") shouldNotBe TimingVerdict.NoWindow
        }
    }
}
