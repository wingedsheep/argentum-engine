package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.MeldedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Mishra, Claimed by Gix (BRO #216) — "Whenever you attack, each opponent loses X life and you gain
 * X life, where X is the number of attacking creatures. If Mishra, Claimed by Gix and a creature
 * named Phyrexian Dragon Engine are attacking, and you both own and control them, exile them, then
 * meld them into Mishra, Lost to Phyrexia. It enters tapped and attacking."
 *
 * Pins the drain counting every attacker, the meld into a tapped, attacking 9/9 whose enters half
 * of its choose-three trigger fires, and no meld without an attacking Dragon Engine.
 */
class MishraClaimedByGixScenarioTest : ScenarioTestBase() {
    private val mishra = "Mishra, Claimed by Gix"
    private val engine = "Phyrexian Dragon Engine"
    private val lost = "Mishra, Lost to Phyrexia"

    private fun TestGame.chooseMode(label: String) {
        val decision = state.pendingDecision as? ChooseOptionDecision
            ?: error("expected a mode choice; got ${state.pendingDecision}")
        val index = decision.options.indexOfFirst { it.startsWith(label) }
        require(index >= 0) { "mode '$label' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    private fun board(partner: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, mishra)
        .withCardOnBattlefield(1, partner)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()

    init {
        test("attacking with Phyrexian Dragon Engine drains two, then melds into a tapped, attacking Mishra, Lost to Phyrexia") {
            val game = board(engine)
            val mishraId = game.findPermanent(mishra)!!

            game.declareAttackers(mapOf(mishra to 2, engine to 2)).error shouldBe null
            game.resolveStack()

            withClue("X = 2 attackers") {
                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 22
            }
            val melded = game.findPermanent(lost).shouldNotBeNull()
            withClue("Mishra's entity is the melded permanent") { melded shouldBe mishraId }
            game.findPermanent(mishra) shouldBe null
            game.findPermanent(engine) shouldBe null
            game.state.getEntity(melded)!!.has<MeldedComponent>() shouldBe true
            game.state.projectedState.getPower(melded) shouldBe 9
            game.state.projectedState.getToughness(melded) shouldBe 9
            game.state.getEntity(melded)!!.has<TappedComponent>() shouldBe true
            withClue("it entered attacking") { game.state.getEntity(melded)!!.has<AttackingComponent>() shouldBe true }

            withClue("the enters half of the choose-three trigger fires") {
                game.chooseMode("Creatures you control gain menace")
                game.chooseMode("Creatures you don't control get -1/-1")
                game.chooseMode("Create two tapped Powerstone")
                game.resolveStack()
            }
            game.findPermanents("Powerstone").size shouldBe 2
            game.state.projectedState.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 1
            game.state.projectedState.hasKeyword(melded, Keyword.TRAMPLE) shouldBe true
        }

        test("without an attacking Dragon Engine there is no meld, only the drain") {
            val game = board("Grizzly Bears")

            game.declareAttackers(mapOf(mishra to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 22
            game.findPermanent(mishra).shouldNotBeNull()
            game.findPermanent(lost) shouldBe null
        }

        test("a Dragon Engine that stays home doesn't meld; only Mishra's attack is counted") {
            val game = board(engine)

            game.declareAttackers(mapOf(mishra to 2)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 21
            game.findPermanent(mishra).shouldNotBeNull()
            game.findPermanent(engine).shouldNotBeNull()
            game.findPermanent(lost) shouldBe null
        }
    }
}
