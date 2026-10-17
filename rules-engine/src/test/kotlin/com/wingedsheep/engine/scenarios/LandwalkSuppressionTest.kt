package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SuppressLandwalkForGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * [SuppressLandwalkForGroup] — "Creatures with forestwalk can be blocked as though they didn't have
 * forestwalk" (Deadfall, Lord Magnus). Landwalk (CR 702.14) makes a creature unblockable while the
 * defending player controls a land of the named type; the suppression ignores exactly that
 * evasion for the covered creatures, whoever controls the suppressing permanent, and nothing else.
 */
class LandwalkSuppressionTest : FunSpec({

    val forestwalkSuppressor = card("Test Deadfall") {
        manaCost = "{2}{G}"
        typeLine = "Enchantment"
        staticAbility { ability = SuppressLandwalkForGroup(Keyword.FORESTWALK) }
    }

    val islandwalkSuppressor = card("Test Undertow") {
        manaCost = "{2}{U}"
        typeLine = "Enchantment"
        staticAbility { ability = SuppressLandwalkForGroup(Keyword.ISLANDWALK) }
    }

    // Only its controller's forestwalkers lose the evasion.
    val ownForestwalkSuppressor = card("Test Own Deadfall") {
        manaCost = "{2}{G}"
        typeLine = "Enchantment"
        staticAbility {
            ability = SuppressLandwalkForGroup(Keyword.FORESTWALK, GroupFilter(GameObjectFilter.Creature.youControl()))
        }
    }

    val doubleWalker = card("Test Forest Island Walker") {
        manaCost = "{1}{G}{U}"
        typeLine = "Creature — Elemental"
        power = 2
        toughness = 2
        keywords(Keyword.FORESTWALK, Keyword.ISLANDWALK)
    }

    val nonbasicWalker = card("Test Nonbasic Walker") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Shade"
        power = 2
        toughness = 2
        keywords(Keyword.NONBASIC_LANDWALK)
    }

    val nonbasicLand = card("Test Nonbasic Land") {
        typeLine = "Land"
    }

    // One sentence naming two landwalks (the Staff of the Ages shape).
    val bothSuppressor = card("Test Forest Island Wall") {
        manaCost = "{3}"
        typeLine = "Artifact"
        staticAbility { ability = SuppressLandwalkForGroup(setOf(Keyword.FORESTWALK, Keyword.ISLANDWALK)) }
    }

    val nonbasicSuppressor = card("Test Nonbasic Shield") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility { ability = SuppressLandwalkForGroup(Keyword.NONBASIC_LANDWALK) }
    }

    val mute = card("Test Mute") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(t) }
    }

    val forestPath = card("Test Forest Path") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.GrantStaticAbility(SuppressLandwalkForGroup(Keyword.FORESTWALK, GroupFilter.source()), t)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(forestwalkSuppressor)
        driver.registerCard(islandwalkSuppressor)
        driver.registerCard(ownForestwalkSuppressor)
        driver.registerCard(doubleWalker)
        driver.registerCard(mute)
        listOf(nonbasicWalker, nonbasicLand, bothSuppressor, nonbasicSuppressor).forEach(driver::registerCard)
        driver.registerCard(forestPath)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Island" to 10, "Grizzly Bears" to 10),
            skipMulligans = true
        )
        return driver
    }

    fun GameTestDriver.toPlayer1DeclareAttackers() {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.DECLARE_ATTACKERS)
            safety++
        }
    }

    /** Attacks with [attackerName] into a Grizzly Bears behind [defenderLands]; returns the block attempt. */
    fun GameTestDriver.attackAndBlock(
        attackerName: String,
        defenderLands: List<String>,
        setup: GameTestDriver.() -> Unit = {}
    ): Pair<EntityId, com.wingedsheep.engine.core.ExecutionResult> {
        val attacker = putCreatureOnBattlefield(player1, attackerName)
        removeSummoningSickness(attacker)
        val blocker = putCreatureOnBattlefield(player2, "Grizzly Bears")
        removeSummoningSickness(blocker)
        defenderLands.forEach { putLandOnBattlefield(player2, it) }
        setup()
        toPlayer1DeclareAttackers()
        declareAttackers(player1, listOf(attacker), player2).outcome shouldBe Outcome.Done
        bothPass()
        currentStep shouldBe Step.DECLARE_BLOCKERS
        return attacker to submit(DeclareBlockers(player2, mapOf(blocker to listOf(attacker))))
    }

    test("a forestwalker can be blocked by a player who controls a Forest") {
        val driver = createDriver()
        val (_, result) = driver.attackAndBlock("Forest Walker", listOf("Forest")) {
            putPermanentOnBattlefield(player2, "Test Deadfall")
        }
        result.error shouldBe null
    }

    test("it applies whoever controls the suppressor — the attacker's own Deadfall works too") {
        val driver = createDriver()
        val (_, result) = driver.attackAndBlock("Forest Walker", listOf("Forest")) {
            putPermanentOnBattlefield(player1, "Test Deadfall")
        }
        result.error shouldBe null
    }

    test("suppressing a different landwalk leaves forestwalk intact") {
        val driver = createDriver()
        val (_, result) = driver.attackAndBlock("Forest Walker", listOf("Forest")) {
            putPermanentOnBattlefield(player2, "Test Undertow")
        }
        result.outcome shouldNotBe Outcome.Done
        result.error shouldContainIgnoringCase "forestwalk"
    }

    test("a creature with two landwalks stays unblockable through the one that isn't suppressed") {
        val driver = createDriver()
        val (_, result) = driver.attackAndBlock("Test Forest Island Walker", listOf("Forest", "Island")) {
            putPermanentOnBattlefield(player2, "Test Deadfall")
        }
        result.outcome shouldNotBe Outcome.Done
        result.error shouldContainIgnoringCase "islandwalk"
    }

    test("with both of its landwalks suppressed, it can be blocked") {
        val driver = createDriver()
        val (_, result) = driver.attackAndBlock("Test Forest Island Walker", listOf("Forest", "Island")) {
            putPermanentOnBattlefield(player2, "Test Deadfall")
            putPermanentOnBattlefield(player2, "Test Undertow")
        }
        result.error shouldBe null
    }

    test("one instance can name several landwalks") {
        val driver = createDriver()
        val (_, result) = driver.attackAndBlock("Test Forest Island Walker", listOf("Forest", "Island")) {
            putPermanentOnBattlefield(player2, "Test Forest Island Wall")
        }
        result.error shouldBe null
    }

    test("nonbasic landwalk is suppressed the same way") {
        val driver = createDriver()
        val (_, unsuppressed) = driver.attackAndBlock("Test Nonbasic Walker", listOf("Test Nonbasic Land"))
        unsuppressed.error shouldContainIgnoringCase "nonbasic landwalk"

        val other = createDriver()
        val (_, result) = other.attackAndBlock("Test Nonbasic Walker", listOf("Test Nonbasic Land")) {
            putPermanentOnBattlefield(player2, "Test Nonbasic Shield")
        }
        result.error shouldBe null
    }

    test("the creature keeps the keyword — only blocking is affected") {
        val driver = createDriver()
        val walker = driver.putCreatureOnBattlefield(driver.player1, "Forest Walker")
        driver.putPermanentOnBattlefield(driver.player2, "Test Deadfall")
        driver.state.projectedState.hasKeyword(walker, Keyword.FORESTWALK) shouldBe true
    }

    test("the group filter scopes it: an opponent's 'creatures you control' suppressor doesn't reach my walker") {
        val driver = createDriver()
        val (_, result) = driver.attackAndBlock("Forest Walker", listOf("Forest")) {
            putPermanentOnBattlefield(player2, "Test Own Deadfall")
        }
        result.outcome shouldNotBe Outcome.Done
    }

    /** Puts the board up, casts [spellName] at [spellTarget] in player 1's main phase, then attacks and blocks. */
    fun GameTestDriver.castThenAttack(
        spellName: String,
        spellTarget: GameTestDriver.(attacker: EntityId) -> EntityId,
        setup: GameTestDriver.() -> Unit = {}
    ): com.wingedsheep.engine.core.ExecutionResult {
        val attacker = putCreatureOnBattlefield(player1, "Forest Walker")
        removeSummoningSickness(attacker)
        val blocker = putCreatureOnBattlefield(player2, "Grizzly Bears")
        removeSummoningSickness(blocker)
        putLandOnBattlefield(player2, "Forest")
        setup()
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        while (activePlayer != player1) { bothPass(); passPriorityUntil(Step.PRECOMBAT_MAIN) }
        val spell = putCardInHand(player1, spellName)
        castSpell(player1, spell, listOf(spellTarget(attacker))).error shouldBe null
        bothPass()
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(attacker), player2).outcome shouldBe Outcome.Done
        bothPass()
        return submit(DeclareBlockers(player2, mapOf(blocker to listOf(attacker))))
    }

    test("the suppressor having lost all its abilities restores the evasion") {
        val driver = createDriver()
        var deadfall: EntityId? = null
        val result = driver.castThenAttack("Test Mute", { deadfall!! }) {
            deadfall = putPermanentOnBattlefield(player2, "Test Deadfall")
        }
        result.outcome shouldNotBe Outcome.Done
    }

    test("a granted until-end-of-turn suppression on the attacker makes it blockable") {
        val driver = createDriver()
        val result = driver.castThenAttack("Test Forest Path", { it })
        result.error shouldBe null
    }

    test("only landwalk keywords are accepted") {
        shouldThrow<IllegalArgumentException> { SuppressLandwalkForGroup(Keyword.FLYING) }
        shouldThrow<IllegalArgumentException> { SuppressLandwalkForGroup(emptySet()) }
    }
})
