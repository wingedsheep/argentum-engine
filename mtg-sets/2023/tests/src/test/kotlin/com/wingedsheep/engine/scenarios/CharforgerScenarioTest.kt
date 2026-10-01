package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Shatter
import com.wingedsheep.mtg.sets.definitions.one.cards.Charforger
import com.wingedsheep.mtg.sets.definitions.wth.cards.MindStone
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Charforger (ONE #199) — {1}{B}{R} 2/3 Creature — Phyrexian Beast.
 *
 * "When this creature enters, create a 1/1 red Phyrexian Goblin creature token.
 *  Whenever another creature or artifact you control is put into a graveyard from the battlefield,
 *  put an oil counter on this creature.
 *  Remove three oil counters from this creature: Exile the top card of your library. You may play
 *  that card this turn."
 */
class CharforgerScenarioTest : FunSpec({

    val impulseAbilityId = Charforger.activatedAbilities.single().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Charforger, MindStone, Shatter))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun seedOil(driver: GameTestDriver, id: EntityId, amount: Int) {
        driver.replaceState(driver.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.OIL, amount))
        })
    }

    fun castCharforger(driver: GameTestDriver): EntityId {
        val p1 = driver.player1
        val card = driver.putCardInHand(p1, "Charforger")
        driver.giveMana(p1, Color.BLACK, 1)
        driver.giveMana(p1, Color.RED, 1)
        driver.giveColorlessMana(p1, 1)
        driver.castSpell(p1, card).outcome shouldBe Outcome.Done
        driver.bothPass()
        if (driver.state.stack.isNotEmpty()) driver.bothPass()
        val forger = driver.findPermanent(p1, "Charforger")
        forger shouldNotBe null
        return forger!!
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

    test("entering creates a 1/1 red Phyrexian Goblin token") {
        val driver = newDriver()
        castCharforger(driver)
        val goblin = driver.findPermanent(driver.player1, "Phyrexian Goblin Token")
            ?: driver.state.getBattlefield().firstOrNull { id ->
                id != driver.findPermanent(driver.player1, "Charforger") &&
                    driver.state.projectedState.hasSubtype(id, "Goblin")
            }
        goblin shouldNotBe null
        driver.state.projectedState.getPower(goblin!!) shouldBe 1
        driver.state.projectedState.getToughness(goblin) shouldBe 1
        driver.state.projectedState.hasSubtype(goblin, "Phyrexian") shouldBe true
        driver.state.projectedState.getColors(goblin).contains(Color.RED.name) shouldBe true
    }

    test("another creature you control dying adds an oil counter; an opponent's does not") {
        val driver = newDriver()
        val forger = castCharforger(driver)
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Savannah Lions")

        bolt(driver, mine)
        driver.assertInGraveyard(driver.player1, "Centaur Courser")
        oil(driver, forger) shouldBe 1

        bolt(driver, theirs)
        driver.assertInGraveyard(driver.player2, "Savannah Lions")
        oil(driver, forger) shouldBe 1
    }

    test("removing three oil counters exiles the top card and lets you play it this turn") {
        val driver = newDriver()
        val p1 = driver.player1
        val forger = castCharforger(driver)

        // Two counters is not enough to pay the cost.
        seedOil(driver, forger, 2)
        driver.submitExpectFailure(ActivateAbility(playerId = p1, sourceId = forger, abilityId = impulseAbilityId))

        seedOil(driver, forger, 1)
        driver.putCardOnTopOfLibrary(p1, "Mountain")
        driver.submitSuccess(ActivateAbility(playerId = p1, sourceId = forger, abilityId = impulseAbilityId))
        oil(driver, forger) shouldBe 0
        driver.bothPass()

        driver.getExileCardNames(p1) shouldBe listOf("Mountain")
        val exiled = driver.getExile(p1).single()
        driver.state.mayPlayPermissions.any { exiled in it.cardIds } shouldBe true
        driver.playLand(p1, exiled).outcome shouldBe Outcome.Done
        driver.getExile(p1).contains(exiled) shouldBe false
    }

    test("a noncreature artifact you control going to the graveyard also adds an oil counter") {
        val driver = newDriver()
        val forger = castCharforger(driver)
        val stone = driver.putPermanentOnBattlefield(driver.player1, "Mind Stone")

        shatter(driver, stone)
        driver.assertInGraveyard(driver.player1, "Mind Stone")
        oil(driver, forger) shouldBe 1
    }
})
