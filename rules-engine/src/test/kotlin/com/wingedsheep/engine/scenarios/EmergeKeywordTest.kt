package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.ElderDeepFiend
import com.wingedsheep.mtg.sets.definitions.emn.cards.WretchedGryff
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Emerge (CR 702.119, Eldritch Moon) — an alternative cost that bundles a creature sacrifice and a
 * cost reduction derived from what was sacrificed:
 *
 *  - 702.119a "Emerge [cost]" means "You may cast this spell by paying [cost] and sacrificing a
 *    creature rather than paying its mana cost", plus "if you chose to pay this spell's emerge
 *    cost, its total cost is reduced by an amount of **generic** mana equal to the sacrificed
 *    creature's mana value."
 *  - 702.119c You choose which permanent to sacrifice as you choose to pay the emerge cost
 *    (CR 601.2b) and sacrifice it as you pay the total cost (CR 601.2h) — i.e. after mana abilities
 *    have been activated, so it can be tapped for mana toward its own emerge cost first.
 *  - Emerge grants no timing permission of its own; the spell is cast at its normal timing.
 *
 * Cards under test:
 *   Wretched Gryff    — {7} 3/4 Flying, Emerge {5}{U}, "When you cast this spell, draw a card."
 *   Elder Deep-Fiend  — {8} 5/6 Flash, Emerge {5}{U}{U}, "When you cast this spell, tap up to four
 *                       target permanents."
 */
class EmergeKeywordTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WretchedGryff)
        driver.registerCard(ElderDeepFiend)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun emergeCast(
        player: com.wingedsheep.sdk.model.EntityId,
        cardId: com.wingedsheep.sdk.model.EntityId,
        sacrifice: com.wingedsheep.sdk.model.EntityId,
        payment: PaymentStrategy = PaymentStrategy.FromPool,
    ) = CastSpell(
        playerId = player,
        cardId = cardId,
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.EMERGE,
        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(sacrifice)),
        paymentStrategy = payment,
    )

    test("emerge pays its cost reduced by the sacrificed creature's mana value") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val gryff = driver.putCardInHand(player, "Wretched Gryff")
        val courser = driver.putCreatureOnBattlefield(player, "Centaur Courser") // {2}{G} → mana value 3
        // Emerge {5}{U} reduced by 3 → {2}{U}, exactly 3 mana.
        driver.giveMana(player, Color.BLUE, 3)

        val handBefore = driver.getHandSize(player)

        driver.submit(emergeCast(player, gryff, courser)).isSuccess shouldBe true

        // CR 702.119c — the creature is sacrificed as the cost is paid, before anything resolves.
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldContain courser

        driver.bothPass() // "When you cast this spell, draw a card" resolves first
        driver.bothPass() // then the creature spell itself

        // Hand: -1 for the Gryff leaving, +1 for the cast trigger's draw.
        driver.getHandSize(player) shouldBe handBefore
        driver.findPermanent(player, "Wretched Gryff") shouldNotBe null
    }

    test("the reduction comes off generic mana only — excess mana value is wasted (CR 702.119a)") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val fiend = driver.putCardInHand(player, "Elder Deep-Fiend")
        val angler = driver.putCreatureOnBattlefield(player, "Gurmag Angler") // {6}{B} → mana value 7
        // Emerge {5}{U}{U}: the mana value 7 wipes out the {5} and the surplus 2 is wasted —
        // {U}{U} still has to be paid.
        driver.giveMana(player, Color.BLUE, 2)

        // The cast succeeds and then pauses on the cast trigger's "up to four target permanents"
        // choice, so assert on the absence of an error rather than on `isSuccess`.
        driver.submit(emergeCast(player, fiend, angler)).error shouldBe null
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldContain angler
    }

    test("a colored pip left after the reduction still has to be paid") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val fiend = driver.putCardInHand(player, "Elder Deep-Fiend")
        val angler = driver.putCreatureOnBattlefield(player, "Gurmag Angler") // mana value 7
        driver.giveMana(player, Color.BLUE, 1) // one short of the surviving {U}{U}

        driver.submitExpectFailure(emergeCast(player, fiend, angler))
        // Nothing was paid: the creature is still on the battlefield and the card still in hand.
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldNotContain angler
        driver.state.getZone(ZoneKey(player, Zone.HAND)) shouldContain fiend
    }

    test("emerge without choosing a creature to sacrifice is rejected") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val gryff = driver.putCardInHand(player, "Wretched Gryff")
        driver.putCreatureOnBattlefield(player, "Centaur Courser")
        driver.giveMana(player, Color.BLUE, 8)

        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = gryff,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.EMERGE,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        driver.state.getZone(ZoneKey(player, Zone.HAND)) shouldContain gryff
    }

    test("a permanent that isn't a creature you control can't pay the emerge sacrifice") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val gryff = driver.putCardInHand(player, "Wretched Gryff")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.giveMana(player, Color.BLUE, 8)

        driver.submitExpectFailure(emergeCast(player, gryff, theirs))
        driver.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD)) shouldNotContain theirs
    }

    test("enumeration offers only sacrifices that leave the reduced cost payable") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val gryff = driver.putCardInHand(player, "Wretched Gryff")
        val lions = driver.putCreatureOnBattlefield(player, "Savannah Lions") // {W} → mana value 1
        val courser = driver.putCreatureOnBattlefield(player, "Centaur Courser") // {2}{G} → mana value 3
        // Four Islands. Sacrificing the Lions leaves {4}{U} (5 mana — unaffordable);
        // sacrificing the Courser leaves {2}{U} (3 mana — affordable).
        repeat(4) { driver.putLandOnBattlefield(player, "Island") }

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)

        val emerge = actions.firstOrNull { la ->
            (la.action as? CastSpell)?.cardId == gryff &&
                la.action.alternativeCostType == AlternativeCostType.EMERGE
        }
        emerge shouldNotBe null
        emerge!!.actionType shouldBe "CastWithAlternativeCost"
        val candidates = emerge.additionalCostInfo!!.validSacrificeTargets
        candidates shouldContain courser
        candidates shouldNotContain lions
    }

    test("enumeration reports the cost each candidate leaves, so the client can show it") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val gryff = driver.putCardInHand(player, "Wretched Gryff")
        val courser = driver.putCreatureOnBattlefield(player, "Centaur Courser") // mana value 3
        val angler = driver.putCreatureOnBattlefield(player, "Gurmag Angler") // mana value 7
        repeat(6) { driver.putLandOnBattlefield(player, "Island") }

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
        val emerge = actions.first { la ->
            (la.action as? CastSpell)?.cardId == gryff &&
                la.action.alternativeCostType == AlternativeCostType.EMERGE
        }

        val costs = emerge.additionalCostInfo!!.costAfterSacrifice
        // Emerge {5}{U} minus 3 → {2}{U}; minus 7 → the generic is exhausted and {U} survives,
        // the surplus 4 wasted (CR 702.119a). The client renders these verbatim, so they are the
        // player-visible contract.
        costs[courser] shouldBe "{2}{U}"
        costs[angler] shouldBe "{U}"

        // The two halves the cast button renders as live arithmetic ("{5}{U} → as low as {U}"): the
        // label names the mechanic only, and the un-reduced emerge cost is the starting point the
        // per-candidate costs above reduce. Spelling the rule out inside the label instead would
        // leave the button showing a price it never charges.
        emerge.description shouldBe "Emerge Wretched Gryff"
        emerge.manaCostString shouldBe "{5}{U}"
    }

    test("no emerge option at all when the reduced cost is unaffordable for every creature") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val gryff = driver.putCardInHand(player, "Wretched Gryff")
        driver.putCreatureOnBattlefield(player, "Savannah Lions") // mana value 1 → leaves {4}{U}
        repeat(2) { driver.putLandOnBattlefield(player, "Island") }

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)

        actions.none { la ->
            (la.action as? CastSpell)?.cardId == gryff &&
                la.action.alternativeCostType == AlternativeCostType.EMERGE
        } shouldBe true
    }

    test("no emerge option when you control no creature to sacrifice") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val gryff = driver.putCardInHand(player, "Wretched Gryff")
        repeat(8) { driver.putLandOnBattlefield(player, "Island") }

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)

        actions.none { la ->
            (la.action as? CastSpell)?.cardId == gryff &&
                la.action.alternativeCostType == AlternativeCostType.EMERGE
        } shouldBe true
    }

    test("emerge grants no timing permission — only a flash spell is offered outside a main phase") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val gryff = driver.putCardInHand(player, "Wretched Gryff") // no flash
        val fiend = driver.putCardInHand(player, "Elder Deep-Fiend") // flash
        driver.putCreatureOnBattlefield(player, "Gurmag Angler") // mana value 7 — covers both costs
        repeat(4) { driver.putLandOnBattlefield(player, "Island") }
        driver.passPriorityUntil(Step.END)

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
        fun emergeFor(cardId: com.wingedsheep.sdk.model.EntityId) = actions.any { la ->
            (la.action as? CastSpell)?.cardId == cardId &&
                la.action.alternativeCostType == AlternativeCostType.EMERGE
        }

        emergeFor(gryff) shouldBe false
        emergeFor(fiend) shouldBe true
    }

    test("the sacrificed creature may be tapped for mana toward its own emerge cost (CR 601.2f-h)") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val gryff = driver.putCardInHand(player, "Wretched Gryff")
        val birds = driver.putCreatureOnBattlefield(player, "Birds of Paradise") // {G} → mana value 1
        driver.removeSummoningSickness(birds)
        // Emerge {5}{U} reduced by 1 → {4}{U} = 5 mana. Only four Islands are on the battlefield,
        // so the fifth mana has to come from the Birds — which is then sacrificed to pay the rest
        // of the cost. CR 601.2f-g put mana abilities before the total cost is paid in 601.2h.
        repeat(4) { driver.putLandOnBattlefield(player, "Island") }

        driver.submit(emergeCast(player, gryff, birds, payment = PaymentStrategy.AutoPay))
            .isSuccess shouldBe true

        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldContain birds
        driver.bothPass()
        driver.bothPass()
        driver.findPermanent(player, "Wretched Gryff") shouldNotBe null
    }

    test("a card with emerge cast normally pays its printed cost and sacrifices nothing") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val gryff = driver.putCardInHand(player, "Wretched Gryff")
        val courser = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        driver.giveMana(player, Color.BLUE, 7) // the printed {7}

        driver.submit(
            CastSpell(playerId = player, cardId = gryff, paymentStrategy = PaymentStrategy.FromPool)
        ).isSuccess shouldBe true

        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldNotContain courser
        driver.bothPass()
        driver.bothPass()
        driver.findPermanent(player, "Wretched Gryff") shouldNotBe null
    }
})
