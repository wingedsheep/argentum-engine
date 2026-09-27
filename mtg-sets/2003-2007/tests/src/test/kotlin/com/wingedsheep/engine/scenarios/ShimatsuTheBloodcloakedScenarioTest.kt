package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.ShimatsuTheBloodcloaked
import com.wingedsheep.mtg.sets.definitions.lrw.cards.MakeshiftMannequin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Shimatsu the Bloodcloaked (CHK #186) — {3}{R} 0/0 Legendary Creature — Demon Spirit.
 *
 * "As Shimatsu enters, sacrifice any number of permanents. Shimatsu enters with that many +1/+1
 * counters on it."
 */
class ShimatsuTheBloodcloakedScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(ShimatsuTheBloodcloaked, MakeshiftMannequin))
        d.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Grizzly Bears" to 30),
            skipMulligans = true,
            startingPlayer = 0
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("sacrifices the chosen permanents of any type and enters with that many +1/+1 counters") {
        val d = newDriver()
        val p1 = d.player1
        val p2 = d.player2

        val shimatsu = d.putCardInHand(p1, "Shimatsu the Bloodcloaked")
        val landA = d.putLandOnBattlefield(p1, "Mountain")
        val landB = d.putLandOnBattlefield(p1, "Mountain")
        val bear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val theirBear = d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        d.giveMana(p1, Color.RED, 4)

        d.submit(CastSpell(playerId = p1, cardId = shimatsu))
        d.bothPass()

        val sel = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("only your own permanents, and never Shimatsu itself") {
            sel.options.contains(theirBear) shouldBe false
            sel.options.contains(shimatsu) shouldBe false
            sel.options.toSet() shouldBe setOf(landA, landB, bear)
        }
        d.submitDecision(p1, CardsSelectedResponse(decisionId = sel.id, selectedCards = listOf(landA, bear)))

        d.isPaused shouldBe false
        d.getGraveyard(p1) shouldContain landA
        d.getGraveyard(p1) shouldContain bear
        d.findPermanent(p1, "Mountain") shouldBe landB
        val body = d.findPermanent(p1, "Shimatsu the Bloodcloaked").shouldNotBeNull()
        d.plusCounters(body) shouldBe 2
        d.state.projectedState.getPower(body) shouldBe 2
        d.state.projectedState.getToughness(body) shouldBe 2
    }

    test("sacrificing nothing leaves a 0/0 that dies to state-based actions") {
        val d = newDriver()
        val p1 = d.player1

        val shimatsu = d.putCardInHand(p1, "Shimatsu the Bloodcloaked")
        val land = d.putLandOnBattlefield(p1, "Mountain")
        d.giveMana(p1, Color.RED, 4)

        d.submit(CastSpell(playerId = p1, cardId = shimatsu))
        d.bothPass()

        val sel = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(p1, CardsSelectedResponse(decisionId = sel.id, selectedCards = emptyList()))

        d.findPermanent(p1, "Shimatsu the Bloodcloaked") shouldBe null
        d.getGraveyard(p1) shouldContain shimatsu
        d.findPermanent(p1, "Mountain") shouldBe land
    }

    test("the as-enters sacrifice also applies when Shimatsu is returned from the graveyard") {
        val d = newDriver()
        val p1 = d.player1

        val corpse = d.putCardInGraveyard(p1, "Shimatsu the Bloodcloaked")
        val bear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val mannequin = d.putCardInHand(p1, "Makeshift Mannequin")
        d.giveMana(p1, Color.BLACK, 4)

        d.castSpellWithTargets(p1, mannequin, listOf(entityIdToChosenTarget(d.state, corpse)))
        d.bothPass()

        val sel = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        sel.options.toSet() shouldBe setOf(bear)
        d.submitDecision(p1, CardsSelectedResponse(decisionId = sel.id, selectedCards = listOf(bear)))

        d.getGraveyard(p1) shouldContain bear
        val body = d.findPermanent(p1, "Shimatsu the Bloodcloaked").shouldNotBeNull()
        d.plusCounters(body) shouldBe 1
    }
})
