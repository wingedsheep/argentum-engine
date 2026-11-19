package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A fight granted until end of turn — "until end of turn, any number of target creatures each get
 * +1/+1 and gain '{T}: This creature fights another target creature'" (Setessan Tactics) — is
 * removal waiting on its activation, as the same fight printed on the creature is. The +1/+1 beside
 * it is the fights' setup, so the removal owns the timing, not a combat-trick hold: the
 * hold leaves it open in our precombat main, where the pumped creature can fight before combat —
 * the window a trick held for blocks shuts.
 */
class GrantedFightRemovalReadingAiTest : ScenarioTestBase() {

    init {
        test("a fight granted until end of turn reads as removal, not as a combat trick") {
            val tactics = cardRegistry.requireCard(SETESSAN_TACTICS)
            val tags = CardIntentAnalyzer.analyze(tactics).tags
            tags shouldContain IntentTag.REMOVAL
            tags shouldContain IntentTag.FIGHT
            tags shouldNotContain IntentTag.COMBAT_TRICK
            val reading = AiReadingAudit.read(tactics)
            withClue(reading.toString()) { reading.readFully shouldBe true }
        }

        test("Setessan Tactics is not held out of our precombat main, where the pumped creature can fight") {
            val game = scenario().withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, SETESSAN_TACTICS)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .apply { repeat(4) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val card = game.findCardsInHand(1, SETESSAN_TACTICS).first()
            val cast = CastSpell(game.player1Id, card, listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!)))
            val policy = HoldPolicy(
                IntentCatalog.of(cardRegistry),
                holdExpiringGrantsForCombat = true, refuseUnspendableGrants = true, holdUnusablePumps = true,
            )
            withClue("the Giant can fight the Bears before combat: the removal's window, not a trick's hold") {
                policy.verdictFor(game.state, game.player1Id, SETESSAN_TACTICS, cast = cast) shouldNotBe TimingVerdict.NoWindow
            }
        }
    }

    private companion object {
        const val SETESSAN_TACTICS = "Setessan Tactics"
    }
}
