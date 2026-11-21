package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A combat lever beside a cantrip's draw — "target creature can't block this turn. Draw a card."
 * (Stun), "target creature gets -1/-0 until end of turn … draw a card" (Timely Interference),
 * "target creature blocks this turn if able … Learn" (Academic Dispute).
 *
 * The draw always buys something, so the whole-spell floor never judged these, and the end-step
 * reward for cantrips paid the AI to cast Stun at the opponent's end step, where the can't-block
 * keeps nothing out of any block. Cast on our turn before blocks, the same Stun draws the same card
 * and keeps a blocker home.
 */
class CantripLeverTimingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun position(spell: String, activePlayer: Int, step: Step) = scenario()
        .withPlayers()
        .withTurnNumber(6)
        .withActivePlayer(activePlayer)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInHand(1, spell)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .build()
        .advanceToPriority(1, step)

    private fun TestGame.verdict(spell: String, refuseWastedPayoffs: Boolean = true): TimingVerdict {
        val card = state.getZone(player1Id, Zone.HAND).first { state.getEntity(it)?.get<CardComponent>()?.name == spell }
        val cast = CastSpell(player1Id, card, targets = listOf(ChosenTarget.Permanent(findPermanent("Grizzly Bears")!!)))
        return HoldPolicy(intents, refuseWastedPayoffs = refuseWastedPayoffs).verdictFor(state, player1Id, spell, cast = cast)
    }

    init {
        test("Stun's can't-block is thrown away on their turn, and live on ours before blocks") {
            withClue("their end step: their Bears blocks nothing of ours before cleanup") {
                position("Stun", activePlayer = 2, step = Step.END).verdict("Stun") shouldBe TimingVerdict.NoWindow
            }
            withClue("the floor rides on refuseWastedPayoffs") {
                position("Stun", activePlayer = 2, step = Step.END).verdict("Stun", refuseWastedPayoffs = false) shouldNotBe
                    TimingVerdict.NoWindow
            }
            withClue("our precombat main: Stun keeps the Bears from blocking the Giant") {
                position("Stun", activePlayer = 1, step = Step.PRECOMBAT_MAIN).verdict("Stun") shouldNotBe TimingVerdict.NoWindow
            }
        }

        test("the live AI holds Stun at the opponent's end step instead of cashing it as a cantrip") {
            val game = position("Stun", activePlayer = 2, step = Step.END)
            val choice = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            withClue("chose $choice") { (choice is CastSpell) shouldBe false }
        }

        test("Timely Interference's -1/-0 waits for a fight; Academic Dispute's lure for our attack") {
            withClue("Timely Interference in their end step") {
                position("Timely Interference", activePlayer = 2, step = Step.END).verdict("Timely Interference") shouldBe
                    TimingVerdict.NoWindow
            }
            withClue("Academic Dispute in their end step") {
                position("Academic Dispute", activePlayer = 2, step = Step.END).verdict("Academic Dispute") shouldBe
                    TimingVerdict.NoWindow
            }
            withClue("Academic Dispute on our turn before blocks drags the Bears into a block") {
                position("Academic Dispute", activePlayer = 1, step = Step.PRECOMBAT_MAIN).verdict("Academic Dispute") shouldNotBe
                    TimingVerdict.NoWindow
            }
        }

        test("the audit reads the levers' timing on all three spells") {
            for (name in listOf("Stun", "Timely Interference", "Academic Dispute")) {
                val reading = AiReadingAudit.read(cardRegistry.requireCard(name))
                withClue("$name: $reading") { reading.flags.none { it.key.startsWith("timing") } shouldBe true }
            }
        }
    }
}
