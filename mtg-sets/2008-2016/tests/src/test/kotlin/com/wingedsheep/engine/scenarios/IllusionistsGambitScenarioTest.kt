package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c13.cards.IllusionistsGambit
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Illusionist's Gambit (C13) — {2}{U}{U} Instant.
 *
 * "Cast this spell only during the declare blockers step on an opponent's turn. Remove all
 * attacking creatures from combat and untap them. After this phase, there is an additional combat
 * phase. Each of those creatures attacks that combat if able. They can't attack you or
 * planeswalkers you control that combat."
 *
 * The composition under test: the removed attackers carry both a must-attack requirement and a
 * granted can't-attack-the-caster restriction into the added combat. With a third player the
 * requirement redirects them; heads-up it is unsatisfiable, so it doesn't force an illegal attack.
 */
class IllusionistsGambitScenarioTest : FunSpec({

    fun GameTestDriver.passUntilStackEmpty() {
        var guard = 0
        while (state.stack.isNotEmpty() && state.pendingDecision == null && guard++ < 20) {
            passPriority(state.priorityPlayerId!!).error shouldBe null
        }
    }

    /** Active player attacks [defender] with fresh Grizzly Bears; stops in declare blockers. */
    fun GameTestDriver.attackInto(defender: EntityId): EntityId {
        val attacker = activePlayer!!
        val bears = putCreatureOnBattlefield(attacker, "Grizzly Bears")
        removeSummoningSickness(bears)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(attacker, listOf(bears), defender).error shouldBe null
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        autoSubmitCombatDeclarationIfNeeded()
        return bears
    }

    fun GameTestDriver.castGambit(caster: EntityId) {
        // Hand priority to the caster within the declare blockers step.
        var guard = 0
        while (priorityPlayer != caster && guard++ < 5) passPriority(priorityPlayer!!).error shouldBe null
        state.step shouldBe Step.DECLARE_BLOCKERS
        val spell = putCardInHand(caster, "Illusionist's Gambit")
        giveMana(caster, Color.BLUE, 4)
        castSpell(caster, spell).error shouldBe null
        passUntilStackEmpty()
    }

    test("with a third player, the removed attacker must attack someone other than the caster") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + IllusionistsGambit)
            initMultiplayer(List(3) { Deck.of("Island" to 40) })
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val (p1, p2, p3) = d.state.turnOrder
        d.activePlayer shouldBe p1

        val bears = d.attackInto(p2)
        d.castGambit(p2)

        withClue("removed from combat and untapped") {
            d.state.getEntity(bears)!!.has<AttackingComponent>() shouldBe false
            d.isTapped(bears) shouldBe false
        }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.activePlayer shouldBe p1
        withClue("the caster no longer took the damage") { d.getLifeTotal(p2) shouldBe 20 }

        withClue("it can't attack the caster") {
            d.declareAttackers(p1, listOf(bears), p2).error shouldNotBe null
        }
        withClue("it must attack if able") {
            d.declareAttackers(p1, emptyMap()).error shouldNotBe null
        }
        d.declareAttackers(p1, listOf(bears), p3).error shouldBe null
    }

    test("heads-up the attacker has no legal defender, so it isn't forced to attack") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + IllusionistsGambit)
            initMirrorMatch(Deck.of("Island" to 40))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)

        val bears = d.attackInto(p2)
        d.castGambit(p2)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.getLifeTotal(p2) shouldBe 20
        withClue("with no legal defender left, the requirement is void and nothing attacks") {
            d.state.getEntity(bears)!!.has<AttackingComponent>() shouldBe false
            d.passPriorityUntil(Step.END)
            d.getLifeTotal(p2) shouldBe 20
        }
    }

    test("can't be cast outside the declare blockers step of an opponent's turn") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + IllusionistsGambit)
            initMirrorMatch(Deck.of("Island" to 40))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        val bears = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.removeSummoningSickness(bears)
        val mine = d.putCardInHand(p1, "Illusionist's Gambit")
        val theirs = d.putCardInHand(p2, "Illusionist's Gambit")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(bears), p2).error shouldBe null
        withClue("not during declare attackers") {
            d.passPriority(p1).error shouldBe null
            d.priorityPlayer shouldBe p2
            d.giveMana(p2, Color.BLUE, 4)
            d.castSpell(p2, theirs).error shouldNotBe null
        }
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.autoSubmitCombatDeclarationIfNeeded()
        withClue("not on your own turn") {
            if (d.priorityPlayer != p1) d.passPriority(d.priorityPlayer!!).error shouldBe null
            d.state.step shouldBe Step.DECLARE_BLOCKERS
            d.priorityPlayer shouldBe p1
            d.giveMana(p1, Color.BLUE, 4)
            d.castSpell(p1, mine).error shouldNotBe null
        }
    }
})
