package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Divided damage scoped to one target requirement of a spell that targets more than once
 * (`DividedDamage(among = …)` — Magma Opus: "deals 4 damage divided as you choose among any number
 * of targets. Tap two target permanents."). The division is announced as the spell is cast among
 * that requirement's targets only (CR 601.2d), each getting at least 1; the other requirement's
 * targets take none of it, the same object may be chosen for both target words, and a target that
 * becomes illegal loses its share without the rest being re-divided.
 */
class ScopedDividedDamageTest : FunSpec({
    val opus = card("Scoped Opus Probe") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell {
            val damaged = targets(AnyTarget(count = 3, minCount = 0))
            val (first, second) = targets(TargetFilter.Permanent, count = 2)
            effect = Effects.DividedDamage(total = 3, minTargets = 0, maxTargets = 3, among = damaged) then
                Effects.Tap(first) then
                Effects.Tap(second)
        }
    }

    fun newGame(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + opus)
        d.initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.damage(id: EntityId): Int = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    fun GameTestDriver.target(id: EntityId): ChosenTarget =
        if (id == player1 || id == player2) ChosenTarget.Player(id) else ChosenTarget.Permanent(id)

    fun GameTestDriver.castOpus(
        damaged: List<EntityId>,
        tapped: List<EntityId>,
        division: Map<EntityId, Int>?
    ): com.wingedsheep.engine.core.ExecutionResult {
        val spell = putCardInHand(player1, "Scoped Opus Probe")
        giveMana(player1, Color.RED, 1)
        return submit(
            CastSpell(
                playerId = player1,
                cardId = spell,
                targets = (damaged + tapped).map { target(it) },
                targetGroupCounts = listOf(damaged.size, tapped.size),
                damageDistribution = division,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
    }

    test("the cast offer divides among the first requirement only") {
        val d = newGame()
        d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        d.putCardInHand(d.player1, "Scoped Opus Probe")
        d.giveMana(d.player1, Color.RED, 1)

        val offer = d.legalActions(d.player1).first { it.description == "Cast Scoped Opus Probe" }
        offer.requiresDamageDistribution shouldBe true
        offer.totalDamageToDistribute shouldBe 3
        offer.distributionTargetGroup shouldBe 0
    }

    test("damage is dealt only to the divided group; the tapped permanents take none of it") {
        val d = newGame()
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        val bears = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")

        d.castOpus(listOf(d.player2, courser), listOf(lions, bears), mapOf(d.player2 to 1, courser to 2)).error shouldBe null
        d.state.getEntity(d.getTopOfStack()!!)!!.get<SpellOnStackComponent>()!!.damageDistribution shouldBe
            mapOf(d.player2 to 1, courser to 2)
        d.bothPass().error shouldBe null

        d.getLifeTotal(d.player2) shouldBe 19
        d.damage(courser) shouldBe 2
        d.damage(lions) shouldBe 0
        d.damage(bears) shouldBe 0
        d.isTapped(lions) shouldBe true
        d.isTapped(bears) shouldBe true
        d.isTapped(courser) shouldBe false
    }

    test("a division naming a permanent outside the divided group is rejected") {
        val d = newGame()
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        val bears = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")

        d.castOpus(listOf(courser, d.player2), listOf(lions, bears), mapOf(courser to 1, d.player2 to 1, lions to 1))
            .error shouldNotBe null
        d.stackSize shouldBe 0
    }

    test("two or more divided targets need an announced division") {
        val d = newGame()
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        val bears = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")

        d.castOpus(listOf(courser, d.player2), listOf(lions, bears), null).error shouldNotBe null
        d.stackSize shouldBe 0
    }

    test("the same creature may be damaged and tapped; a lone divided target takes it all") {
        val d = newGame()
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")

        d.castOpus(listOf(courser), listOf(courser, lions), null).error shouldBe null
        d.bothPass().error shouldBe null

        d.damage(courser) shouldBe 0 // 3 damage to a 3/3 — it died
        d.state.getBattlefield().contains(courser) shouldBe false
        d.isTapped(lions) shouldBe true
    }

    test("any number includes none — the spell still taps its two permanents") {
        val d = newGame()
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")

        d.castOpus(emptyList(), listOf(courser, lions), null).error shouldBe null
        d.bothPass().error shouldBe null

        d.getLifeTotal(d.player2) shouldBe 20
        d.damage(courser) shouldBe 0
        d.isTapped(courser) shouldBe true
        d.isTapped(lions) shouldBe true
    }

    test("a divided target removed in response loses its share; the others keep theirs") {
        val d = newGame()
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        val bears = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")

        d.castOpus(listOf(lions, courser), listOf(bears, courser), mapOf(lions to 2, courser to 1)).error shouldBe null
        // Player 2 bolts their own Lions in response.
        d.passPriority(d.player1).error shouldBe null
        val bolt = d.putCardInHand(d.player2, "Lightning Bolt")
        d.giveMana(d.player2, Color.RED, 1)
        d.castSpell(d.player2, bolt, listOf(lions)).error shouldBe null
        d.bothPass().error shouldBe null
        d.state.getBattlefield().contains(lions) shouldBe false
        d.bothPass().error shouldBe null

        d.damage(courser) shouldBe 1
        d.damage(bears) shouldBe 0
        d.isTapped(bears) shouldBe true
        d.isTapped(courser) shouldBe true
    }
})
