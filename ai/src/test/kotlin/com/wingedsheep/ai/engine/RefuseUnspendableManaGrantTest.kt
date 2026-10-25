package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.engine.knowledge.UnspendableManaGrant
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [AiProfile.refuseWastedPayoffs]' borrowed-mana check, as [AiProfile.LIVE] ships it.
 *
 * 2026-10-25 AI playtest, four-player game 1, turn 26: Piracy ("until end of turn, you may tap
 * lands you don't control for mana; spend this mana only to cast spells") cast in the precombat
 * main with only two Extinguish in hand and nothing on the stack to counter. Nothing else was cast.
 */
class RefuseUnspendableManaGrantTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val enumerator by lazy { LegalActionEnumerator.create(cardRegistry) }

    private fun TestGame.choice(profile: AiProfile = AiProfile.LIVE): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.castName(action: GameAction): String? =
        (action as? CastSpell)?.let { state.getEntity(it.cardId)?.get<CardComponent>()?.name }

    /** The verdict on Piracy, read against the position its cast resolves to. */
    private fun TestGame.piracyVerdict(refuseWastedPayoffs: Boolean = true): TimingVerdict {
        val before = state
        val card = before.getZone(player1Id, Zone.HAND).first {
            before.getEntity(it)?.get<CardComponent>()?.name == "Piracy"
        }
        castSpell(1, "Piracy")
        resolveStack()
        val leaf = state
        state = before
        return HoldPolicy(
            intents,
            refuseWastedPayoffs = refuseWastedPayoffs,
            legalActions = { s, p -> enumerator.enumerate(s, p, EnumerationMode.ACTIONS_ONLY) },
        ).verdictFor(before, player1Id, "Piracy", cast = CastSpell(player1Id, card), leaf = leaf)
    }

    private fun position(vararg alsoInHand: String) = scenario()
        .withPlayers()
        .withTurnNumber(5)
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInHand(1, "Piracy")
        .apply { alsoInHand.forEach { withCardInHand(1, it) } }
        .withLandsOnBattlefield(2, "Forest", 4)
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    init {
        test("T26: Piracy is not cast holding only counterspells with nothing to counter") {
            val game = position("Extinguish", "Extinguish")

            game.castName(game.choice()) shouldNotBe "Piracy"
            game.piracyVerdict() shouldBe TimingVerdict.NoWindow
            game.piracyVerdict(refuseWastedPayoffs = false) shouldNotBe TimingVerdict.NoWindow
        }

        test("with a spell only the borrowed mana pays for, Piracy is cast") {
            val game = position("Extinguish", "Grizzly Bears")

            game.piracyVerdict() shouldNotBe TimingVerdict.NoWindow
            game.castName(game.choice()) shouldBe "Piracy"
        }

        test("an instant copy of the grant cast in an opponent's upkeep is not refused") {
            val game = position("Extinguish", "Extinguish")
            // Test-local instant reuse of Piracy's shape: same spell effect, instant speed.
            val instantPiracy = cardRegistry.requireCard("Piracy").copy(typeLine = TypeLine.instant())
            val enumerate = { s: com.wingedsheep.engine.state.GameState, p: com.wingedsheep.sdk.model.EntityId ->
                enumerator.enumerate(s, p, EnumerationMode.ACTIONS_ONLY)
            }
            val ownMain = game.state
            game.castSpell(1, "Piracy")
            game.resolveStack()
            val leaf = game.state
            // Nothing castable afterwards: our own main phase, empty stack — refused.
            UnspendableManaGrant.holds(ownMain, game.player1Id, instantPiracy, leaf, enumerate) shouldBe true

            // Same hand and board, but cast in the opponent's upkeep with an empty stack: the borrowed
            // mana could still pay for a counter later that turn, so the grant is not refused.
            val theirUpkeep = ownMain.copy(
                activePlayerId = game.player2Id,
                phase = Phase.BEGINNING,
                step = Step.UPKEEP,
            )
            UnspendableManaGrant.holds(theirUpkeep, game.player1Id, instantPiracy, leaf, enumerate) shouldBe false
        }
    }
}
