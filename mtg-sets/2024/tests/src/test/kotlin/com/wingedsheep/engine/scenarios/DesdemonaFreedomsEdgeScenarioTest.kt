package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.DesdemonaFreedomsEdge
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Desdemona, Freedom's Edge (PIP #101) — {2}{R}{W} 3/4 Legendary Creature — Human Rogue.
 *
 * "Vigilance. Whenever Desdemona attacks, target creature card in your graveyard that's an artifact
 *  or that has mana value 3 or less gains escape until end of turn. The escape cost is equal to its
 *  mana cost plus exile two other cards from your graveyard."
 *
 * The attack trigger targets only your creature cards that are artifacts or have mana value 3 or
 * less; the chosen card can then be cast from the graveyard for its mana cost plus exiling two
 * other graveyard cards, and the permanent escaped (CR 702.138b).
 */
class DesdemonaFreedomsEdgeScenarioTest : FunSpec({

    val bigConstruct = card("Desdemona Test Construct") {
        manaCost = "{5}"
        typeLine = "Artifact Creature — Construct"
        power = 4
        toughness = 4
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DesdemonaFreedomsEdge, bigConstruct))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("attack trigger targets an artifact or MV<=3 creature card you own; it then escapes from the graveyard") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.player2

        val desdemona = driver.putCreatureOnBattlefield(you, "Desdemona, Freedom's Edge")
        driver.removeSummoningSickness(desdemona)
        val bears = driver.putCardInGraveyard(you, "Grizzly Bears")                 // MV 2
        val construct = driver.putCardInGraveyard(you, "Desdemona Test Construct")  // artifact, MV 5
        driver.putCardInGraveyard(you, "Force of Nature")                           // MV 5, not artifact
        val others = List(2) { driver.putCardInGraveyard(you, "Mountain") }          // not creatures
        driver.putCardInGraveyard(opponent, "Grizzly Bears")                        // not yours

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(desdemona), opponent)

        val decision = driver.pendingDecision as? ChooseTargetsDecision
        decision.shouldNotBeNull()
        decision.legalTargets[0].shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(bears, construct)
        driver.submitTargetSelection(you, listOf(bears))
        driver.bothPass()

        driver.state.grantedKeywordAbilities.any { it.entityId == bears } shouldBe true

        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, you)
            .filter { (it.action as? CastSpell)?.cardId == bears }.shouldBeEmpty()
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.giveMana(you, Color.GREEN, 2)
        val offer = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, you)
            .single { (it.action as? CastSpell)?.let { a -> a.cardId == bears && a.alternativeCostType == AlternativeCostType.ESCAPE } == true }
        offer.manaCostString shouldBe "{1}{G}"

        val result = driver.submit(
            CastSpell(
                playerId = you,
                cardId = bears,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = others),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.getExile(you) shouldContainAll others
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val perm = driver.findPermanent(you, "Grizzly Bears").shouldNotBeNull()
        driver.state.getEntity(perm)?.get<CastChoicesComponent>()?.chosen?.containsKey(ChoiceSlot.ESCAPED) shouldBe true
        // Desdemona has vigilance: attacking didn't tap her.
        driver.isTapped(desdemona) shouldBe false
    }

    test("the grant lapses at end of turn") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.player2

        val desdemona = driver.putCreatureOnBattlefield(you, "Desdemona, Freedom's Edge")
        driver.removeSummoningSickness(desdemona)
        val bears = driver.putCardInGraveyard(you, "Grizzly Bears")
        driver.putCardInGraveyard(you, "Desdemona Test Construct")
        List(2) { driver.putCardInGraveyard(you, "Mountain") }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(desdemona), opponent)
        driver.submitTargetSelection(you, listOf(bears))
        driver.bothPass()
        driver.state.grantedKeywordAbilities.any { it.entityId == bears } shouldBe true

        driver.passPriorityUntil(Step.UPKEEP, opponent)
        driver.state.grantedKeywordAbilities.shouldBeEmpty()
    }
})
