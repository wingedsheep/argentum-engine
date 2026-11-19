package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.tribute
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tribute N (CR 702.104):
 *  - 702.104a — as the creature enters, an opponent may put N additional +1/+1 counters on it; it
 *    *enters* with them (no stack, no response window).
 *  - 702.104b — "if tribute wasn't paid" is true when that opponent didn't have it enter with them.
 * Rulings: the opponent chosen to pay tribute is the one putting the counters on (2017-04-18), and
 * the "tribute wasn't paid" trigger resolves even if the creature has left the battlefield.
 */
class TributeTest : FunSpec({

    val tributeBeast = card("Test Tribute Beast") {
        manaCost = "{1}{G}"
        colorIdentity = "G"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
        tribute(2)
        triggeredAbility {
            trigger = Triggers.self.enters()
            interveningIf = Conditions.TributeNotPaid
            effect = Effects.GainLife(4)
        }
    }

    // "If you would put one or more +1/+1 counters on a creature you control, put that many plus one"
    // — keyed to the placer (Doc Samson's reading), so it must not see an opponent's tribute.
    val yourCounterBooster = card("Test Your Counter Booster") {
        manaCost = "{1}{G}"
        colorIdentity = "G"
        typeLine = "Enchantment"
        replacementEffect(ModifyCounterPlacement(modifier = 1, placer = Player.You))
    }

    val reanimate = card("Test Reanimate") {
        manaCost = "{B}"
        colorIdentity = "B"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.CreatureInYourGraveyard)
            effect = Effects.Move(t, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        }
    }

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(tributeBeast, yourCounterBooster, reanimate))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun plusOne(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Cast the beast and resolve it up to the tribute decision, which must be the opponent's. */
    fun castToTributeChoice(driver: GameTestDriver, you: EntityId): ChooseOptionDecision {
        driver.giveMana(you, Color.GREEN, 1)
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, driver.putCardInHand(you, "Test Tribute Beast"))
        driver.bothPass()
        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe driver.getOpponent(you)
        return decision
    }

    fun answer(driver: GameTestDriver, decision: ChooseOptionDecision, needle: String) {
        val index = decision.options.indexOfFirst { it.startsWith(needle, ignoreCase = true) }
        driver.submitDecision(decision.playerId, OptionChosenResponse(decision.id, index))
    }

    test("the opponent pays tribute: it enters with the counters and the not-paid trigger doesn't fire") {
        val (driver, you) = newGame()
        val decision = castToTributeChoice(driver, you)
        answer(driver, decision, "Pay tribute")

        val beast = driver.findPermanent(you, "Test Tribute Beast")!!
        plusOne(driver, beast) shouldBe 2
        driver.state.stack.size shouldBe 0
        driver.getLifeTotal(you) shouldBe 20
    }

    test("the opponent doesn't pay: no counters, and the not-paid trigger resolves") {
        val (driver, you) = newGame()
        val decision = castToTributeChoice(driver, you)
        answer(driver, decision, "Don't pay")

        val beast = driver.findPermanent(you, "Test Tribute Beast")!!
        plusOne(driver, beast) shouldBe 0
        driver.state.stack.size shouldBe 1
        driver.bothPass()
        driver.getLifeTotal(you) shouldBe 24
    }

    test("the not-paid trigger resolves even after the creature has left the battlefield") {
        val (driver, you) = newGame()
        val decision = castToTributeChoice(driver, you)
        answer(driver, decision, "Don't pay")

        val beast = driver.findPermanent(you, "Test Tribute Beast")!!
        driver.moveToGraveyard(beast)
        driver.bothPass()
        driver.getLifeTotal(you) shouldBe 24
    }

    test("tribute applies however the creature enters — returned from the graveyard, not cast") {
        val (driver, you) = newGame()
        val inYard = driver.putCardInGraveyard(you, "Test Tribute Beast")
        driver.giveMana(you, Color.BLACK, 1)
        driver.castSpellWithTargets(you, driver.putCardInHand(you, "Test Reanimate"), listOf(ChosenTarget.Card(inYard, you, Zone.GRAVEYARD)))
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe driver.getOpponent(you)
        answer(driver, decision, "Pay tribute")

        val beast = driver.findPermanent(you, "Test Tribute Beast")!!
        plusOne(driver, beast) shouldBe 2
    }

    test("the opponent puts the tribute counters on — not its controller") {
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Test Your Counter Booster")
        val decision = castToTributeChoice(driver, you)
        answer(driver, decision, "Pay tribute")

        val beast = driver.findPermanent(you, "Test Tribute Beast")!!
        // Your "if you would put counters" replacement doesn't apply: the opponent put them on.
        plusOne(driver, beast) shouldBe 2
        driver.events.filterIsInstance<CountersAddedEvent>().map { it.entityId to it.placedBy } shouldContain
            (beast to driver.getOpponent(you))
    }
})
