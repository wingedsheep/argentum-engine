package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.WallCrawl
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val projector = StateProjector()

/**
 * Wall Crawl (SPM #121) — {3}{G} Enchantment.
 *
 *   When this enchantment enters, create a 2/1 green Spider creature token with reach, then you
 *   gain 1 life for each Spider you control.
 *   Spiders you control get +1/+1 and can't be blocked by creatures with defender.
 *
 * Covers: (1) the ETB — creates the 2/1 green Spider with reach, then gains 1 life per Spider you
 * control counting the just-created token; (2) the +1/+1 anthem over Spiders you control (and only
 * yours); (3) the "can't be blocked by creatures with defender" evasion granted to your Spiders.
 */
class WallCrawlScenarioTest : FunSpec({

    // A plain Spider you can pre-place to prove the anthem/evasion and to be counted for life gain.
    val testSpider = CardDefinition.creature(
        name = "Test Spider",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Spider")),
        power = 1,
        toughness = 1
    )

    // A defender creature the opponent will try (and fail) to block with.
    val testWall = CardDefinition.creature(
        name = "Test Wall",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Wall")),
        power = 0,
        toughness = 4,
        keywords = setOf(Keyword.DEFENDER)
    )

    // A non-Spider, non-defender creature: unaffected by the anthem, and a legal blocker.
    val testBear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WallCrawl, testSpider, testWall, testBear))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, skipMulligans = true)
        return driver
    }

    fun GameTestDriver.advanceToPlayer1DeclareAttackers() {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.DECLARE_ATTACKERS)
            safety++
        }
    }

    test("ETB creates a 2/1 green Spider with reach, then gains 1 life per Spider you control") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // One Spider already in play, so the life gain must count TWO (existing + new token).
        driver.putCreatureOnBattlefield(you, "Test Spider")
        val lifeBefore = driver.getLifeTotal(you)

        val wallCrawl = driver.putCardInHand(you, "Wall Crawl")
        driver.giveMana(you, Color.GREEN, 1)
        driver.giveColorlessMana(you, 3)
        driver.castSpell(you, wallCrawl)
        driver.bothPass() // resolve the enchantment -> ETB trigger onto the stack
        driver.bothPass() // resolve the ETB trigger

        val projected = projector.project(driver.state)

        // The token: a Spider you control that is not the pre-placed Test Spider.
        val spiders = driver.getCreatures(you).filter { projected.hasSubtype(it, "Spider") }
        spiders.size shouldBe 2
        val token = spiders.first { driver.getCardName(it) != "Test Spider" }
        projected.hasKeyword(token, Keyword.REACH) shouldBe true

        // "1 life for each Spider you control" — evaluated after the token exists, so it counts 2.
        driver.getLifeTotal(you) shouldBe lifeBefore + 2
    }

    test("Spiders you control get +1/+1; opponents' Spiders and your non-Spiders are unaffected") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(you, "Wall Crawl")
        val mySpider = driver.putCreatureOnBattlefield(you, "Test Spider")
        val myBear = driver.putCreatureOnBattlefield(you, "Test Bear")
        val enemySpider = driver.putCreatureOnBattlefield(opponent, "Test Spider")

        val projected = projector.project(driver.state)

        // Your Spider is buffed 1/1 -> 2/2.
        projected.getPower(mySpider) shouldBe 2
        projected.getToughness(mySpider) shouldBe 2
        // A non-Spider you control is untouched.
        projected.getPower(myBear) shouldBe 2
        projected.getToughness(myBear) shouldBe 2
        // The opponent's Spider is untouched ("Spiders YOU control").
        projected.getPower(enemySpider) shouldBe 1
        projected.getToughness(enemySpider) shouldBe 1
    }

    test("your Spider can't be blocked by a creature with defender, but can be blocked normally") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2

        driver.putPermanentOnBattlefield(me, "Wall Crawl")
        val spider = driver.putCreatureOnBattlefield(me, "Test Spider")
        driver.removeSummoningSickness(spider)
        val wall = driver.putCreatureOnBattlefield(opponent, "Test Wall")
        driver.removeSummoningSickness(wall)
        val bear = driver.putCreatureOnBattlefield(opponent, "Test Bear")
        driver.removeSummoningSickness(bear)

        driver.advanceToPlayer1DeclareAttackers()
        driver.activePlayer shouldBe me

        driver.declareAttackers(me, listOf(spider), opponent).isSuccess shouldBe true
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // A creature with defender can't block the Spider (Wall Crawl's evasion).
        val defenderBlock = driver.submitExpectFailure(
            DeclareBlockers(opponent, mapOf(wall to listOf(spider)))
        )
        defenderBlock.isSuccess shouldBe false

        // A creature without defender blocks the Spider just fine.
        val bearBlock = driver.declareBlockers(opponent, mapOf(bear to listOf(spider)))
        bearBlock.isSuccess shouldBe true
    }

    test("without Wall Crawl, a defender creature CAN block your Spider") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2

        // No Wall Crawl on the battlefield, so the evasion is absent.
        val spider = driver.putCreatureOnBattlefield(me, "Test Spider")
        driver.removeSummoningSickness(spider)
        val wall = driver.putCreatureOnBattlefield(opponent, "Test Wall")
        driver.removeSummoningSickness(wall)

        driver.advanceToPlayer1DeclareAttackers()
        driver.declareAttackers(me, listOf(spider), opponent).isSuccess shouldBe true
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        driver.declareBlockers(opponent, mapOf(wall to listOf(spider))).isSuccess shouldBe true
    }
})
