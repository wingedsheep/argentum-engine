package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The opponent uses what its board can do to a spell aimed at it
 * ([AiProfile.anticipateTargetResponses]): a removal spell is scored with the opponent's visible
 * answer — here Liliana the Faultless ("{1}, {T}, Discard a card: Another target creature or
 * planeswalker you control gains hexproof until end of turn") — already used against it.
 *
 * Off game `74644ce0` turn 16 (2026-11-04, Reality Fracture draft): the AI held Prophesied End and
 * Surgical Precision against Liliana, Kiora of Fire and Ashes and a 5/5 Dragon. It aimed Prophesied
 * End at Kiora, the human gave Kiora hexproof in response, and both spells were wasted — Surgical
 * Precision on the Dragon only resolved because Liliana was by then tapped. Killing Liliana first
 * leaves nothing to protect the Dragon from the second spell.
 */
class AnticipateTargetResponsesTest : ScenarioTestBase() {

    private val without = AiProfile.LIVE.copy(id = "live-without-target-responses", anticipateTargetResponses = false)

    private fun TestGame.choice(profile: AiProfile = AiProfile.LIVE): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.describe(action: GameAction): String {
        val cast = action as? CastSpell ?: return action.toString()
        val target = cast.targets.firstOrNull() as? ChosenTarget.Permanent
        return "${nameOf(cast.cardId)} -> ${target?.let { nameOf(it.entityId) }}"
    }

    private fun TestGame.nameOf(id: com.wingedsheep.sdk.model.EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name

    private fun TestGame.target(action: GameAction): String? =
        ((action as? CastSpell)?.targets?.firstOrNull() as? ChosenTarget.Permanent)?.let { nameOf(it.entityId) }

    private fun position(lilianaTapped: Boolean = false) = scenario().withPlayers()
        .withTurnNumber(16)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInHand(1, "Prophesied End")
        .withCardInHand(1, "Surgical Precision")
        .withCardOnBattlefield(2, "Liliana the Faultless", tapped = lilianaTapped)
        .withCardOnBattlefield(2, "Kiora of Fire and Ashes")
        .withCardOnBattlefield(2, "Shivan Dragon")
        .withLandsOnBattlefield(2, "Plains", 3)
        .withCardInHand(2, "Grizzly Bears")
        // Prophesied End draws its target's controller a card: an empty library would end the game.
        .apply { repeat(5) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Plains") } }
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    init {
        test("T16: removal goes at the hexproof granter first, not at what it can protect") {
            val game = position()

            val before = game.choice(without)
            withClue("reproduction: ${game.describe(before)}") {
                game.target(before) shouldNotBe "Liliana the Faultless"
            }

            val after = game.choice()
            withClue("chose ${game.describe(after)}") {
                game.target(after) shouldBe "Liliana the Faultless"
            }
        }

        test("T16 played out: Liliana first, then the Dragon with nothing left to protect it") {
            val game = position()
            val ai = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
            repeat(20) {
                if (game.state.step != Step.PRECOMBAT_MAIN) return@repeat
                val decision = game.state.pendingDecision
                if (decision != null) {
                    game.execute(SubmitDecision(decision.playerId, ai.respondToDecision(game.state, decision))).error shouldBe null
                    return@repeat
                }
                val priority = game.state.priorityPlayerId ?: return@repeat
                game.execute(if (priority == game.player1Id) ai.chooseAction(game.state) else PassPriority(priority)).error shouldBe null
            }
            game.findPermanent("Liliana the Faultless") shouldBe null
            game.findPermanent("Shivan Dragon") shouldBe null
        }

        test("with the granter tapped, the removal goes at the biggest threat") {
            val game = position(lilianaTapped = true)

            val after = game.choice()
            withClue("chose ${game.describe(after)}") {
                game.target(after) shouldNotBe "Liliana the Faultless"
                (after is CastSpell) shouldBe true
            }
        }
    }
}
