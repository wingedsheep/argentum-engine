package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.SylvanPrimordial
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sylvan Primordial — "When this creature enters, for each opponent, destroy target noncreature
 * permanent that player controls. Search your library for up to X Forest cards, where X is the
 * number of permanents destroyed this way, put those cards onto the battlefield tapped, then shuffle."
 */
class SylvanPrimordialScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SylvanPrimordial))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castPrimordial(driver: GameTestDriver, me: EntityId) {
        val primordial = driver.putCardInHand(me, "Sylvan Primordial")
        driver.giveMana(me, Color.GREEN, 7)
        driver.castSpell(me, primordial).error.shouldBeNull()
        driver.bothPass()
    }

    fun forests(driver: GameTestDriver, player: EntityId): List<EntityId> =
        driver.getPermanents(player).filter { driver.state.getEntity(it)?.get<CardComponent>()?.name == "Forest" }

    test("destroys the opponent's noncreature permanent and fetches one Forest tapped") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val land = driver.putLandOnBattlefield(opponent, "Forest")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        castPrimordial(driver, me)

        val targets = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        targets.targetRequirements.single().minTargets shouldBe 1
        driver.submitTargetSelection(me, listOf(land)).error.shouldBeNull()
        driver.bothPass()

        driver.getGraveyard(opponent) shouldContain land
        val search = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        search.maxSelections shouldBe 1
        driver.submitCardSelection(me, search.options.take(1)).error.shouldBeNull()

        val fetched = forests(driver, me)
        fetched.size shouldBe 1
        driver.isTapped(fetched.single()) shouldBe true
    }

    test("with no noncreature permanent to target, nothing is destroyed and nothing is fetched") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        castPrimordial(driver, me)
        if (driver.pendingDecision is ChooseTargetsDecision) {
            driver.submitTargetSelection(me, emptyList()).error.shouldBeNull()
        }
        driver.bothPass()

        driver.getPermanents(opponent) shouldContain bears
        forests(driver, me).size shouldBe 0
    }
})
