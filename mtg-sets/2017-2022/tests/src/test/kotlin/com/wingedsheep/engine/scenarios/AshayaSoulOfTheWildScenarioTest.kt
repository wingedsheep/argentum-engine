package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.znr.cards.AshayaSoulOfTheWild
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ashaya, Soul of the Wild (ZNR #179):
 *   Ashaya's power and toughness are each equal to the number of lands you control.
 *   Nontoken creatures you control are Forest lands in addition to their other types.
 *   (They're still affected by summoning sickness.)
 *
 * Pins the G85 engine path: a permanent that is a land only through a continuous effect gets the
 * intrinsic "{T}: Add {G}." of its projected Forest subtype — offered as a legal action, activatable,
 * and used by auto-pay — while summoning sickness still stops a fresh creature-land from tapping.
 */
class AshayaSoulOfTheWildScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AshayaSoulOfTheWild)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.greenInPool(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<ManaPoolComponent>()?.green ?: 0

    /** An affordable `{T}: Add {G}.` offer — a summoning-sick source is listed, but unaffordable. */
    fun GameTestDriver.offersGreenFrom(playerId: EntityId, sourceId: EntityId): Boolean =
        legalActions(playerId).filter { it.affordable }.map { it.action }.filterIsInstance<ActivateAbility>()
            .any { it.sourceId == sourceId && it.abilityId == AbilityId.intrinsicMana('G') }

    test("nontoken creatures you control, Ashaya included, are Forest lands and Ashaya counts itself") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        repeat(2) { driver.putLandOnBattlefield(p1, "Plains") }
        val ashaya = driver.putCreatureOnBattlefield(p1, "Ashaya, Soul of the Wild")
        val bears = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val opponentBears = driver.putCreatureOnBattlefield(driver.getOpponent(p1), "Grizzly Bears")

        val projected = driver.state.projectedState
        for (id in listOf(ashaya, bears)) {
            projected.hasType(id, "LAND").shouldBeTrue()
            projected.hasType(id, "CREATURE").shouldBeTrue()
            projected.hasSubtype(id, "Forest").shouldBeTrue()
        }
        projected.hasType(opponentBears, "LAND").shouldBeFalse()
        // Two Plains + Ashaya + Grizzly Bears.
        projected.getPower(ashaya) shouldBe 4
        projected.getToughness(ashaya) shouldBe 4
    }

    test("tokens are not affected") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.putCreatureOnBattlefield(p1, "Ashaya, Soul of the Wild")
        val token = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.addComponent(token, TokenComponent)

        driver.state.projectedState.hasType(token, "LAND").shouldBeFalse()
        driver.state.projectedState.hasSubtype(token, "Forest").shouldBeFalse()
    }

    test("a creature that is a Forest only through Ashaya taps for {G}") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.putCreatureOnBattlefield(p1, "Ashaya, Soul of the Wild")
        val bears = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        driver.offersGreenFrom(p1, bears).shouldBeTrue()
        driver.submitSuccess(ActivateAbility(p1, bears, AbilityId.intrinsicMana('G')))

        driver.greenInPool(p1) shouldBe 1
        driver.isTapped(bears).shouldBeTrue()
    }

    test("summoning sickness still stops a creature-land from tapping for mana") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val ashaya = driver.putCreatureOnBattlefield(p1, "Ashaya, Soul of the Wild")
        val bears = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(ashaya)

        driver.offersGreenFrom(p1, ashaya).shouldBeTrue()
        driver.offersGreenFrom(p1, bears).shouldBeFalse()
        driver.submitExpectFailure(ActivateAbility(p1, bears, AbilityId.intrinsicMana('G')))
    }

    test("auto-pay taps the creature-Forests to cast a spell") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val ashaya = driver.putCreatureOnBattlefield(p1, "Ashaya, Soul of the Wild")
        val bears = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(ashaya)
        driver.removeSummoningSickness(bears)

        val secondBears = driver.putCardInHand(p1, "Grizzly Bears")
        driver.castSpell(p1, secondBears).error shouldBe null

        driver.getStackSpellNames() shouldContain "Grizzly Bears"
        driver.isTapped(ashaya).shouldBeTrue()
        driver.isTapped(bears).shouldBeTrue()
    }

    test("auto-pay won't tap a summoning-sick creature-Forest") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val ashaya = driver.putCreatureOnBattlefield(p1, "Ashaya, Soul of the Wild")
        val sickBears = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(ashaya)

        // {1}{G} needs two sources; only Ashaya can tap.
        val secondBears = driver.putCardInHand(p1, "Grizzly Bears")
        driver.castSpell(p1, secondBears).error shouldNotBe null
        driver.isTapped(sickBears).shouldBeFalse()
    }

    test("once Ashaya leaves, the creatures are no longer lands and lose the mana ability") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val ashaya = driver.putCreatureOnBattlefield(p1, "Ashaya, Soul of the Wild")
        val bears = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(bears)
        driver.offersGreenFrom(p1, bears).shouldBeTrue()

        driver.moveToGraveyard(ashaya)

        driver.state.projectedState.hasType(bears, "LAND").shouldBeFalse()
        driver.offersGreenFrom(p1, bears).shouldBeFalse()
    }
})
