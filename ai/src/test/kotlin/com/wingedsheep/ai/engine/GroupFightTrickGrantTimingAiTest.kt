package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A fight trick handed to a group until end of turn — "until end of turn, attacking creatures you
 * control gain '{R}: This creature gets +1/+0 until end of turn'" (Dragonrage), "creatures you
 * control … gain '{B}: This creature gets +1/+1 until end of turn'" (Shade's Breath) — is a team
 * pump: [com.wingedsheep.ai.engine.knowledge.CardIntent.expiringGroupPump], held as a combat trick
 * until the team is in the fight ([AiProfile.holdUnusablePumps]).
 */
class GroupFightTrickGrantTimingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val policy by lazy { HoldPolicy(intents, refuseUnspendableGrants = true, holdUnusablePumps = true) }

    private fun TestGame.castVerdict(cardName: String): TimingVerdict {
        val card = findCardsInHand(1, cardName).first()
        return policy.verdictFor(state, player1Id, cardName, cast = CastSpell(player1Id, card))
    }

    private fun board() = scenario().withPlayers()
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, "Dragonrage")
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withCardOnBattlefield(2, "Hill Giant")

    init {
        test("a group grant of a self-pump reads as a team pump") {
            for (card in listOf("Dragonrage", "Shade's Breath")) {
                CardIntentAnalyzer.analyze(cardRegistry.requireCard(card)).expiringGroupPump shouldBe true
            }
            // A granted counterspell is not a fight trick.
            CardIntentAnalyzer.analyze(cardRegistry.requireCard("Psychic Trance")).expiringGroupPump shouldBe false
        }

        test("Dragonrage in our declare-attackers step with no attackers is held") {
            val game = board().build().advanceToPriority(1, Step.DECLARE_ATTACKERS)
            game.castVerdict("Dragonrage") shouldBe TimingVerdict.NoWindow
        }

        test("Dragonrage in our beginning-of-combat step waits for attackers") {
            val game = board().build().advanceToPriority(1, Step.BEGIN_COMBAT)
            game.castVerdict("Dragonrage") shouldBe TimingVerdict.NoWindow
        }

        test("Dragonrage with an attacker in the fight gets its combat window") {
            val game = board().build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.advanceToPriority(1, Step.DECLARE_ATTACKERS)
            game.castVerdict("Dragonrage").shouldBeInstanceOf<TimingVerdict.Adjust>()
        }
    }
}
