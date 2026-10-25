package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [AiProfile.refuseWastedPayoffs]' end-of-combat sweep check, as [AiProfile.LIVE] ships it.
 *
 * 2026-10-25 AI playtest, four-player game 1, turn 27: the active player cast Wall of Caltrops and
 * then Glyph of Doom ("destroy all creatures that were blocked by that creature this turn") on it in
 * its own precombat main. The Wall can't block on its controller's turn, so the Glyph was a card
 * thrown away.
 */
class RefuseIdleBlockWatcherTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.choice(profile: AiProfile = AiProfile.LIVE): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.castName(action: GameAction): String? =
        (action as? CastSpell)?.let { state.getEntity(it.cardId)?.get<CardComponent>()?.name }

    private fun TestGame.glyphVerdict(refuseWastedPayoffs: Boolean = true): TimingVerdict {
        val card = state.getZone(player1Id, Zone.HAND).first {
            state.getEntity(it)?.get<CardComponent>()?.name == "Glyph of Doom"
        }
        val wall = findPermanent("Wall of Caltrops")!!
        return HoldPolicy(intents, refuseWastedPayoffs = refuseWastedPayoffs).verdictFor(
            state, player1Id, "Glyph of Doom",
            cast = CastSpell(player1Id, card, targets = listOf(ChosenTarget.Permanent(wall))),
        )
    }

    private fun board() = scenario()
        .withPlayers()
        .withTurnNumber(5)
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardOnBattlefield(1, "Wall of Caltrops")
        .withCardInHand(1, "Glyph of Doom")
        .withLandsOnBattlefield(2, "Forest", 6)
        .withCardOnBattlefield(2, "Craw Wurm")

    init {
        test("T27: Glyph of Doom is not cast on our own Wall in our own main phase") {
            val game = board().build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.castName(game.choice()) shouldNotBe "Glyph of Doom"
            game.glyphVerdict() shouldBe TimingVerdict.NoWindow
            game.glyphVerdict(refuseWastedPayoffs = false) shouldNotBe TimingVerdict.NoWindow
        }

        // Whether the Glyph then beats passing is the evaluator's call; this reader only has to
        // stay out of the window where the sweep has something to destroy.
        test("on their turn, once our Wall has blocked, the Glyph is not refused") {
            val game = board().withActivePlayer(2).build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Craw Wurm" to 1))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Wall of Caltrops" to listOf("Craw Wurm")))
            game.advanceToPriority(1, Step.DECLARE_BLOCKERS)

            game.glyphVerdict() shouldNotBe TimingVerdict.NoWindow
        }

        test("on their turn, once blockers are in without our Wall, the Glyph is refused") {
            val game = board().withActivePlayer(2).build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Craw Wurm" to 1))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.advanceToPriority(1, Step.DECLARE_BLOCKERS)

            game.glyphVerdict() shouldBe TimingVerdict.NoWindow
        }
    }
}
