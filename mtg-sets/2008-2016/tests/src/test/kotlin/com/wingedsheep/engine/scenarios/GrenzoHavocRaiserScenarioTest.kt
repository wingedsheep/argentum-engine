package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.combat.GoadedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.cn2.cards.GrenzoHavocRaiser
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Grenzo, Havoc Raiser (CN2 #54) — {R}{R} Legendary Creature — Goblin Rogue 2/2.
 *
 *   "Whenever a creature you control deals combat damage to a player, choose one —
 *    • Goad target creature that player controls.
 *    • Exile the top card of that player's library. Until end of turn, you may cast that card and
 *      you may spend mana as though it were mana of any color to cast that spell."
 *
 * Three players, so "that player" (the damaged one) is distinguishable from "an opponent": the goad
 * mode may only target creatures the damaged player controls, and the exile mode reads the damaged
 * player's library. The exiled card is then cast with off-colour mana.
 */
class GrenzoHavocRaiserScenarioTest : FunSpec({

    class Table(val d: GameTestDriver, val p1: EntityId, val p2: EntityId, val p3: EntityId)

    fun table(): Table {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + GrenzoHavocRaiser)
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Mountain" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Table(d, p1, p2, p3)
    }

    /** Grenzo stays home; a Grizzly Bears attacks [defender] unblocked until the mode choice. */
    fun Table.attackToModeChoice(defender: EntityId): ChooseOptionDecision {
        d.putCreatureOnBattlefield(p1, "Grenzo, Havoc Raiser")
        val bears = d.putCreatureOnBattlefield(p1, "Grizzly Bears").also { d.removeSummoningSickness(it) }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(bears), defender).error shouldBe null
        var guard = 0
        while (guard++ < 30 && d.pendingDecision !is ChooseOptionDecision) {
            check(d.pendingDecision == null) { "unexpected decision ${d.pendingDecision}" }
            d.bothPass()
        }
        return d.pendingDecision as ChooseOptionDecision
    }

    fun Table.chooseMode(decision: ChooseOptionDecision, text: String) {
        val index = decision.options.indexOfFirst { it.contains(text) }
        check(index >= 0) { "mode '$text' not offered; options=${decision.options}" }
        d.submitDecision(p1, OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    fun Table.resolveStack() {
        var guard = 0
        while (guard++ < 20 && d.pendingDecision == null && d.stackSize > 0) d.bothPass()
    }

    test("goad mode targets only creatures the damaged player controls") {
        val t = table()
        val damagedPlayersCreature = t.d.putCreatureOnBattlefield(t.p2, "Centaur Courser")
        val otherOpponentsCreature = t.d.putCreatureOnBattlefield(t.p3, "Centaur Courser")

        val decision = t.attackToModeChoice(defender = t.p2)
        withClue("the Bears' combat damage landed") { t.d.getLifeTotal(t.p2) shouldBe 18 }
        t.chooseMode(decision, "Goad")

        val targeting = t.d.pendingDecision as ChooseTargetsDecision
        val legal = targeting.legalTargets.getValue(0)
        legal shouldContain damagedPlayersCreature
        withClue("a creature controlled by an opponent who wasn't dealt damage is not 'that player's'") {
            legal shouldNotContain otherOpponentsCreature
        }

        t.d.submitTargetSelection(t.p1, listOf(damagedPlayersCreature)).error shouldBe null
        t.resolveStack()

        t.d.state.getEntity(damagedPlayersCreature)!!.get<GoadedComponent>()!!.goaderIds shouldContain t.p1
        t.d.state.getEntity(otherOpponentsCreature)!!.get<GoadedComponent>() shouldBe null
    }

    test("exile mode exiles the damaged player's top card, castable this turn with mana of any colour") {
        val t = table()
        val top = t.d.putCardOnTopOfLibrary(t.p3, "Grizzly Bears")
        t.d.putCardOnTopOfLibrary(t.p2, "Centaur Courser")

        val decision = t.attackToModeChoice(defender = t.p3)
        t.chooseMode(decision, "Exile the top card")
        t.resolveStack()

        withClue("the damaged player's (p3's) top card is exiled, not p2's") {
            t.d.getExile(t.p3) shouldContain top
            t.d.getExileCardNames(t.p2) shouldNotContain "Centaur Courser"
        }

        t.d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        // {1}{G} paid by two Mountains — "as though it were mana of any color".
        repeat(2) { t.d.putLandOnBattlefield(t.p1, "Mountain") }
        t.d.castSpell(t.p1, top).error shouldBe null
        t.resolveStack()

        withClue("the stolen Grizzly Bears resolved under Grenzo's controller") {
            t.d.getController(top) shouldBe t.p1
        }
    }
})
