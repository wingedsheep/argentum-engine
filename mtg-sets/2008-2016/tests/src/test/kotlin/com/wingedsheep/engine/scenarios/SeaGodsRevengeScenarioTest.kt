package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sea God's Revenge — {5}{U} Sorcery (THS #61).
 *
 * "Return up to three target creatures your opponents control to their owners' hands. Scry 1."
 */
class SeaGodsRevengeScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Sea God's Revenge")
        .withLandsOnBattlefield(1, "Island", 6)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Savannah Lions")
        .withCardOnBattlefield(2, "Raging Goblin")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("bounces three opponent creatures, then scries 1") {
            val game = board()
            val targets = listOf("Grizzly Bears", "Savannah Lions", "Raging Goblin")
                .map { ChosenTarget.Permanent(game.findPermanent(it)!!) }
            val spell = game.findCardsInHand(1, "Sea God's Revenge").single()
            game.execute(CastSpell(game.player1Id, spell, targets)).error shouldBe null
            game.resolveStack()

            val top = game.state.getLibrary(game.player1Id).first()
            val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("scry 1 looks at the top card") { scry.options shouldBe listOf(top) }
            game.selectCards(listOf(top)).error shouldBe null
            game.resolveStack()

            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.isInHand(2, "Savannah Lions") shouldBe true
            game.isInHand(2, "Raging Goblin") shouldBe true
            withClue("the caster's own creature stays") { game.isOnBattlefield("Hill Giant") shouldBe true }
            withClue("the scried card went to the bottom") {
                game.state.getLibrary(game.player1Id).last() shouldBe top
            }
        }

        test("cannot target a creature its caster controls") {
            val game = board()
            val spell = game.findCardsInHand(1, "Sea God's Revenge").single()
            val result = game.execute(
                CastSpell(game.player1Id, spell, listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!)))
            )
            result.error shouldNotBe null
        }
    }
}
