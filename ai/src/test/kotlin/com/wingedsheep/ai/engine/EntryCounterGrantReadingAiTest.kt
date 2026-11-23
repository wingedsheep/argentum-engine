package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "{G}{U}: This turn, each creature you control enters with an additional +1/+1 counter on it"
 * (Zameck Guildmage, Combine Guildmage) — an `EntersWithCounters` replacement granted until end of
 * turn — reads as the lasting growth it gives the creatures that follow ([IntentTag.PUMP]), feeds
 * the +1/+1-counter axis, and is spent only while a main phase of our own turn is still to come.
 *
 * The decision it changes: the activation is left open in our main phase and refused on the
 * opponent's turn and in our end step, where nothing of ours will enter before it wears off.
 */
class EntryCounterGrantReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private val guildmage by lazy { cardRegistry.requireCard("Zameck Guildmage") }

    private val policy by lazy { HoldPolicy(intents, refuseUnspendableGrants = true) }

    private fun verdictOn(activePlayer: Int, step: Step): TimingVerdict {
        val game = scenario()
            .withPlayers()
            .withTurnNumber(6)
            .withActivePlayer(activePlayer)
            .withCardOnBattlefield(1, "Zameck Guildmage")
            .withLandsOnBattlefield(1, "Forest", 1)
            .withLandsOnBattlefield(1, "Island", 1)
            .withCardInHand(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .build()
            .advanceToPriority(1, step)
        return policy.verdictFor(
            game.state, game.player1Id, "Zameck Guildmage",
            activation = ActivateAbility(
                game.player1Id, game.findPermanent("Zameck Guildmage")!!, guildmage.script.activatedAbilities[0].id,
            ),
        )
    }

    init {
        test("the entry-counter grant reads as growth for what follows, with a timing") {
            val ability = guildmage.script.activatedAbilities[0]
            CardIntentAnalyzer.analyzeEffect(ability.effect, ability.targetRequirements, source = guildmage).tags shouldContain
                IntentTag.PUMP
            val reading = AiReadingAudit.read(guildmage)
            withClue("$reading") { reading.readFully shouldBe true }
        }

        test("the entry-counter grant feeds the +1/+1 counters axis") {
            SynergyAxes.of(guildmage).feeds shouldContain SynergyAxis.PLUS_COUNTERS
        }

        test("the entry-counter grant is open in our main phase and refused where nothing of ours will enter") {
            withClue("in our precombat main a creature can still be cast onto it") {
                verdictOn(activePlayer = 1, step = Step.PRECOMBAT_MAIN) shouldNotBe TimingVerdict.NoWindow
            }
            withClue("on the opponent's turn it wears off before our next main phase") {
                verdictOn(activePlayer = 2, step = Step.PRECOMBAT_MAIN) shouldBe TimingVerdict.NoWindow
            }
            withClue("in our end step no main phase is left this turn") {
                verdictOn(activePlayer = 1, step = Step.END) shouldBe TimingVerdict.NoWindow
            }
        }
    }
}
