package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The spell twin of [DiesTriggerGrantTimingTest]: Showstopper's "Until end of turn, creatures you
 * control gain 'When this creature dies, it deals 2 damage to target creature an opponent
 * controls.'"
 *
 * The granted trigger pays off only if one of our creatures dies before cleanup — in a fight still
 * ahead, or to something on the stack. In our second main phase with an empty stack, nothing of
 * ours can die this turn, so the instant is a card thrown away; with removal aimed at our creature
 * it is live.
 */
class DiesTriggerGrantSpellTimingTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    /** The spell-side "nothing can spend it" floor. */
    private val policy by lazy { HoldPolicy(intents, refuseUnspendableGrants = true, refuseWastedPayoffs = true) }

    private fun TestGame.verdict(): TimingVerdict =
        policy.verdictFor(state, player1Id, SHOWSTOPPER, cast = CastSpell(player1Id, findCardsInHand(1, SHOWSTOPPER).first()))

    private fun board(active: Int) = scenario().withPlayers()
        .withActivePlayer(active)
        .withCardInHand(1, SHOWSTOPPER)
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInHand(2, "Murder")
        .withLandsOnBattlefield(2, "Swamp", 3)
        .apply { repeat(3) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Swamp") } }
        .build()

    init {
        test("Showstopper reads fully") {
            val reading = AiReadingAudit.read(cardRegistry.requireCard(SHOWSTOPPER))
            withClue(reading.toString()) { reading.readFully shouldBe true }
        }

        test("our second main phase, empty stack: nothing of ours can die before cleanup, so no window") {
            val game = board(active = 1).advanceToPriority(1, Step.POSTCOMBAT_MAIN)
            game.verdict() shouldBe TimingVerdict.NoWindow
        }

        test("a sacrifice outlet of ours spends the granted trigger at any time") {
            val game = scenario().withPlayers()
                .withActivePlayer(1)
                .withCardInHand(1, SHOWSTOPPER)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Ashnod's Altar")
                .withCardOnBattlefield(2, "Hill Giant")
                .apply { repeat(3) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Swamp") } }
                .build()
                .advanceToPriority(1, Step.POSTCOMBAT_MAIN)
            game.verdict() shouldNotBe TimingVerdict.NoWindow
        }

        test("removal aimed at our Bears: the granted trigger can still pay off") {
            val game = board(active = 2).advanceToPriority(2, Step.PRECOMBAT_MAIN)
            game.castSpell(2, "Murder", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.passPriority()
            game.verdict() shouldNotBe TimingVerdict.NoWindow
        }
    }

    private companion object {
        const val SHOWSTOPPER = "Showstopper"
    }
}
