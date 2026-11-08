package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.ItzquinthFirstbornOfGishath
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.engine.core.Outcome

/**
 * Itzquinth, Firstborn of Gishath (LCI #230) — {R}{G}, Legendary Creature — Dinosaur, 2/3.
 *
 * "Haste"
 * "When Itzquinth enters, you may pay {2}. When you do, target Dinosaur you control deals
 *  damage equal to its power to another target creature."
 *
 * Tests:
 *  1. Haste is present in projected state and lets Itzquinth attack the turn it enters.
 *  2. ETB trigger fires. "When you do" is a reflexive trigger (CR 603.12): the ETB trigger goes
 *     on the stack with no targets and, as it resolves, offers the "Pay {2}?" YesNoDecision; only after paying does the reflexive trigger go on
 *     the stack and prompt for its two targets (ChooseTargetsDecision). The Dinosaur then deals
 *     power-damage to the other creature.
 *  3. Declining the may-pay skips targeting entirely and leaves the other creature unharmed.
 */
class ItzquinthFirstbornOfGishathScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(ItzquinthFirstbornOfGishath)
        initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )
    }

    // ─── Haste ───────────────────────────────────────────────────────────────

    test("Itzquinth has haste in projected state") {
        val d = driver()
        val player = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val itz = d.putCreatureOnBattlefield(player, "Itzquinth, Firstborn of Gishath")

        withClue("Itzquinth should have haste") {
            d.state.projectedState.hasKeyword(itz, Keyword.HASTE) shouldBe true
        }
    }

    test("haste lets Itzquinth attack the turn it enters") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(player, "Itzquinth, Firstborn of Gishath")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val result = d.declareAttackers(player, listOf(
            d.findPermanent(player, "Itzquinth, Firstborn of Gishath")!!
        ), opponent)

        withClue("Itzquinth should be able to attack on the turn it enters (haste): ${result.error}") {
            result.outcome shouldBe Outcome.Done
        }
    }

    // ─── ETB trigger: pay {2}, Dinosaur deals power-damage ──────────────────

    test("ETB trigger: paying {2} causes Itzquinth (the Dinosaur) to deal 2 damage to another creature") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent controls a 2/2 that will take the damage.
        val victim = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // {R}{G} in the pool pays the cast cleanly (FromPool, no mana-source prompt).
        d.giveMana(player, Color.RED, 1)
        d.giveMana(player, Color.GREEN, 1)
        // Two untapped lands provide the {2} for the reflexive "you may pay" gate.
        d.putLandOnBattlefield(player, "Mountain")
        d.putLandOnBattlefield(player, "Forest")

        val card = d.putCardInHand(player, "Itzquinth, Firstborn of Gishath")
        d.castSpell(player, card).outcome shouldBe Outcome.Done

        // Itzquinth resolves → enters the battlefield → its ETB trigger (no targets of its own)
        // goes on the stack → it resolves to the pay gate.
        d.bothPass()
        d.bothPass()

        // Step 1: the "Pay {2}?" gate is offered FIRST (the reflexive-trigger order).
        withClue("YesNoDecision (Pay {2}?) expected first") {
            d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        }
        d.submitYesNo(player, true)

        // Step 2: pay the {2} — auto-pay taps the two untapped lands.
        withClue("SelectManaSourcesDecision expected to pay {2}") {
            d.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        }
        d.submitManaAutoPayOrDecline(player, autoPay = true)

        // Step 3: paying spawns the reflexive trigger, which now prompts for its two targets.
        withClue("ChooseTargetsDecision expected after paying {2}") {
            d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        }
        val itz = d.findPermanent(player, "Itzquinth, Firstborn of Gishath")!!
        // t1 (index 0) = Itzquinth (it IS a Dinosaur you control); t2 (index 1) = victim.
        d.submitMultiTargetSelection(player, mapOf(0 to listOf(itz), 1 to listOf(victim)))
        d.bothPass()

        // Itzquinth is 2/3; dealing 2 damage kills the 2/2 Grizzly Bears.
        withClue("Grizzly Bears should die to 2 damage from Itzquinth's bite") {
            d.findPermanent(opponent, "Grizzly Bears") shouldBe null
            d.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"
        }
        withClue("Itzquinth should survive (it dealt the damage, wasn't targeted for damage)") {
            d.findPermanent(player, "Itzquinth, Firstborn of Gishath") shouldNotBe null
        }
    }

    test("ETB trigger: declining to pay {2} leaves the other creature unharmed") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        d.giveMana(player, Color.RED, 1)
        d.giveMana(player, Color.GREEN, 1)
        d.giveColorlessMana(player, 2) // afford the cast and optionally the {2}

        val card = d.putCardInHand(player, "Itzquinth, Firstborn of Gishath")
        d.castSpell(player, card).outcome shouldBe Outcome.Done
        d.bothPass() // Itzquinth resolves; its ETB trigger goes on the stack
        d.bothPass() // the trigger resolves to the pay gate

        // The "Pay {2}?" gate is offered first — decline it.
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(player, false)
        d.bothPass()

        withClue("declining {2} means no reflexive trigger, so no target selection") {
            d.pendingDecision shouldBe null
        }
        withClue("Grizzly Bears should survive when {2} is declined") {
            d.findPermanent(opponent, "Grizzly Bears") shouldNotBe null
        }
    }
})
