package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmt.cards.NinjaTeen
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome
import io.kotest.matchers.shouldNotBe

/**
 * Ninja Teen level 3: "Creature cards in your graveyard have sneak {3}{B}. You may cast creature
 * spells from your graveyard using their sneak abilities." Proves the `GraveyardCreaturesHaveSneak`
 * grant — a graveyard creature with NO printed Sneak becomes castable from the graveyard via the
 * granted sneak cost during the declare-blockers sneak window, entering the battlefield tapped.
 */
class NinjaTeenTest : FunSpec({

    // A vanilla creature with no printed sneak; it gains sneak only via Ninja Teen in the graveyard.
    val goon = card("Graveyard Goon") {
        manaCost = "{5}{B}"
        typeLine = "Creature — Zombie"
        power = 4
        toughness = 4
    }
    // A vanilla creature that declares as the unblocked attacker to return for the sneak cost.
    val brawler = card("Plain Brawler") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(NinjaTeen, goon, brawler))
        return d
    }

    /** Advance to the declare blockers step with [attackerCreature] declared, unblocked. */
    fun GameTestDriver.openSneakWindow(attacker: EntityId, defender: EntityId, attackerCreature: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(attacker, listOf(attackerCreature), defender).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        declareBlockers(defender, emptyMap()).outcome shouldBe Outcome.Done
        var guard = 0
        while (state.priorityPlayerId != null && state.priorityPlayerId != attacker &&
            state.step == Step.DECLARE_BLOCKERS && guard++ < 4
        ) {
            passPriority(state.priorityPlayerId!!)
        }
    }

    test("Ninja Teen L3: a graveyard creature is castable from the graveyard via granted sneak") {
        val d = createDriver()
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)

        // Ninja Teen at level 3 grants graveyard creature cards sneak {3}{B}.
        d.putPermanentOnBattlefield(me, "Ninja Teen", classLevel = 3)
        // A creature in my graveyard (no printed sneak) + an unblocked attacker to return.
        val goonId = d.putCardInGraveyard(me, "Graveyard Goon")
        val brawlerId = d.putCreatureOnBattlefield(me, "Plain Brawler")
        d.removeSummoningSickness(brawlerId)

        d.openSneakWindow(me, opp, brawlerId)

        // Pay the granted sneak mana ({3}{B} = 4 black) and return the unblocked Brawler.
        d.giveMana(me, Color.BLACK, 4)
        val cast = d.submit(
            CastSpell(
                playerId = me,
                cardId = goonId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SNEAK,
                additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(brawlerId)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        withClue("error=${cast.error} pendingDecision=${cast.pendingDecision}") {
            cast.outcome shouldBe Outcome.Done
        }
        while (d.state.stack.isNotEmpty()) d.bothPass()

        // The Goon resolved from the graveyard onto the battlefield, tapped (sneak, CR 702.190b).
        val goonPerm = d.findPermanent(me, "Graveyard Goon")
        goonPerm.shouldNotBeNull()
        d.state.getEntity(goonPerm)?.has<TappedComponent>() shouldBe true
        // The Brawler was returned to its owner's hand as part of the sneak cost.
        d.getHand(me) shouldContain brawlerId
    }

    test("without the level-3 grant, a graveyard creature is NOT sneak-castable") {
        val d = createDriver()
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)

        // Ninja Teen only at level 2 — the graveyard-sneak grant is inactive.
        d.putPermanentOnBattlefield(me, "Ninja Teen", classLevel = 2)
        val goonId = d.putCardInGraveyard(me, "Graveyard Goon")
        val brawlerId = d.putCreatureOnBattlefield(me, "Plain Brawler")
        d.removeSummoningSickness(brawlerId)

        d.openSneakWindow(me, opp, brawlerId)
        d.giveMana(me, Color.BLACK, 4)
        val cast = d.submit(
            CastSpell(
                playerId = me,
                cardId = goonId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SNEAK,
                additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(brawlerId)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        cast.outcome shouldNotBe Outcome.Done
    }

    test("Ninja Teen levelled to 3 in play offers a graveyard sneak cast in legal actions") {
        val d = createDriver()
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)

        val teen = d.putPermanentOnBattlefield(me, "Ninja Teen", classLevel = 1)
        repeat(6) { d.putLandOnBattlefield(me, "Swamp") }
        val goonId = d.putCardInGraveyard(me, "Graveyard Goon")
        val brawlerId = d.putCreatureOnBattlefield(me, "Plain Brawler")
        d.removeSummoningSickness(brawlerId)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        for (level in 2..3) {
            val r = d.submit(com.wingedsheep.engine.core.ActivateAbility(me, teen,
                com.wingedsheep.sdk.scripting.AbilityId.classLevelUp(level)))
            withClue("level $level: ${r.error}") { r.error shouldBe null }
            while (d.state.stack.isNotEmpty()) d.bothPass()
        }
        d.state.getEntity(teen)?.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()
            ?.currentLevel shouldBe 3
        // Untap lands for the sneak payment.
        d.state.getBattlefield().forEach { id ->
            if (d.state.getEntity(id)?.has<TappedComponent>() == true) d.untapPermanent(id)
        }

        d.openSneakWindow(me, opp, brawlerId)
        val offers = d.legalActions(me).filter { (it.action as? CastSpell)?.cardId == goonId }
        withClue("offers=${offers.map { it.description }}") { offers.size shouldBe 1 }
        // The client only shows graveyard cards whose offer names the graveyard as its source.
        offers.single().sourceZone shouldBe "GRAVEYARD"

        val offered = offers.single().action as CastSpell
        val cast = d.submit(offered.copy(
            additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(brawlerId)),
            paymentStrategy = PaymentStrategy.AutoPay
        ))
        withClue("error=${cast.error} pendingDecision=${cast.pendingDecision}") { cast.outcome shouldBe Outcome.Done }
        while (d.state.stack.isNotEmpty()) d.bothPass()
        val goonPerm = d.findPermanent(me, "Graveyard Goon")
        goonPerm.shouldNotBeNull()
        d.state.getEntity(goonPerm)?.has<com.wingedsheep.engine.state.components.combat.AttackingComponent>() shouldBe true
    }
})
