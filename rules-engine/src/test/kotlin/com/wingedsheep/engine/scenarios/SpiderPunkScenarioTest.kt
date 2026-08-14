package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.HealingSalve
import com.wingedsheep.mtg.sets.definitions.spm.cards.SpiderPunk
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spider-Punk (SPM) — Riot (printed + granted to other Spiders, one choice per grant per CR
 * 702.136b), "Spells and abilities can't be countered," and "Damage can't be prevented."
 */
class SpiderPunkScenarioTest : FunSpec({

    // A plain Spider creature spell, to exercise granted Riot.
    val testSpider = card("Test Web-Spinner") {
        manaCost = "{2}{G}"
        colorIdentity = "G"
        typeLine = "Creature — Spider"
        power = 1
        toughness = 1
    }

    // A second, non-legendary lord that also grants riot to Spiders — so a Spider can carry two
    // granted riot instances (Spider-Punk is legendary; Spider-Verse / Impostor Syndrome can also
    // put a second Spider-Punk in play).
    val testRiotLord = card("Test Riot Lord") {
        manaCost = "{2}{R}"
        colorIdentity = "R"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        staticAbility {
            ability = GrantKeyword(
                Keyword.RIOT,
                GroupFilter(GameObjectFilter.Creature.withSubtype("Spider").youControl(), excludeSelf = true),
            )
        }
    }

    // A creature with an ETB "gain 3 life" trigger, to exercise "abilities can't be countered".
    val lifeGainCreature = card("Test Lifegainer") {
        manaCost = "{1}{G}"
        colorIdentity = "G"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Effects.GainLife(3)
        }
    }

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(SpiderPunk, testSpider, testRiotLord, lifeGainCreature, HealingSalve)
        )
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        return Triple(driver, you, opponent)
    }

    fun settle(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun plusOne(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Choose the Riot mode whose label contains [needle] on the pending decision. */
    fun chooseRiotMode(driver: GameTestDriver, you: EntityId, needle: String) {
        val pick = driver.pendingDecision as ChooseOptionDecision
        val idx = pick.options.indexOfFirst { it.contains(needle, ignoreCase = true) }
        driver.submitDecision(you, OptionChosenResponse(pick.id, idx))
        settle(driver)
    }

    test("printed Riot: choosing the counter gives Spider-Punk a +1/+1 counter") {
        val (driver, you, _) = newGame()
        driver.giveMana(you, Color.RED, 1)
        driver.giveColorlessMana(you, 1)
        val sp = driver.putCardInHand(you, "Spider-Punk")
        driver.castSpell(you, sp)
        settle(driver)
        chooseRiotMode(driver, you, "counter")

        plusOne(driver, driver.findPermanent(you, "Spider-Punk")!!) shouldBe 1
    }

    test("printed Riot: choosing haste grants Spider-Punk haste") {
        val (driver, you, _) = newGame()
        driver.giveMana(you, Color.RED, 1)
        driver.giveColorlessMana(you, 1)
        val sp = driver.putCardInHand(you, "Spider-Punk")
        driver.castSpell(you, sp)
        settle(driver)
        chooseRiotMode(driver, you, "haste")

        val onBf = driver.findPermanent(you, "Spider-Punk")!!
        driver.state.projectedState.hasKeyword(onBf, Keyword.HASTE) shouldBe true
    }

    test("granted Riot: a Spider you cast while Spider-Punk is in play gets the enters-with choice") {
        val (driver, you, _) = newGame()
        driver.putCreatureOnBattlefield(you, "Spider-Punk") // the granter

        driver.giveMana(you, Color.GREEN, 1)
        driver.giveColorlessMana(you, 2)
        val spider = driver.putCardInHand(you, "Test Web-Spinner")
        driver.castSpell(you, spider)
        settle(driver) // pauses on the SYNTHESIZED riot choice
        chooseRiotMode(driver, you, "counter")

        plusOne(driver, driver.findPermanent(you, "Test Web-Spinner")!!) shouldBe 1
    }

    test("granted Riot: two riot granters give a Spider two separate choices (CR 702.136b)") {
        val (driver, you, _) = newGame()
        driver.putCreatureOnBattlefield(you, "Spider-Punk")   // grants riot to other Spiders
        driver.putCreatureOnBattlefield(you, "Test Riot Lord") // also grants riot to Spiders

        driver.giveMana(you, Color.GREEN, 1)
        driver.giveColorlessMana(you, 2)
        val spider = driver.putCardInHand(you, "Test Web-Spinner")
        driver.castSpell(you, spider)
        settle(driver)
        chooseRiotMode(driver, you, "counter") // first instance
        chooseRiotMode(driver, you, "counter") // second instance re-pauses separately

        // Each instance works separately, so the counter branch fires twice → two +1/+1 counters.
        plusOne(driver, driver.findPermanent(you, "Test Web-Spinner")!!) shouldBe 2
    }

    test("spells can't be countered: opponent's Counterspell can't counter your spell") {
        val (driver, you, opp) = newGame()
        driver.putCreatureOnBattlefield(you, "Spider-Punk")

        driver.giveMana(you, Color.GREEN, 2)
        val bears = driver.putCardInHand(you, "Grizzly Bears")
        driver.castSpell(you, bears)
        val bearsOnStack = driver.getTopOfStack()!!
        driver.passPriority(you)

        driver.giveMana(opp, Color.BLUE, 2)
        val counter = driver.putCardInHand(opp, "Counterspell")
        driver.submit(
            CastSpell(
                playerId = opp,
                cardId = counter,
                targets = listOf(ChosenTarget.Spell(bearsOnStack)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        settle(driver) // Counterspell resolves (fizzles), then Grizzly Bears resolves.

        // Counterspell resolves but can't counter — Grizzly Bears enters the battlefield.
        driver.findPermanent(you, "Grizzly Bears") shouldNotBe null
    }

    test("abilities can't be countered: Stifle can't counter your ETB trigger") {
        val (driver, you, opp) = newGame()
        driver.putCreatureOnBattlefield(you, "Spider-Punk")

        driver.giveMana(you, Color.GREEN, 2)
        val creature = driver.putCardInHand(you, "Test Lifegainer")
        driver.castSpell(you, creature)
        driver.bothPass() // creature resolves; its ETB "gain 3 life" trigger goes on the stack

        val trigger = driver.getTopOfStack()!!
        driver.passPriority(you)
        driver.giveMana(opp, Color.BLUE, 1)
        val stifle = driver.putCardInHand(opp, "Stifle")
        driver.castSpellWithTargets(opp, stifle, listOf(ChosenTarget.Spell(trigger)))
        settle(driver) // Stifle resolves (fizzles), then the ETB trigger resolves.

        // Stifle resolves but can't counter the ability — the trigger resolves and you gain 3 life.
        driver.getLifeTotal(you) shouldBe 23
    }

    test("damage can't be prevented: a prevention shield doesn't stop your damage") {
        val (driver, you, opp) = newGame()
        driver.putCreatureOnBattlefield(you, "Spider-Punk")

        // Shield the opponent from the next 3 damage this turn (Healing Salve, mode 1).
        driver.giveMana(you, Color.WHITE, 1)
        val salve = driver.putCardInHand(you, "Healing Salve")
        driver.submit(
            CastSpell(
                playerId = you,
                cardId = salve,
                targets = listOf(ChosenTarget.Player(opp)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(opp))),
            )
        )
        driver.bothPass()

        // Lightning Bolt the opponent — with Spider-Punk out the shield can't prevent it.
        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opp)))
        driver.bothPass()

        driver.getLifeTotal(opp) shouldBe 17
    }
})
