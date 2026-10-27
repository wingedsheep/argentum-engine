package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.msh.cards.BullseyeDeathDealer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bullseye, Death Dealer (MSH #209) — {2}{B/R} Legendary Creature — Human Assassin Villain, 2/3.
 *
 * - "When Bullseye enters, you may sacrifice an artifact or discard a nonland card. When you do,
 *   Bullseye deals 2 damage to any target."
 * - "{3}, {T}, Sacrifice an artifact or discard a nonland card: Bullseye deals 2 damage to any
 *   target."
 *
 * The ETB is a CR 603.12 reflexive trigger whose action is a two-branch [ChooseOptionDecision]
 * (`ReflexiveTriggerEffect(action = ChooseActionEffect(...))`), so the damage target is chosen only
 * after the cost is actually paid.
 *
 * Bullseye is the first card in the repo where **every** branch of that `ChooseActionEffect` carries
 * a `FeasibilityCheck` — Nimble Hobbit and Anthropede both have a "pay {N}" branch with no check, so
 * `ReflexiveTriggerEffectExecutor.isActionFeasible`'s `choices.any { … }` can never return false for
 * them. The third test below is that corner: controlling no artifact and holding only lands, the
 * whole "you may" prompt must be suppressed rather than offered and then fizzled.
 *
 * The printed activated ability is decomposed into two abilities (one per cost half), so both are
 * exercised separately.
 */
class BullseyeDeathDealerScenarioTest : FunSpec({

    val sacrificeAbilityId = BullseyeDeathDealer.activatedAbilities[0].id
    val discardAbilityId = BullseyeDeathDealer.activatedAbilities[1].id

    // An all-Mountain deck means the opening hand is all lands: the discard branch is infeasible
    // unless a test explicitly puts a nonland card in hand.
    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BullseyeDeathDealer))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /**
     * Resolve the cast Bullseye and its ETB trigger. Returns whether the optional "you may
     * sacrifice … or discard …" prompt was ever presented — `false` is the suppression case, not a
     * timeout: the loop only ever passes priority while something is still on the stack.
     */
    fun GameTestDriver.advanceToMayPrompt(): Boolean {
        var guard = 0
        while (guard++ < 10) {
            if (pendingDecision is YesNoDecision) return true
            if (pendingDecision != null) return false
            if (state.stack.isEmpty()) return false
            bothPass()
        }
        return false
    }

    /** Answer the reflexive trigger's target round with [damageTarget] and resolve it. */
    fun GameTestDriver.finishReflexive(me: EntityId, damageTarget: EntityId) {
        var guard = 0
        while (guard++ < 15) {
            when (pendingDecision) {
                is ChooseTargetsDecision -> submitTargetSelection(me, listOf(damageTarget))
                else -> if (state.stack.isNotEmpty()) bothPass() else return
            }
        }
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (guard++ < 15 && state.stack.isNotEmpty()) bothPass()
    }

    test("ETB sacrifice branch: the artifact is gone before the damage target is chosen") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        driver.giveMana(me, Color.BLACK, 3)

        val artifact = driver.putPermanentOnBattlefield(me, "Artifact Creature")
        // A nonland card in hand as well, so both branches are feasible and a real choice is asked.
        driver.putCardInHand(me, "Savannah Lions")

        val bullseye = driver.putCardInHand(me, "Bullseye, Death Dealer")
        driver.castSpell(me, bullseye)

        driver.advanceToMayPrompt() shouldBe true
        driver.submitYesNo(me, true)

        val choose = driver.pendingDecision as ChooseOptionDecision
        choose.options.size shouldBe 2
        val sacrificeIdx = choose.options.indexOfFirst { it.contains("Sacrifice", ignoreCase = true) }
        driver.submitDecision(me, OptionChosenResponse(choose.id, sacrificeIdx))

        // CR 603.12: the cost is paid first, and only then does the reflexive trigger go on the
        // stack and pick its target. The artifact is already gone at this point.
        driver.state.getZone(me, Zone.BATTLEFIELD).contains(artifact) shouldBe false

        driver.finishReflexive(me, opponent)
        driver.getLifeTotal(opponent) shouldBe 18
        driver.findPermanent(me, "Bullseye, Death Dealer") shouldNotBe null
    }

    test("ETB discard branch: only nonland cards are offered, and discarding deals the 2 damage") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        driver.giveMana(me, Color.BLACK, 3)

        driver.putPermanentOnBattlefield(me, "Artifact Creature")
        // Two nonland cards, so the discard pipeline actually asks which one — with a single
        // eligible card it auto-selects and there is no decision to inspect.
        val lions = driver.putCardInHand(me, "Savannah Lions")
        val bolt = driver.putCardInHand(me, "Lightning Bolt")

        val bullseye = driver.putCardInHand(me, "Bullseye, Death Dealer")
        driver.castSpell(me, bullseye)

        driver.advanceToMayPrompt() shouldBe true
        driver.submitYesNo(me, true)

        val choose = driver.pendingDecision as ChooseOptionDecision
        val discardIdx = choose.options.indexOfFirst { it.contains("Discard", ignoreCase = true) }
        driver.submitDecision(me, OptionChosenResponse(choose.id, discardIdx))

        // The rest of the hand is Mountains — the Nonland filter must exclude them.
        val selection = driver.pendingDecision as SelectCardsDecision
        selection.options.toSet() shouldBe setOf(lions, bolt)
        driver.submitCardSelection(me, listOf(lions))

        driver.finishReflexive(me, opponent)
        driver.getGraveyard(me).contains(lions) shouldBe true
        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("no artifact and a hand of only lands: the may offers only no") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        driver.giveMana(me, Color.BLACK, 3)

        // Nothing to sacrifice, nothing legal to discard. Which cards are in hand is hidden, so
        // the may is still asked (#3340), with "no" as its only answer.
        val bullseye = driver.putCardInHand(me, "Bullseye, Death Dealer")
        driver.castSpell(me, bullseye)

        driver.advanceToMayPrompt() shouldBe true
        (driver.pendingDecision as YesNoDecision).yesAvailable shouldBe false
        driver.submitYesNo(me, true).error shouldNotBe null
        driver.submitYesNo(me, false)
        driver.pendingDecision shouldBe null
        driver.state.stack.isEmpty() shouldBe true
        driver.getLifeTotal(opponent) shouldBe 20
        driver.getLifeTotal(me) shouldBe 20
        driver.findPermanent(me, "Bullseye, Death Dealer") shouldNotBe null
    }

    test("{3}, {T}, Sacrifice an artifact: Bullseye deals 2 damage to any target") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)

        val bullseye = driver.putCreatureOnBattlefield(me, "Bullseye, Death Dealer")
        driver.removeSummoningSickness(bullseye)
        val artifact = driver.putPermanentOnBattlefield(me, "Artifact Creature")
        driver.giveColorlessMana(me, 3)

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = bullseye,
                abilityId = sacrificeAbilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, opponent)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(artifact))
            )
        )
        driver.resolveStack()

        driver.getLifeTotal(opponent) shouldBe 18
        driver.isTapped(bullseye) shouldBe true
        driver.state.getZone(me, Zone.BATTLEFIELD).contains(artifact) shouldBe false
    }

    test("{3}, {T}, Discard a nonland card: Bullseye deals 2 damage to any target") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)

        val bullseye = driver.putCreatureOnBattlefield(me, "Bullseye, Death Dealer")
        driver.removeSummoningSickness(bullseye)
        val lions = driver.putCardInHand(me, "Savannah Lions")
        driver.giveColorlessMana(me, 3)

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = bullseye,
                abilityId = discardAbilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, opponent)),
                costPayment = AdditionalCostPayment(discardedCards = listOf(lions))
            )
        )
        driver.resolveStack()

        driver.getLifeTotal(opponent) shouldBe 18
        driver.isTapped(bullseye) shouldBe true
        driver.getGraveyard(me).contains(lions) shouldBe true
    }
})
