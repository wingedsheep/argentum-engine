package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Shatter
import com.wingedsheep.mtg.sets.definitions.one.cards.VatOfRebirth
import com.wingedsheep.mtg.sets.definitions.wth.cards.MindStone
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Vat of Rebirth (ONE #113) — {B} Artifact.
 *
 * "Whenever another artifact or creature you control is put into a graveyard from the battlefield,
 *  put an oil counter on this artifact.
 *  {2}{B}, {T}, Remove four oil counters from this artifact: Return target creature card from your
 *  graveyard to the battlefield. Activate only as a sorcery."
 */
class VatOfRebirthScenarioTest : FunSpec({

    val reanimate = VatOfRebirth.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(VatOfRebirth, MindStone, Shatter))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun giveOil(driver: GameTestDriver, id: EntityId, amount: Int) {
        driver.replaceState(driver.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.OIL, amount))
        })
    }

    fun bolt(driver: GameTestDriver, target: EntityId) {
        val bolt = driver.putCardInHand(driver.player1, "Lightning Bolt")
        driver.giveMana(driver.player1, Color.RED, 1)
        driver.castSpellWithTargets(driver.player1, bolt, listOf(ChosenTarget.Permanent(target))).error shouldBe null
        driver.bothPass()
        while (driver.pendingDecision != null) driver.autoResolveDecision()
        if (driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    fun shatter(driver: GameTestDriver, target: EntityId) {
        val spell = driver.putCardInHand(driver.player1, "Shatter")
        driver.giveMana(driver.player1, Color.RED, 2)
        driver.castSpellWithTargets(driver.player1, spell, listOf(ChosenTarget.Permanent(target))).error shouldBe null
        driver.bothPass()
        while (driver.pendingDecision != null) driver.autoResolveDecision()
        if (driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    test("another creature you control dying puts an oil counter on it; an opponent's does not") {
        val driver = newDriver()
        val vat = driver.putPermanentOnBattlefield(driver.player1, "Vat of Rebirth")
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Savannah Lions")

        bolt(driver, mine)
        driver.assertInGraveyard(driver.player1, "Centaur Courser")
        oil(driver, vat) shouldBe 1

        bolt(driver, theirs)
        driver.assertInGraveyard(driver.player2, "Savannah Lions")
        oil(driver, vat) shouldBe 1
    }

    test("removing four oil returns a creature card from your graveyard to the battlefield") {
        val driver = newDriver()
        val p1 = driver.player1
        val vat = driver.putPermanentOnBattlefield(p1, "Vat of Rebirth")
        giveOil(driver, vat, 5)
        val dead = driver.putCardInGraveyard(p1, "Centaur Courser")

        driver.giveMana(p1, Color.BLACK, 3)
        driver.submitSuccess(
            ActivateAbility(playerId = p1, sourceId = vat, abilityId = reanimate, targets = listOf(ChosenTarget.Card(dead, p1, Zone.GRAVEYARD)))
        )
        oil(driver, vat) shouldBe 1
        driver.bothPass()
        driver.findPermanent(p1, "Centaur Courser") shouldNotBe null
    }

    test("can't be activated with fewer than four oil counters") {
        val driver = newDriver()
        val p1 = driver.player1
        val vat = driver.putPermanentOnBattlefield(p1, "Vat of Rebirth")
        giveOil(driver, vat, 3)
        val dead = driver.putCardInGraveyard(p1, "Centaur Courser")

        driver.giveMana(p1, Color.BLACK, 3)
        driver.submitExpectFailure(
            ActivateAbility(playerId = p1, sourceId = vat, abilityId = reanimate, targets = listOf(ChosenTarget.Card(dead, p1, Zone.GRAVEYARD)))
        )
    }

    test("activate only as a sorcery") {
        val driver = newDriver()
        val p1 = driver.player1
        val vat = driver.putPermanentOnBattlefield(p1, "Vat of Rebirth")
        giveOil(driver, vat, 4)
        val dead = driver.putCardInGraveyard(p1, "Centaur Courser")
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.currentStep shouldBe Step.BEGIN_COMBAT

        driver.giveMana(p1, Color.BLACK, 3)
        driver.submitExpectFailure(
            ActivateAbility(playerId = p1, sourceId = vat, abilityId = reanimate, targets = listOf(ChosenTarget.Card(dead, p1, Zone.GRAVEYARD)))
        )
    }

    test("a noncreature artifact you control going to the graveyard also adds an oil counter") {
        val driver = newDriver()
        val vat = driver.putPermanentOnBattlefield(driver.player1, "Vat of Rebirth")
        val stone = driver.putPermanentOnBattlefield(driver.player1, "Mind Stone")

        shatter(driver, stone)
        driver.assertInGraveyard(driver.player1, "Mind Stone")
        oil(driver, vat) shouldBe 1
    }
})
