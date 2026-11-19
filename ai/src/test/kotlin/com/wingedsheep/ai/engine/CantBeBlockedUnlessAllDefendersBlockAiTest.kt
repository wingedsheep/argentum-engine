package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Can't be blocked unless all creatures defending player controls block it" (Tromokratis): the
 * AI's combat math reads the attacker as unblockable while the defender has more than one creature,
 * so it never submits a lone chump block the engine would reject.
 */
class CantBeBlockedUnlessAllDefendersBlockAiTest : ScenarioTestBase() {

    init {
        test("Tromokratis reads as evasive") {
            CardIntentAnalyzer.analyze(cardRegistry.requireCard("Tromokratis")).tags shouldContain IntentTag.EVASION_GRANT
        }

        test("combat math: blockable by a lone creature, evasive against two") {
            val game = scenario()
                .withPlayers("Attacker", "AI")
                .withCardOnBattlefield(1, "Tromokratis")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tromokratis = game.findPermanent("Tromokratis")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            CombatMath.canBeBlockedBy(game.state, game.state.projectedState, tromokratis, bears, cardRegistry) shouldBe true

            val crowded = scenario()
                .withPlayers("Attacker", "AI")
                .withCardOnBattlefield(1, "Tromokratis")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val projected = crowded.state.projectedState
            val blockers = listOf(crowded.findPermanent("Grizzly Bears")!!, crowded.findPermanent("Savannah Lions")!!)
            CombatMath.isEvasive(crowded.state, projected, crowded.findPermanent("Tromokratis")!!, blockers, cardRegistry) shouldBe true
        }

        test("facing lethal from Tromokratis with two creatures, the AI blocks it with both creatures") {
            val game = scenario()
                .withPlayers("Attacker", "AI")
                .withCardOnBattlefield(1, "Tromokratis")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withLifeTotal(2, 8)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Tromokratis" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            val action = AIPlayer.create(cardRegistry, game.player2Id, AiProfile.LIVE).chooseAction(game.state)
            val blocks = action.shouldBeInstanceOf<DeclareBlockers>().blockers
            val tromokratis = game.findPermanent("Tromokratis")!!
            withClue("blocks: $blocks") {
                blocks shouldBe mapOf(
                    game.findPermanent("Grizzly Bears")!! to listOf(tromokratis),
                    game.findPermanent("Savannah Lions")!! to listOf(tromokratis),
                )
                game.execute(action).error shouldBe null
            }
        }

        test("not facing lethal, the AI keeps its creatures out of Tromokratis' way") {
            val game = scenario()
                .withPlayers("Attacker", "AI")
                .withCardOnBattlefield(1, "Tromokratis")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Tromokratis" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            val action = AIPlayer.create(cardRegistry, game.player2Id, AiProfile.LIVE).chooseAction(game.state)
            val blocks = action.shouldBeInstanceOf<DeclareBlockers>().blockers
            withClue("blocks: $blocks") {
                blocks shouldBe emptyMap()
                game.execute(action).error shouldBe null
            }
        }
    }
}
