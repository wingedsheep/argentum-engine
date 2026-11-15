package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.CreatureTypeSynergy
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
 * "This creature can block Dragons as though it had reach" (Dragon Hunter): the AI reads the
 * permission as more blocks open to the creature, not as an ask for Dragons, and its combat math
 * lets the creature block a flying Dragon — so facing lethal from one, it walls it.
 */
class BlockAsThoughReachAiTest : ScenarioTestBase() {

    init {
        test("Dragon Hunter reads as extra blocking and asks for no tribe") {
            val hunter = cardRegistry.requireCard("Dragon Hunter")
            CardIntentAnalyzer.analyze(hunter).tags shouldContain IntentTag.EXTRA_BLOCKING
            CreatureTypeSynergy.rewarded(hunter) shouldBe emptySet()
        }

        test("combat math lets it block a flying Dragon but not another flyer") {
            val game = scenario()
                .withPlayers("Attacker", "AI")
                .withCardOnBattlefield(1, "Shivan Dragon")
                .withCardOnBattlefield(1, "Wind Drake")
                .withCardOnBattlefield(2, "Dragon Hunter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val hunter = game.findPermanent("Dragon Hunter")!!
            val projected = game.state.projectedState
            CombatMath.canBeBlockedBy(game.state, projected, game.findPermanent("Shivan Dragon")!!, hunter, cardRegistry) shouldBe true
            CombatMath.canBeBlockedBy(game.state, projected, game.findPermanent("Wind Drake")!!, hunter, cardRegistry) shouldBe false
        }

        test("facing lethal from a flying Dragon, the defending AI blocks it with Dragon Hunter") {
            val game = scenario()
                .withPlayers("Attacker", "AI")
                .withCardOnBattlefield(1, "Shivan Dragon")
                .withCardOnBattlefield(2, "Dragon Hunter")
                .withLifeTotal(2, 4)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Shivan Dragon" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            val hunter = game.findPermanent("Dragon Hunter")!!
            val dragon = game.findPermanent("Shivan Dragon")!!
            val action = AIPlayer.create(cardRegistry, game.player2Id, AiProfile.LIVE).chooseAction(game.state)
            val blocks = action.shouldBeInstanceOf<DeclareBlockers>().blockers
            withClue("blocks: $blocks") { blocks[hunter] shouldBe listOf(dragon) }
            game.execute(action).error shouldBe null
        }
    }
}
