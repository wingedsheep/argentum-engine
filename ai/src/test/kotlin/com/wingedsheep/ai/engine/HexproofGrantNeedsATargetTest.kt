package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * An end-of-turn **hexproof** grant is spent only by a spell or ability that targets the creature,
 * and an instant-speed granter can wait for one: the response window comes with it.
 *
 * Off a live misplay report (2026-11-07, Reality Fracture): on the human's turn, in the
 * declare-attackers step with an empty stack, the AI paid {1}, tapped Liliana the Faultless
 * ("{1}, {T}, Discard a card: Another target creature or planeswalker you control gains hexproof
 * until end of turn") and discarded a card to give its best creature hexproof. Nothing was aiming
 * at anything. The unspendable-grant floor read hexproof like a combat keyword, so a creature that
 * could still block counted as a use.
 */
class HexproofGrantNeedsATargetTest : ScenarioTestBase() {

    private val without = AiProfile.LIVE.copy(id = "live-without-unspendable", refuseUnspendableGrants = false)

    private fun TestGame.choice(profile: AiProfile = AiProfile.LIVE): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.activatesLiliana(action: GameAction): Boolean =
        (action as? ActivateAbility)?.let {
            state.getEntity(it.sourceId)?.get<CardComponent>()?.name == "Liliana the Faultless"
        } == true

    /** Our Liliana beside a Dragon, spare lands to pitch; the opponent's turn. */
    private fun position(opponentHand: List<String> = emptyList()) = scenario().withPlayers()
        .withTurnNumber(8)
        .withActivePlayer(2)
        .withCardOnBattlefield(1, "Liliana the Faultless")
        .withCardOnBattlefield(1, "Shivan Dragon")
        .withLandsOnBattlefield(1, "Plains", 6)
        .apply { repeat(3) { withCardInHand(1, "Plains") } }
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(2, "Swamp", 3)
        .apply { opponentHand.forEach { withCardInHand(2, it) } }
        .apply { repeat(5) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Swamp") } }
        .build()

    init {
        test("their attack declared, empty stack: Liliana is not activated") {
            val game = position().advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(DeclareAttackers(game.player2Id, mapOf(bears to game.player1Id))).error shouldBe null
            game.advanceToPriority(1, Step.DECLARE_ATTACKERS)

            val before = game.choice(without)
            withClue("reproduction: $before") { game.activatesLiliana(before) shouldBe true }

            val after = game.choice()
            withClue("chose $after") { game.activatesLiliana(after) shouldBe false }
        }

        test("their main phase, empty stack: Liliana is not activated") {
            val game = position().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            val action = game.choice()
            withClue("chose $action") { game.activatesLiliana(action) shouldBe false }
        }

        test("a removal spell aimed at the Dragon: Liliana answers it") {
            val game = position(opponentHand = listOf("Murder")).advanceToPriority(2, Step.PRECOMBAT_MAIN)
            game.castSpell(2, "Murder", game.findPermanent("Shivan Dragon")!!).error shouldBe null
            game.passPriority()

            val action = game.choice()
            withClue("chose $action") { game.activatesLiliana(action) shouldBe true }
        }
    }
}
