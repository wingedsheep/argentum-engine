package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Paladin Elizabeth Taggerdy (PIP) — Battalion: "Whenever Paladin Elizabeth Taggerdy and at least
 * two other creatures attack, draw a card, then you may put a creature card with mana value X or
 * less from your hand onto the battlefield tapped and attacking, where X is Paladin Elizabeth
 * Taggerdy's power."
 *
 * Pins the order (the card just drawn is eligible), X read from Elizabeth's power (3: a mana value
 * 3 card qualifies, a mana value 4 card doesn't), the tapped-and-attacking put, and that battalion
 * needs three attackers.
 */
class PaladinElizabethTaggerdyScenarioTest : ScenarioTestBase() {

    private fun creature(name: String, cost: String) = card(name) {
        manaCost = cost
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 2
    }

    private val squireA = creature("Taggerdy Test Squire A", "{W}")
    private val squireB = creature("Taggerdy Test Squire B", "{W}")
    private val recruit = creature("Taggerdy Test Recruit", "{1}{W}")    // MV 2, in library
    private val knight = creature("Taggerdy Test Knight", "{2}{W}")      // MV 3, eligible
    private val vertibird = creature("Taggerdy Test Vertibird", "{3}{W}") // MV 4, too big

    private fun battalion(attackers: List<String>): TestGame {
        val game = scenario()
            .withPlayers("You", "Opponent")
            .withCardOnBattlefield(1, "Paladin Elizabeth Taggerdy")
            .withCardOnBattlefield(1, squireA.name)
            .withCardOnBattlefield(1, squireB.name)
            .withCardInHand(1, knight.name)
            .withCardInHand(1, vertibird.name)
            .withCardInLibrary(1, recruit.name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(attackers.associateWith { 2 }).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        listOf(squireA, squireB, recruit, knight, vertibird).forEach { cardRegistry.register(it) }

        test("three attackers: draw, then put a creature card with mana value up to her power in tapped and attacking") {
            val game = battalion(listOf("Paladin Elizabeth Taggerdy", squireA.name, squireB.name))

            val decision = game.getPendingDecision()
            withClue("the battalion trigger offers the hand selection") { (decision is SelectCardsDecision) shouldBe true }
            decision as SelectCardsDecision
            val selectable = decision.options - decision.nonSelectableOptions.toSet()
            val drawn = game.findCardsInHand(1, recruit.name).single()
            val knightCard = game.findCardsInHand(1, knight.name).single()
            val tooBig = game.findCardsInHand(1, vertibird.name).single()
            withClue("the card drawn first is already eligible") { selectable shouldContain drawn }
            withClue("mana value 3 = her power qualifies") { selectable shouldContain knightCard }
            withClue("mana value 4 is above her power") { selectable shouldNotContain tooBig }

            game.selectCards(listOf(knightCard)).error shouldBe null
            game.resolveStack()

            val put = game.findPermanent(knight.name)!!
            game.state.getEntity(put)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(put)?.has<AttackingComponent>() shouldBe true
            game.isInHand(1, recruit.name) shouldBe true
            game.isInHand(1, vertibird.name) shouldBe true
        }

        test("the put is optional") {
            val game = battalion(listOf("Paladin Elizabeth Taggerdy", squireA.name, squireB.name))
            (game.getPendingDecision() is SelectCardsDecision) shouldBe true
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.isInHand(1, recruit.name) shouldBe true
            game.isInHand(1, knight.name) shouldBe true
            game.findPermanent(knight.name) shouldBe null
        }

        test("two attackers isn't battalion — no draw, no put") {
            val game = battalion(listOf("Paladin Elizabeth Taggerdy", squireA.name))

            game.hasPendingDecision() shouldBe false
            game.isInHand(1, recruit.name) shouldBe false
            game.handSize(1) shouldBe 2
        }
    }
}
