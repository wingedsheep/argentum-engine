package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Shatter
import com.wingedsheep.mtg.sets.definitions.m11.cards.PhylacteryLich
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Phylactery Lich (M11 #110) — {B}{B}{B} 5/5 Creature — Zombie.
 *
 * "Indestructible. As this creature enters, put a phylactery counter on an artifact you control.
 *  When you control no permanents with phylactery counters on them, sacrifice this creature."
 *
 * The counter goes on an artifact you control chosen as the Lich enters (not targeted), and the
 * state trigger sacrifices the Lich — indestructible doesn't stop a sacrifice — whenever no
 * permanent you control carries a phylactery counter, whichever Lich put it there.
 */
class PhylacteryLichScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(PhylacteryLich, Shatter))
        d.initMirrorMatch(
            deck = Deck.of("Swamp" to 30, "Grizzly Bears" to 30),
            skipMulligans = true,
            startingPlayer = 0
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.phylacteryCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PHYLACTERY) ?: 0

    /** Casts a Lich and resolves it, choosing [artifact] for the counter when asked. */
    fun GameTestDriver.castLich(artifact: EntityId?): SelectCardsDecision? {
        val lich = putCardInHand(player1, "Phylactery Lich")
        giveMana(player1, Color.BLACK, 3)
        submit(CastSpell(playerId = player1, cardId = lich))
        bothPass()
        val decision = pendingDecision as? SelectCardsDecision
        if (decision != null && artifact != null) {
            submitDecision(player1, CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(artifact)))
        }
        return decision
    }

    fun GameTestDriver.shatter(artifact: EntityId) {
        val shatter = putCardInHand(player1, "Shatter")
        giveMana(player1, Color.RED, 2)
        castSpell(player1, shatter, listOf(artifact))
        bothPass()
    }

    test("the counter goes on the chosen artifact you control and the Lich stays") {
        val d = newDriver()
        val mine = d.putCreatureOnBattlefield(d.player1, "Artifact Creature")
        val other = d.putCreatureOnBattlefield(d.player1, "Artifact Creature")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Artifact Creature")
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        val decision = d.castLich(other).shouldNotBeNull()
        decision.prompt shouldBe "Choose an artifact you control — put a phylactery counter on it"
        withClue("only artifacts you control are offered") {
            decision.options shouldContainExactlyInAnyOrder listOf(mine, other)
            decision.options.contains(theirs) shouldBe false
        }
        d.phylacteryCounters(other) shouldBe 1
        d.phylacteryCounters(mine) shouldBe 0

        d.bothPass()
        d.findPermanent(d.player1, "Phylactery Lich").shouldNotBeNull()
        d.state.stack.isEmpty() shouldBe true
    }

    test("with no artifact the counter goes nowhere and the Lich is sacrificed") {
        val d = newDriver()
        d.castLich(artifact = null).shouldBeNull()

        d.findPermanent(d.player1, "Phylactery Lich").shouldNotBeNull()
        d.bothPass()
        d.findPermanent(d.player1, "Phylactery Lich").shouldBeNull()
        d.getGraveyardCardNames(d.player1) shouldContain "Phylactery Lich"
    }

    test("destroying the marked artifact makes the indestructible Lich sacrifice itself") {
        val d = newDriver()
        val artifact = d.putCreatureOnBattlefield(d.player1, "Artifact Creature")
        d.castLich(artifact)
        d.phylacteryCounters(artifact) shouldBe 1

        d.shatter(artifact)
        d.findPermanent(d.player1, "Phylactery Lich").shouldNotBeNull()
        d.bothPass()
        d.findPermanent(d.player1, "Phylactery Lich").shouldBeNull()
    }

    test("any phylactery counter you control keeps every Lich alive") {
        val d = newDriver()
        val first = d.putCreatureOnBattlefield(d.player1, "Artifact Creature")
        val second = d.putCreatureOnBattlefield(d.player1, "Artifact Creature")
        d.castLich(first)
        d.castLich(second)
        d.phylacteryCounters(first) shouldBe 1
        d.phylacteryCounters(second) shouldBe 1

        d.shatter(first)
        d.bothPass()
        withClue("the second artifact's counter covers both Liches") {
            d.state.stack.isEmpty() shouldBe true
            d.getGraveyardCardNames(d.player1).count { it == "Phylactery Lich" } shouldBe 0
        }
    }
})
