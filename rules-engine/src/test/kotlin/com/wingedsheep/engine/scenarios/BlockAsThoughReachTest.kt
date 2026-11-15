package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.combat.rules.BlockAsThoughReach
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CanBlockAsThoughHadReach
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * [CanBlockAsThoughHadReach] — "This creature can block Dragons as though it had reach" (Dragon
 * Hunter). A creature with flying can't be blocked except by creatures with flying and/or reach
 * (CR 702.9b / 702.17b); the static answers that reach question for the attackers its filter names
 * and nothing else — the blocker doesn't gain reach, and other flyers still evade it.
 */
class BlockAsThoughReachTest : FunSpec({

    val dragonHunter = card("Test Dragon Hunter") {
        manaCost = "{W}"
        typeLine = "Creature — Human Warrior"
        power = 2
        toughness = 1
        staticAbility { ability = CanBlockAsThoughHadReach(GameObjectFilter.Creature.withSubtype("Dragon")) }
    }

    // Battlefield scope: "Creatures you control can block Dragons as though they had reach."
    val dragonNets = card("Test Dragon Nets") {
        manaCost = "{1}{G}"
        typeLine = "Enchantment"
        staticAbility {
            ability = CanBlockAsThoughHadReach(
                GameObjectFilter.Creature.withSubtype("Dragon"),
                GroupFilter(GameObjectFilter.Creature.youControl())
            )
        }
    }

    val flyingDragon = card("Test Flying Dragon") {
        manaCost = "{3}{R}"
        typeLine = "Creature — Dragon"
        power = 3
        toughness = 3
        keywords(Keyword.FLYING)
    }

    val flyingBird = card("Test Flying Bird") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Bird"
        power = 2
        toughness = 2
        keywords(Keyword.FLYING)
    }

    // "Can't be blocked except by creatures with flying" — reach answers it (CR 702.17b's ruling
    // shape), so blocking it "as though it had reach" does too.
    val skyDragon = card("Test Sky-Only Dragon") {
        manaCost = "{2}{U}"
        typeLine = "Creature — Dragon"
        power = 2
        toughness = 2
        staticAbility { ability = CantBeBlockedExceptBy(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)) }
    }

    val mute = card("Test Mute") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(t) }
    }

    val dragonSight = card("Test Dragon Sight") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.GrantStaticAbility(
                CanBlockAsThoughHadReach(GameObjectFilter.Creature.withSubtype("Dragon")), t
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        listOf(dragonHunter, dragonNets, flyingDragon, flyingBird, skyDragon, mute, dragonSight)
            .forEach(driver::registerCard)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Island" to 10, "Grizzly Bears" to 10),
            skipMulligans = true
        )
        return driver
    }

    fun GameTestDriver.toPlayer1(step: Step) {
        passPriorityUntil(step)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(step)
            safety++
        }
    }

    /**
     * Player 1 attacks with [attackerName]; player 2 blocks with [blockerName]. [beforeCombat] runs
     * in player 1's main phase with both creatures in play.
     */
    fun GameTestDriver.attackAndBlock(
        attackerName: String,
        blockerName: String,
        setup: GameTestDriver.() -> Unit = {},
        beforeCombat: GameTestDriver.(attacker: EntityId, blocker: EntityId) -> Unit = { _, _ -> }
    ): ExecutionResult {
        val attacker = putCreatureOnBattlefield(player1, attackerName)
        removeSummoningSickness(attacker)
        val blocker = putCreatureOnBattlefield(player2, blockerName)
        removeSummoningSickness(blocker)
        setup()
        toPlayer1(Step.PRECOMBAT_MAIN)
        beforeCombat(attacker, blocker)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(attacker), player2).outcome shouldBe Outcome.Done
        bothPass()
        currentStep shouldBe Step.DECLARE_BLOCKERS
        return submit(DeclareBlockers(player2, mapOf(blocker to listOf(attacker))))
    }

    test("it can block a flying Dragon") {
        val driver = createDriver()
        driver.attackAndBlock("Test Flying Dragon", "Test Dragon Hunter").error shouldBe null
    }

    test("it still can't block a flyer that isn't a Dragon") {
        val driver = createDriver()
        val result = driver.attackAndBlock("Test Flying Bird", "Test Dragon Hunter")
        result.outcome shouldNotBe Outcome.Done
        result.error shouldContainIgnoringCase "flying"
    }

    test("it can block a Dragon that can't be blocked except by creatures with flying") {
        val driver = createDriver()
        driver.attackAndBlock("Test Sky-Only Dragon", "Test Dragon Hunter").error shouldBe null
    }

    test("it doesn't gain reach") {
        val driver = createDriver()
        val hunter = driver.putCreatureOnBattlefield(driver.player2, "Test Dragon Hunter")
        driver.state.projectedState.hasKeyword(hunter, Keyword.REACH) shouldBe false
    }

    test("a creature without the permission can't block the flying Dragon") {
        val driver = createDriver()
        val result = driver.attackAndBlock("Test Flying Dragon", "Grizzly Bears")
        result.outcome shouldNotBe Outcome.Done
        result.error shouldContainIgnoringCase "flying"
    }

    test("having lost all its abilities, it can't block the flying Dragon") {
        val driver = createDriver()
        val result = driver.attackAndBlock("Test Flying Dragon", "Test Dragon Hunter") { _, blocker ->
            val spell = putCardInHand(player1, "Test Mute")
            castSpell(player1, spell, listOf(blocker)).error shouldBe null
            bothPass()
        }
        result.outcome shouldNotBe Outcome.Done
    }

    test("a battlefield-scoped grant covers its controller's creatures") {
        val driver = createDriver()
        driver.attackAndBlock("Test Flying Dragon", "Grizzly Bears", setup = {
            putPermanentOnBattlefield(player2, "Test Dragon Nets")
        }).error shouldBe null
    }

    test("a battlefield-scoped grant doesn't cover the opponent's creatures") {
        val driver = createDriver()
        val result = driver.attackAndBlock("Test Flying Dragon", "Grizzly Bears", setup = {
            putPermanentOnBattlefield(player1, "Test Dragon Nets")
        })
        result.outcome shouldNotBe Outcome.Done
    }

    test("an until-end-of-turn grant lets the creature block the flying Dragon") {
        val driver = createDriver()
        driver.attackAndBlock("Test Flying Dragon", "Grizzly Bears") { _, blocker ->
            val spell = putCardInHand(player1, "Test Dragon Sight")
            castSpell(player1, spell, listOf(blocker)).error shouldBe null
            bothPass()
        }.error shouldBe null
    }

    test("the shared reader answers per attacker") {
        val driver = createDriver()
        val hunter = driver.putCreatureOnBattlefield(driver.player2, "Test Dragon Hunter")
        val dragon = driver.putCreatureOnBattlefield(driver.player1, "Test Flying Dragon")
        val bird = driver.putCreatureOnBattlefield(driver.player1, "Test Flying Bird")
        val evaluator = com.wingedsheep.engine.handlers.PredicateEvaluator(driver.cardRegistry)
        BlockAsThoughReach.hasReachAgainst(driver.state, hunter, dragon, driver.cardRegistry, evaluator) shouldBe true
        BlockAsThoughReach.hasReachAgainst(driver.state, hunter, bird, driver.cardRegistry, evaluator) shouldBe false
    }
})
