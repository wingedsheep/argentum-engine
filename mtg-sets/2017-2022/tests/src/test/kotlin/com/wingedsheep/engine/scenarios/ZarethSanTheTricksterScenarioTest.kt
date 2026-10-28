package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.znr.cards.GrotagNightRunner
import com.wingedsheep.mtg.sets.definitions.znr.cards.ZarethSanTheTrickster
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Zareth San, the Trickster — the ninjutsu-shaped hand ability (returns an unblocked attacking
 * *Rogue*, puts the card itself from hand onto the battlefield tapped and attacking a defender its
 * controller chooses) and the combat-damage reanimation from the damaged player's graveyard.
 */
class ZarethSanTheTricksterScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + ZarethSanTheTrickster + GrotagNightRunner)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun attackUnblocked(d: GameTestDriver, attacker: EntityId) {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.player2)
        var guard = 0
        while (d.state.priorityPlayerId != d.player1 && d.state.step == Step.DECLARE_BLOCKERS && guard++ < 4) {
            d.passPriority(d.state.priorityPlayerId!!)
        }
    }

    fun activate(d: GameTestDriver, zareth: EntityId, returned: EntityId) = d.submit(
        ActivateAbility(
            playerId = d.player1,
            sourceId = zareth,
            abilityId = ZarethSanTheTrickster.activatedAbilities.single().id,
            costPayment = AdditionalCostPayment(bouncedPermanents = listOf(returned)),
            paymentStrategy = PaymentStrategy.FromPool,
        )
    )

    fun payMana(d: GameTestDriver) {
        d.giveMana(d.player1, Color.BLUE, 2)
        d.giveMana(d.player1, Color.BLACK, 2)
    }

    test("returning an unblocked Rogue puts Zareth San onto the battlefield tapped and attacking") {
        val d = driver()
        val rogue = d.putCreatureOnBattlefield(d.player1, GrotagNightRunner.name).also(d::removeSummoningSickness)
        attackUnblocked(d, rogue)
        val zareth = d.putCardInHand(d.player1, ZarethSanTheTrickster.name)
        payMana(d)

        activate(d, zareth, rogue).outcome shouldBe Outcome.Done
        d.getHand(d.player1) shouldContain rogue
        while (d.state.stack.isNotEmpty()) d.bothPass()

        val onField = d.findPermanent(d.player1, ZarethSanTheTrickster.name).shouldNotBeNull()
        d.isTapped(onField) shouldBe true
        d.state.getEntity(onField)!!.get<AttackingComponent>().shouldNotBeNull().defenderId shouldBe d.player2
        d.getHand(d.player1) shouldNotContain onField
    }

    test("a non-Rogue attacker can't pay the cost") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears").also(d::removeSummoningSickness)
        attackUnblocked(d, bears)
        val zareth = d.putCardInHand(d.player1, ZarethSanTheTrickster.name)
        payMana(d)

        activate(d, zareth, bears).error shouldNotBe null
        d.getHand(d.player1) shouldContain zareth
        d.findPermanent(d.player1, "Grizzly Bears") shouldBe bears
    }

    test("combat damage puts a permanent card from the damaged player's graveyard onto the battlefield under your control") {
        val d = driver()
        val zareth = d.putCreatureOnBattlefield(d.player1, ZarethSanTheTrickster.name).also(d::removeSummoningSickness)
        val theirBears = d.putCardInGraveyard(d.player2, "Grizzly Bears")
        attackUnblocked(d, zareth)
        d.passPriorityUntil(Step.COMBAT_DAMAGE)

        var guard = 0
        while (guard++ < 20 && (d.state.stack.isNotEmpty() || d.pendingDecision != null)) {
            when (val decision = d.pendingDecision) {
                null -> d.bothPass()
                is ChooseTargetsDecision -> d.submitTargetSelection(decision.playerId, listOf(theirBears)).error shouldBe null
                is YesNoDecision -> d.submitYesNo(decision.playerId, true).error shouldBe null
                else -> error("unexpected decision $decision")
            }
        }

        d.getLifeTotal(d.player2) shouldBe 16
        d.findPermanent(d.player1, "Grizzly Bears") shouldBe theirBears
        d.getGraveyard(d.player2) shouldNotContain theirBears
    }
})
