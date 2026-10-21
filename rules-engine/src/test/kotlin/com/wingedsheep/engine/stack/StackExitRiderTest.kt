package com.wingedsheep.engine.stack

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GraveyardCastRiderSelection
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.CastThisWayExile
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * "Exile this card instead of putting it anywhere else any time it would leave the stack" — the
 * stack-side clause of flashback (CR 702.34a), aftermath (CR 702.127a) and jump-start (CR 702.133a),
 * read through `StackExitRider` by every stack exit: resolution, a counter, a bounce to hand, and a
 * put-into-library (which then asks no position).
 *
 * The jump-start cases go through a `MayCastFromGraveyard` grant with
 * `exileCastThisWay = ON_ANY_STACK_EXIT` and a discard-a-card additional cost (Niv-Mizzet, Supreme).
 * The graveyard-only rider (`INSTEAD_OF_GRAVEYARD`, Bilbo) is pinned as unchanged: a bounce still
 * returns that spell to hand.
 */
class StackExitRiderTest : FunSpec({

    val insight = card("Gold Insight") {
        manaCost = "{U}{R}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }
    val monoInsight = card("Blue Insight") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }
    val flashInsight = card("Flash Insight") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
        keywordAbility(KeywordAbility.Flashback(ManaCost.parse("{U}")))
    }
    val jumpStartBench = card("Jump-Start Bench") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = MayCastFromGraveyard(
                GameObjectFilter.InstantOrSorcery.withCardPredicate(
                    com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasExactlyColors(2)
                ),
                additionalCost = Costs.additional.DiscardCards(1),
                exileCastThisWay = CastThisWayExile.ON_ANY_STACK_EXIT
            )
        }
    }
    val graveyardOnlyBench = card("Graveyard-Only Bench") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = MayCastFromGraveyard(
                GameObjectFilter.InstantOrSorcery,
                exileCastThisWay = CastThisWayExile.INSTEAD_OF_GRAVEYARD
            )
        }
    }
    val remand = card("Stack Test Remand") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.ReturnSpellToOwnersHand()
        }
    }
    val counter = card("Stack Test Counter") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.CounterSpell()
        }
    }
    val tuck = card("Stack Test Tuck") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.SpellOnStack)
            effect = Effects.PutOnTopOrBottomOfLibrary(t)
        }
    }

    val jumpStart = GraveyardCastRiderSelection(
        exileCastThisWay = CastThisWayExile.ON_ANY_STACK_EXIT,
        additionalCost = Costs.additional.DiscardCards(1)
    )

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(insight, monoInsight, flashInsight, jumpStartBench, graveyardOnlyBench, remand, counter, tuck)
        )
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.clearHand(player: EntityId) = getHand(player).forEach { moveToGraveyard(it) }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (guard++ < 40 && state.stack.isNotEmpty() && !isPaused) bothPass()
    }

    /** Jump-start Gold Insight out of [player]'s graveyard, discarding [discard]. */
    fun GameTestDriver.jumpStartInsight(player: EntityId, insightId: EntityId, discard: EntityId) {
        giveMana(player, Color.BLUE, 1)
        giveMana(player, Color.RED, 1)
        submit(
            CastSpell(
                player, insightId,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(discard)),
                graveyardCastRider = jumpStart
            )
        ).outcome shouldBe Outcome.Done
        state.stack shouldContain insightId
    }

    fun GameTestDriver.respondWith(player: EntityId, name: String, spellId: EntityId) {
        castSpellWithTargets(player, putCardInHand(player, name), listOf(ChosenTarget.Spell(spellId)))
    }

    fun setUp(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = newDriver()
        val player = d.activePlayer!!
        d.clearHand(player)
        d.putPermanentOnBattlefield(player, "Jump-Start Bench")
        val insightId = d.putCardInGraveyard(player, "Gold Insight")
        return Triple(d, player, insightId)
    }

    test("a two-color instant in the graveyard is offered with a discard, a monocolored one is not") {
        val d = newDriver()
        val player = d.activePlayer!!
        d.clearHand(player)
        d.putPermanentOnBattlefield(player, "Jump-Start Bench")
        val gold = d.putCardInGraveyard(player, "Gold Insight")
        val mono = d.putCardInGraveyard(player, "Blue Insight")
        d.putCardInHand(player, "Island")
        d.giveMana(player, Color.BLUE, 1)
        d.giveMana(player, Color.RED, 1)

        val casts = d.legalActions(player).filter { it.sourceZone == "GRAVEYARD" }
        casts.map { (it.action as CastSpell).cardId } shouldContain gold
        casts.map { (it.action as CastSpell).cardId } shouldNotContain mono
    }

    test("a jump-started spell is exiled as it resolves, and the discarded card goes to the graveyard") {
        val (d, player, insightId) = setUp()
        val discard = d.putCardInHand(player, "Island")
        d.jumpStartInsight(player, insightId, discard)
        d.resolveAll()

        d.getExile(player) shouldContain insightId
        d.getGraveyard(player) shouldNotContain insightId
        d.getGraveyard(player) shouldContain discard
    }

    test("a countered jump-started spell is exiled") {
        val (d, player, insightId) = setUp()
        d.jumpStartInsight(player, insightId, d.putCardInHand(player, "Island"))
        d.respondWith(player, "Stack Test Counter", insightId)
        d.resolveAll()

        d.getExile(player) shouldContain insightId
        d.getGraveyard(player) shouldNotContain insightId
    }

    test("a jump-started spell bounced to hand is exiled instead") {
        val (d, player, insightId) = setUp()
        d.jumpStartInsight(player, insightId, d.putCardInHand(player, "Island"))
        d.respondWith(player, "Stack Test Remand", insightId)
        d.resolveAll()

        d.getExile(player) shouldContain insightId
        d.getHand(player) shouldNotContain insightId
    }

    test("a jump-started spell put into its library is exiled, and its owner is never asked where") {
        val (d, player, insightId) = setUp()
        d.jumpStartInsight(player, insightId, d.putCardInHand(player, "Island"))
        d.respondWith(player, "Stack Test Tuck", insightId)
        d.bothPass() // resolve the tuck

        d.pendingDecision.shouldBeNull()
        d.getExile(player) shouldContain insightId
        d.state.getLibrary(player) shouldNotContain insightId
    }

    test("a flashback spell bounced to hand is exiled instead") {
        val d = newDriver()
        val player = d.activePlayer!!
        val flashId = d.putCardInGraveyard(player, "Flash Insight")
        d.giveMana(player, Color.BLUE, 1)
        d.submit(
            CastSpell(player, flashId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.FLASHBACK)
        ).outcome shouldBe Outcome.Done
        d.respondWith(player, "Stack Test Remand", flashId)
        d.resolveAll()

        d.getExile(player) shouldContain flashId
        d.getHand(player) shouldNotContain flashId
    }

    test("the graveyard-only rider still lets a bounce return the spell to hand") {
        val d = newDriver()
        val player = d.activePlayer!!
        d.putPermanentOnBattlefield(player, "Graveyard-Only Bench")
        val insightId = d.putCardInGraveyard(player, "Gold Insight")
        d.giveMana(player, Color.BLUE, 1)
        d.giveMana(player, Color.RED, 1)
        d.submit(
            CastSpell(
                player, insightId,
                graveyardCastRider = GraveyardCastRiderSelection(exileCastThisWay = CastThisWayExile.INSTEAD_OF_GRAVEYARD)
            )
        ).outcome shouldBe Outcome.Done
        d.respondWith(player, "Stack Test Remand", insightId)
        d.resolveAll()

        d.getHand(player) shouldContain insightId
        d.getExile(player) shouldNotContain insightId
    }
})
