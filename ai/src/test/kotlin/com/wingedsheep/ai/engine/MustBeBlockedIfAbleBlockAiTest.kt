package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "It must be blocked this turn if able" (King Harald's Revenge) names no blocker, so the engine
 * suggests none. The AI used to decline every block against the pumped trampler, the engine
 * rejected the empty declaration, and the AI resubmitted it until its clock ran out and it
 * conceded. It now obeys the requirement with the cheapest able blocker.
 */
class MustBeBlockedIfAbleBlockAiTest : ScenarioTestBase() {

    private fun board(lured: List<String>) = scenario()
        .withPlayers("Attacker", "AI")
        .withCardOnBattlefield(1, "Grizzled Outrider")
        .withCardOnBattlefield(1, "Sarulf's Packmate")
        .withCardOnBattlefield(1, "Icehide Troll")
        .apply { repeat(lured.size) { withCardInHand(1, "King Harald's Revenge") } }
        .withLandsOnBattlefield(1, "Forest", 6)
        .withCardOnBattlefield(2, "Priest of the Haunted Edge")
        .withCardOnBattlefield(2, "Draugr Recruiter")
        .withCardOnBattlefield(2, "Fearless Liberator")
        .withCardOnBattlefield(2, "Elderfang Disciple")
        .withLandsOnBattlefield(2, "Swamp", 5)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { game ->
            for (name in lured) {
                game.castSpell(1, "King Harald's Revenge", game.findPermanent(name)!!).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
            }
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(lured.associateWith { 2 }).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        }

    init {
        test("the AI blocks a creature that must be blocked, with a legal declaration") {
            val game = board(listOf("Icehide Troll"))
            val troll = game.findPermanent("Icehide Troll")!!
            val action = AIPlayer.create(cardRegistry, game.player2Id, AiProfile.LIVE).chooseAction(game.state)
            val blocks = action.shouldBeInstanceOf<DeclareBlockers>().blockers
            withClue("blocks: $blocks") {
                blocks.values.count { troll in it } shouldBe 1
                game.execute(action).error shouldBe null
            }
        }

        test("two creatures that must be blocked each get a blocker") {
            val game = board(listOf("Icehide Troll", "Sarulf's Packmate"))
            val troll = game.findPermanent("Icehide Troll")!!
            val packmate = game.findPermanent("Sarulf's Packmate")!!
            val action = AIPlayer.create(cardRegistry, game.player2Id, AiProfile.LIVE).chooseAction(game.state)
            val blocks = action.shouldBeInstanceOf<DeclareBlockers>().blockers
            withClue("blocks: $blocks") {
                blocks.values.any { troll in it } shouldBe true
                blocks.values.any { packmate in it } shouldBe true
                game.execute(action).error shouldBe null
            }
        }
    }
}
