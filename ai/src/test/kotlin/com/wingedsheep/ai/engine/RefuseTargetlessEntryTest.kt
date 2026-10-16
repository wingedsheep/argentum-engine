package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The cast half of [AiProfile.refuseWastedPayoffs], as [AiProfile.LIVE] ships it: a permanent whose
 * whole payoff is an entry trigger with nothing to target.
 *
 * Game `cfe8d858` (2026-10-15, Edge of Eternities sealed), turn 5: Banishing Light cast into a human
 * with no nonland permanent. Its exile trigger left the stack for want of a target, and the
 * enchantment did nothing all game — it was the AI's only answer to the Thrumming Hivepool that came
 * down later.
 */
class RefuseTargetlessEntryTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.choice(profile: AiProfile = AiProfile.LIVE): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.castName(action: GameAction): String? =
        (action as? CastSpell)?.let { state.getEntity(it.cardId)?.get<CardComponent>()?.name }

    private fun TestGame.verdictForBanishingLight(refuseWastedPayoffs: Boolean): TimingVerdict {
        val card = state.getZone(player1Id, Zone.HAND).first {
            state.getEntity(it)?.get<CardComponent>()?.name == "Banishing Light"
        }
        return HoldPolicy(intents, refuseWastedPayoffs = refuseWastedPayoffs)
            .verdictFor(state, player1Id, "Banishing Light", cast = CastSpell(player1Id, card))
    }

    private fun position(theirCreature: String? = null) = scenario()
        .withPlayers()
        .withTurnNumber(5)
        .withLandsOnBattlefield(1, "Plains", 3)
        .withCardInHand(1, "Banishing Light")
        .withLandsOnBattlefield(2, "Island", 3)
        .apply { if (theirCreature != null) withCardOnBattlefield(2, theirCreature) }
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    init {
        test("T5: Banishing Light is not cast into a board with nothing to exile") {
            val game = position()

            game.verdictForBanishingLight(refuseWastedPayoffs = true) shouldBe TimingVerdict.NoWindow
            game.verdictForBanishingLight(refuseWastedPayoffs = false) shouldBe TimingVerdict.Neutral
            game.castName(game.choice()) shouldNotBe "Banishing Light"
        }

        test("with a creature to exile it is an ordinary removal spell again") {
            val game = position(theirCreature = "Craw Wurm")

            game.verdictForBanishingLight(refuseWastedPayoffs = true) shouldNotBe TimingVerdict.NoWindow
            game.castName(game.choice()) shouldBe "Banishing Light"
        }
    }
}
