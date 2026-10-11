package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.war.cards.NissaWhoShakesTheWorld
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Nissa, Who Shakes the World {3}{G}{G} — Legendary Planeswalker — Nissa (loyalty 5)
 *   +1: Put three +1/+1 counters on up to one target noncreature land you control. Untap it. It
 *       becomes a 0/0 Elemental creature with vigilance and haste that's still a land.
 *   −8: You get an emblem with "Lands you control have indestructible." Search your library for
 *       any number of Forest cards, put them onto the battlefield tapped, then shuffle.
 *
 * The +1 must land the counters on a 0/0 that only becomes a creature afterwards, and the
 * animation must outlive the turn (2019-05-03 ruling). The −8's emblem grants a keyword to a
 * noncreature group, which the creature-only emblems elsewhere never exercise.
 */
class NissaWhoShakesTheWorldScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(NissaWhoShakesTheWorld))
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun putNissa(driver: GameTestDriver, playerId: EntityId, loyalty: Int): EntityId {
        val nissa = driver.putPermanentOnBattlefield(playerId, "Nissa, Who Shakes the World")
        driver.addComponent(nissa, CountersComponent(mapOf(CounterType.LOYALTY to loyalty)))
        return nissa
    }

    val abilities = NissaWhoShakesTheWorld.script.activatedAbilities

    test("+1 makes a tapped land an untapped 3/3 Elemental land creature with vigilance and haste, permanently") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val nissa = putNissa(driver, me, 5)
        val forest = driver.putLandOnBattlefield(me, "Forest")
        driver.tapPermanent(forest)

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = nissa,
                abilityId = abilities[0].id,
                targets = listOf(ChosenTarget.Permanent(forest))
            )
        )
        driver.bothPass()

        driver.isTapped(forest) shouldBe false
        driver.state.getEntity(forest)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3

        fun assertAnimated() {
            val projected = driver.state.projectedState
            projected.isCreature(forest) shouldBe true
            projected.hasType(forest, "LAND") shouldBe true
            projected.hasSubtype(forest, "Elemental") shouldBe true
            projected.getPower(forest) shouldBe 3
            projected.getToughness(forest) shouldBe 3
            projected.hasKeyword(forest, Keyword.VIGILANCE) shouldBe true
            projected.hasKeyword(forest, Keyword.HASTE) shouldBe true
        }
        assertAnimated()

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        withClue("the animation lasts indefinitely, not until end of turn") { assertAnimated() }
    }

    test("−8 grants lands indestructible via emblem and fetches any number of Forests tapped") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val nissa = putNissa(driver, me, 8)
        val bears = driver.putPermanentOnBattlefield(me, "Grizzly Bears")
        val existingForest = driver.putLandOnBattlefield(me, "Forest")
        val libForestA = driver.putCardOnTopOfLibrary(me, "Forest")
        val libForestB = driver.putCardOnTopOfLibrary(me, "Forest")

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = nissa, abilityId = abilities[1].id))

        var offered: List<EntityId> = emptyList()
        var safety = 0
        while (safety++ < 40) {
            when (val pending = driver.state.pendingDecision) {
                is SelectCardsDecision -> {
                    offered = pending.options
                    driver.submitCardSelection(pending.playerId, pending.options)
                }
                null -> if (driver.stackSize > 0) driver.bothPass() else break
                else -> error("unexpected decision $pending")
            }
        }

        withClue("only the Forests were offered") { offered.toSet() shouldBe setOf(libForestA, libForestB) }
        driver.state.getBattlefield().containsAll(listOf(libForestA, libForestB)) shouldBe true
        driver.isTapped(libForestA) shouldBe true
        driver.isTapped(libForestB) shouldBe true

        val projected = driver.state.projectedState
        listOf(existingForest, libForestA, libForestB).forEach {
            projected.hasKeyword(it, Keyword.INDESTRUCTIBLE) shouldBe true
        }
        withClue("the emblem covers lands only") {
            projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe false
        }
    }
})
