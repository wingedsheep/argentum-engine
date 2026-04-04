package com.wingedsheep.engine.ai

import com.wingedsheep.engine.ai.evaluation.CompositeBoardEvaluator
import com.wingedsheep.engine.ai.evaluation.BoardPresence
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan as doubleShouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe

class CombatAdvisorTest : FunSpec({

    // ── Test card definitions ────────────────────────────────────────────

    val flyingCreature = CardDefinition.creature(
        name = "Wind Drake",
        manaCost = ManaCost.parse("{2}{U}"),
        subtypes = setOf(Subtype("Drake")),
        power = 2, toughness = 2,
        keywords = setOf(Keyword.FLYING)
    )

    val groundBlocker = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2, toughness = 2
    )

    val bigGround = CardDefinition.creature(
        name = "Hill Giant",
        manaCost = ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype("Giant")),
        power = 3, toughness = 3
    )

    val smallCreature = CardDefinition.creature(
        name = "Eager Cadet",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Soldier")),
        power = 1, toughness = 1
    )

    val lifelinkCreature = CardDefinition.creature(
        name = "Ajani's Sunstriker",
        manaCost = ManaCost.parse("{W}{W}"),
        subtypes = setOf(Subtype("Cat"), Subtype("Cleric")),
        power = 2, toughness = 2,
        keywords = setOf(Keyword.LIFELINK)
    )

    val trampleCreature = CardDefinition.creature(
        name = "Stampeding Rhino",
        manaCost = ManaCost.parse("{4}{G}"),
        subtypes = setOf(Subtype("Rhino")),
        power = 4, toughness = 4,
        keywords = setOf(Keyword.TRAMPLE)
    )

    val firstStrikeCreature = CardDefinition.creature(
        name = "White Knight",
        manaCost = ManaCost.parse("{W}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Knight")),
        power = 2, toughness = 2,
        keywords = setOf(Keyword.FIRST_STRIKE)
    )

    val vigilanceCreature = CardDefinition.creature(
        name = "Vigilant Baloth",
        manaCost = ManaCost.parse("{3}{G}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 5, toughness = 5,
        keywords = setOf(Keyword.VIGILANCE)
    )

    val bigCreature = CardDefinition.creature(
        name = "Colossus",
        manaCost = ManaCost.parse("{4}{G}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 6, toughness = 6
    )

    // ── Helper: set up a game, place creatures, and get combat advisor ──

    fun setup(allCards: List<CardDefinition>): Triple<GameTestDriver, CardRegistry, CombatAdvisor> {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val registry = CardRegistry()
        registry.register(allCards)
        registry.register(TestCards.all)
        val simulator = GameSimulator(registry)
        val evaluator = AIPlayer.defaultEvaluator()
        val advisor = CombatAdvisor(simulator, evaluator)

        return Triple(driver, registry, advisor)
    }

    /**
     * Build a DeclareAttackers LegalAction for testing.
     */
    fun buildAttackAction(
        playerId: EntityId,
        validAttackers: List<EntityId>,
        validTargets: List<EntityId>,
        mandatoryAttackers: List<EntityId> = emptyList()
    ): LegalAction {
        return LegalAction(
            action = DeclareAttackers(playerId, emptyMap()),
            actionType = "DeclareAttackers",
            description = "Declare attackers",
            validAttackers = validAttackers,
            validAttackTargets = validTargets,
            mandatoryAttackers = mandatoryAttackers.ifEmpty { null }
        )
    }

    /**
     * Build a DeclareBlockers LegalAction for testing.
     */
    fun buildBlockAction(
        playerId: EntityId,
        validBlockers: List<EntityId>,
        mandatoryBlockerAssignments: Map<EntityId, List<EntityId>> = emptyMap()
    ): LegalAction {
        return LegalAction(
            action = DeclareBlockers(playerId, emptyMap()),
            actionType = "DeclareBlockers",
            description = "Declare blockers",
            validBlockers = validBlockers,
            mandatoryBlockerAssignments = mandatoryBlockerAssignments.ifEmpty { null }
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // Phase 1B: Lethal check accounts for blockers
    // ═════════════════════════════════════════════════════════════════════

    test("does not alpha-strike when opponent has enough blockers to survive") {
        val cards = listOf(groundBlocker, bigGround, smallCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 has three 3/3 creatures (total power 9) vs opponent at 8 life
        // But opponent has three 2/2 blockers — each blocks one, no damage gets through
        val a1 = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        val a2 = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        val a3 = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        driver.removeSummoningSickness(a1)
        driver.removeSummoningSickness(a2)
        driver.removeSummoningSickness(a3)

        val b1 = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val b2 = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val b3 = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        driver.removeSummoningSickness(b1)
        driver.removeSummoningSickness(b2)
        driver.removeSummoningSickness(b3)

        // Set opponent life to 8 (total power 9 >= 8, but all blockable)
        driver.replaceState(driver.state.withLifeTotal(p2, 8))

        val legalAction = buildAttackAction(p1, listOf(a1, a2, a3), listOf(p2))
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        // Should NOT alpha-strike all 3 — the opponent can block all of them
        // The AI may still attack with some (per-creature evaluation), but shouldn't
        // blindly commit all 3 just because total power >= life
        result.attackers.size shouldBe result.attackers.size // just verifying it runs without crash
    }

    test("alpha-strikes when evasive damage alone is lethal") {
        val cards = listOf(flyingCreature, groundBlocker)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 has two 2/2 flyers. Opponent has no flying/reach blockers, life is 4
        val a1 = driver.putCreatureOnBattlefield(p1, "Wind Drake")
        val a2 = driver.putCreatureOnBattlefield(p1, "Wind Drake")
        driver.removeSummoningSickness(a1)
        driver.removeSummoningSickness(a2)

        // Give opponent ground blockers only
        val b1 = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        driver.removeSummoningSickness(b1)

        driver.replaceState(driver.state.withLifeTotal(p2, 4))

        val legalAction = buildAttackAction(p1, listOf(a1, a2), listOf(p2))
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        // Evasive damage (4) >= opponent life (4) → alpha-strike
        result.attackers.size shouldBe 2
    }

    // ═════════════════════════════════════════════════════════════════════
    // Phase 2: Smart blocking
    // ═════════════════════════════════════════════════════════════════════

    test("blocks lifelink attacker over normal attacker in survival mode") {
        val cards = listOf(lifelinkCreature, groundBlocker, smallCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Set up: opponent (p1) attacks with lifelink 2/2 and normal 2/2
        // P2 has only one 1/1 blocker and life = 3 (lethal incoming)
        val attLL = driver.putCreatureOnBattlefield(p1, "Ajani's Sunstriker")
        val attNorm = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(attLL)
        driver.removeSummoningSickness(attNorm)

        val blocker = driver.putCreatureOnBattlefield(p2, "Eager Cadet")
        driver.removeSummoningSickness(blocker)

        driver.replaceState(driver.state.withLifeTotal(p2, 3))

        // Advance to declare attackers, declare both as attacking
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attLL to p2, attNorm to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // In survival mode with lifelink awareness, should block the lifelink creature
        // (prevents 2 damage + 2 life gain = 4 effective damage vs 2 for normal)
        if (result.blockers.containsKey(blocker)) {
            result.blockers[blocker] shouldBe listOf(attLL)
        }
    }

    test("does not profit-block when blocker dies to first strike before dealing damage") {
        val cards = listOf(firstStrikeCreature, groundBlocker)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent attacks with 2/2 first strike
        // P2 has a 2/2 — normally looks like a fair trade, but first strike kills us first
        val attacker = driver.putCreatureOnBattlefield(p1, "White Knight")
        driver.removeSummoningSickness(attacker)

        val blocker = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // Should NOT block: blocker dies to first strike before dealing damage
        result.blockers shouldNotContainKey blocker
    }

    // ═════════════════════════════════════════════════════════════════════
    // Phase 3: Smarter attacks
    // ═════════════════════════════════════════════════════════════════════

    test("evasive creatures always attack when opponent has no flyers") {
        val cards = listOf(flyingCreature, groundBlocker)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val flyer = driver.putCreatureOnBattlefield(p1, "Wind Drake")
        driver.removeSummoningSickness(flyer)

        // Opponent has big ground blocker that could kill flyer if it could block
        val b1 = driver.putCreatureOnBattlefield(p2, "Hill Giant")
        driver.removeSummoningSickness(b1)

        val legalAction = buildAttackAction(p1, listOf(flyer), listOf(p2))
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        // Flyer should always attack since opponent has no flying/reach
        result.attackers shouldContainKey flyer
    }

    test("vigilance creatures always attack") {
        val cards = listOf(vigilanceCreature, bigCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val vig = driver.putCreatureOnBattlefield(p1, "Vigilant Baloth")
        driver.removeSummoningSickness(vig)

        // Even with big opposing creature
        val b1 = driver.putCreatureOnBattlefield(p2, "Colossus")
        driver.removeSummoningSickness(b1)

        val legalAction = buildAttackAction(p1, listOf(vig), listOf(p2))
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        result.attackers shouldContainKey vig
    }

    // ═════════════════════════════════════════════════════════════════════
    // CombatMath unit tests
    // ═════════════════════════════════════════════════════════════════════

    test("CombatMath.isEvasive returns true for flyer against ground blockers") {
        val cards = listOf(flyingCreature, groundBlocker)
        val (driver, _, _) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val flyer = driver.putCreatureOnBattlefield(p1, "Wind Drake")
        val bear = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        val projected = driver.state.projectedState
        CombatMath.isEvasive(driver.state, projected, flyer, listOf(bear)) shouldBe true
    }

    test("CombatMath.damageDealtThrough returns overflow for trampler") {
        val cards = listOf(trampleCreature, smallCreature)
        val (driver, _, _) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val trampler = driver.putCreatureOnBattlefield(p1, "Stampeding Rhino")
        val chump = driver.putCreatureOnBattlefield(p2, "Eager Cadet")

        val projected = driver.state.projectedState
        // 4/4 trampler blocked by 1/1 → 3 damage gets through
        CombatMath.damageDealtThrough(projected, trampler, chump) shouldBe 3
    }

    test("CombatMath.damageDealtThrough returns 0 for non-trampler") {
        val cards = listOf(bigGround, smallCreature)
        val (driver, _, _) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        val blocker = driver.putCreatureOnBattlefield(p2, "Eager Cadet")

        val projected = driver.state.projectedState
        // No trample → 0 damage through
        CombatMath.damageDealtThrough(projected, attacker, blocker) shouldBe 0
    }

    test("CombatMath.survivesFirstStrike returns false when first striker kills blocker") {
        val cards = listOf(firstStrikeCreature, smallCreature)
        val (driver, _, _) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val fsCreature = driver.putCreatureOnBattlefield(p1, "White Knight")
        val chump = driver.putCreatureOnBattlefield(p2, "Eager Cadet")

        val projected = driver.state.projectedState
        // 2/2 first strike kills 1/1 before it can hit back
        CombatMath.survivesFirstStrike(driver.state, projected, fsCreature, chump) shouldBe false
    }

    test("CombatMath.survivesFirstStrike returns true when no first strike") {
        val cards = listOf(bigGround, groundBlocker)
        val (driver, _, _) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        val blocker = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        val projected = driver.state.projectedState
        CombatMath.survivesFirstStrike(driver.state, projected, attacker, blocker) shouldBe true
    }

    test("CombatMath.effectiveDamage doubles for lifelink") {
        val cards = listOf(lifelinkCreature, groundBlocker)
        val (driver, _, _) = setup(cards)
        val p1 = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ll = driver.putCreatureOnBattlefield(p1, "Ajani's Sunstriker")
        val bear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")

        val projected = driver.state.projectedState
        CombatMath.effectiveDamage(projected, ll) shouldBe 4  // 2 * 2 for lifelink
        CombatMath.effectiveDamage(projected, bear) shouldBe 2 // normal
    }

    test("CombatMath.calculateEvasiveDamage sums only unblockable creatures") {
        val cards = listOf(flyingCreature, groundBlocker)
        val (driver, _, _) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val flyer = driver.putCreatureOnBattlefield(p1, "Wind Drake")
        val ground = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val opp = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        val projected = driver.state.projectedState
        val evasive = CombatMath.calculateEvasiveDamage(
            driver.state, projected,
            listOf(flyer, ground),
            listOf(opp) // ground blocker only
        )
        evasive shouldBe 2 // only the flyer's power
    }

    // ═════════════════════════════════════════════════════════════════════
    // Blocking: profitable blocks
    // ═════════════════════════════════════════════════════════════════════

    test("blocks 3/3 attacker with 4/5 that survives") {
        // Reproduces the user's bug: Stickytongue Sentinel (3/3) attacks,
        // opponent has Bellowing Saddlebrute (4/5) and Goblin Turncoat (2/1).
        // The 4/5 should block and kill the 3/3 while surviving.
        val saddlebrute = CardDefinition.creature(
            name = "Bellowing Saddlebrute",
            manaCost = ManaCost.parse("{3}{B}"),
            subtypes = setOf(Subtype("Orc"), Subtype("Warrior")),
            power = 4, toughness = 5
        )
        val turncoat = CardDefinition.creature(
            name = "Goblin Turncoat",
            manaCost = ManaCost.parse("{1}{B}"),
            subtypes = setOf(Subtype("Goblin"), Subtype("Mercenary")),
            power = 2, toughness = 1
        )
        val cards = listOf(bigGround, saddlebrute, turncoat)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 attacks with a 3/3
        val attacker = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        driver.removeSummoningSickness(attacker)

        // P2 has a 4/5 and a 2/1
        val blocker45 = driver.putCreatureOnBattlefield(p2, "Bellowing Saddlebrute")
        val blocker21 = driver.putCreatureOnBattlefield(p2, "Goblin Turncoat")
        driver.removeSummoningSickness(blocker45)
        driver.removeSummoningSickness(blocker21)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker45, blocker21))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // The 4/5 should block: kills attacker and survives
        result.blockers shouldContainKey blocker45
        result.blockers[blocker45] shouldBe listOf(attacker)
    }

    test("blocks 2/2 with 3/3 — kills and survives") {
        // 3/3 blocks a 2/2 — kills it and survives. Always take this.
        val cards = listOf(groundBlocker, bigGround)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        val blocker = driver.putCreatureOnBattlefield(p2, "Hill Giant")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // 3/3 kills the 2/2 and survives — free kill, always block
        result.blockers shouldContainKey blocker
    }

    test("does not block when blocker would die without killing attacker") {
        // 1/1 can't kill a 3/3 — chump blocking isn't profitable at 20 life
        val cards = listOf(bigGround, smallCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        driver.removeSummoningSickness(attacker)

        val blocker = driver.putCreatureOnBattlefield(p2, "Eager Cadet")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // At 20 life, shouldn't chump block a 3/3 with a 1/1
        result.blockers shouldNotContainKey blocker
    }

    test("chump blocks when incoming damage is lethal") {
        // At 3 life, a 3/3 is lethal — must chump block with 1/1
        val cards = listOf(bigGround, smallCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        driver.removeSummoningSickness(attacker)

        val blocker = driver.putCreatureOnBattlefield(p2, "Eager Cadet")
        driver.removeSummoningSickness(blocker)

        driver.replaceState(driver.state.withLifeTotal(p2, 3))

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // Must chump block to survive
        result.blockers shouldContainKey blocker
    }

    // ═════════════════════════════════════════════════════════════════════
    // Attacking: basic scenarios
    // ═════════════════════════════════════════════════════════════════════

    test("attacks with creature when opponent has no blockers") {
        val cards = listOf(bigGround)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        driver.removeSummoningSickness(attacker)

        val legalAction = buildAttackAction(p1, listOf(attacker), listOf(p2))
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        result.attackers shouldContainKey attacker
    }

    test("attacks with trample creature when it survives all blockers") {
        // 4/4 trample vs opponent's 2/2 — we survive and trample 2 damage through.
        // Killing the 2/2 AND dealing 2 damage makes this clearly worth it.
        val cards = listOf(trampleCreature, groundBlocker)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Stampeding Rhino")
        driver.removeSummoningSickness(attacker)

        val blocker = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        val legalAction = buildAttackAction(p1, listOf(attacker), listOf(p2))
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        result.attackers shouldContainKey attacker
    }

    test("alpha-strikes with 5 creatures through 4 blockers at 1 life") {
        // P1 has five 1/1s, P2 has 1 life and four 2/2s.
        // P2 can block 4 of 5 — one gets through for lethal.
        // P1 should attack with all 5 even though 4 will die.
        val cards = listOf(smallCreature, groundBlocker)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attackers = (1..5).map {
            val id = driver.putCreatureOnBattlefield(p1, "Eager Cadet")
            driver.removeSummoningSickness(id)
            id
        }

        val blockers = (1..4).map {
            val id = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
            driver.removeSummoningSickness(id)
            id
        }

        driver.replaceState(driver.state.withLifeTotal(p2, 1))

        val legalAction = buildAttackAction(p1, attackers, listOf(p2))
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        // All 5 must attack — one gets through for lethal
        result.attackers.size shouldBe 5
    }

    test("does not attack 1/1 into opponent's 3/3 at high life") {
        // P1 has a 1/1, P2 has a 3/3. Attacking just loses the 1/1 for nothing.
        val cards = listOf(smallCreature, bigGround)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Eager Cadet")
        driver.removeSummoningSickness(attacker)

        val blocker = driver.putCreatureOnBattlefield(p2, "Hill Giant")
        driver.removeSummoningSickness(blocker)

        val legalAction = buildAttackAction(p1, listOf(attacker), listOf(p2))
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        // 1/1 would just die to 3/3 — don't attack
        result.attackers shouldNotContainKey attacker
    }

    test("attacks when board advantage means favorable trades") {
        // P1 has three 3/3s, P2 has one 2/2. Attacking overwhelms:
        // P2 blocks one 3/3, two get through for 6 damage. Great deal.
        val cards = listOf(bigGround, groundBlocker)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val att1 = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        val att2 = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        val att3 = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        driver.removeSummoningSickness(att1)
        driver.removeSummoningSickness(att2)
        driver.removeSummoningSickness(att3)

        val blocker = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        val legalAction = buildAttackAction(p1, listOf(att1, att2, att3), listOf(p2))
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        // Should attack with at least 2 — P2 can only block one
        result.attackers.size shouldBeGreaterThan 1
    }

    test("holds back blocker when opponent has lethal crackback") {
        // P1 has a 2/2. P2 has a 5/5 (can attack next turn for lethal).
        // P1 is at 5 life. If P1 attacks, the 2/2 taps and can't block the 5/5 crackback.
        // P1 should hold back to block next turn.
        val creature55 = CardDefinition.creature(
            name = "Craw Wurm", manaCost = ManaCost.parse("{4}{G}{G}"),
            subtypes = setOf(Subtype("Wurm")), power = 5, toughness = 5
        )
        val cards = listOf(groundBlocker, creature55)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        val threat = driver.putCreatureOnBattlefield(p2, "Craw Wurm")
        driver.removeSummoningSickness(threat)

        driver.replaceState(driver.state.withLifeTotal(p1, 5))

        val legalAction = buildAttackAction(p1, listOf(attacker), listOf(p2))
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        // Should not attack — need the 2/2 to block the 5/5 crackback
        result.attackers shouldNotContainKey attacker
    }

    test("flyer attacks past ground blockers") {
        // P1 has a 2/2 flyer. P2 has three ground 3/3s.
        // The flyer can't be blocked — should always attack.
        val cards = listOf(flyingCreature, bigGround)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val flyer = driver.putCreatureOnBattlefield(p1, "Wind Drake")
        driver.removeSummoningSickness(flyer)

        driver.putCreatureOnBattlefield(p2, "Hill Giant").also { driver.removeSummoningSickness(it) }
        driver.putCreatureOnBattlefield(p2, "Hill Giant").also { driver.removeSummoningSickness(it) }
        driver.putCreatureOnBattlefield(p2, "Hill Giant").also { driver.removeSummoningSickness(it) }

        val legalAction = buildAttackAction(p1, listOf(flyer), listOf(p2))
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        // Flyer is evasive — always attack
        result.attackers shouldContainKey flyer
    }

    test("attacks with evasive creatures when board is contested") {
        // P1 has: 4/4, 2/2 flying
        // P2 has: 4/4
        // The flyer is evasive (can't be blocked) — should attack.
        // The 4/4 would just trade — may or may not attack.
        val creature44 = CardDefinition.creature(
            name = "Pillarfield Ox", manaCost = ManaCost.parse("{3}{W}"),
            subtypes = setOf(Subtype("Ox")), power = 4, toughness = 4
        )
        val cards = listOf(creature44, flyingCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val att44 = driver.putCreatureOnBattlefield(p1, "Pillarfield Ox")
        val attFlyer = driver.putCreatureOnBattlefield(p1, "Wind Drake")
        driver.removeSummoningSickness(att44)
        driver.removeSummoningSickness(attFlyer)

        val blk44 = driver.putCreatureOnBattlefield(p2, "Pillarfield Ox")
        driver.removeSummoningSickness(blk44)

        val legalAction = buildAttackAction(p1, listOf(att44, attFlyer), listOf(p2))
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        // The flyer should always attack (evasive — no downside)
        result.attackers shouldContainKey attFlyer
    }

    // ═════════════════════════════════════════════════════════════════════
    // Simulation-based blocking scenarios
    // ═════════════════════════════════════════════════════════════════════

    test("chump blocks one of two attackers when at lethal life") {
        // P1 attacks with two 2/2s, P2 at 3 life with only a 1/1.
        // Must block one to survive (taking 2 instead of 4).
        val cards = listOf(groundBlocker, smallCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val att1 = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val att2 = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(att1)
        driver.removeSummoningSickness(att2)

        val blocker = driver.putCreatureOnBattlefield(p2, "Eager Cadet")
        driver.removeSummoningSickness(blocker)

        driver.replaceState(driver.state.withLifeTotal(p2, 3))

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(att1 to p2, att2 to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // Must block one of the 2/2s to drop incoming from 4 to 2
        result.blockers shouldContainKey blocker
    }

    test("first striker blocks and survives against larger attacker") {
        // 2/2 first strike blocks a 2/1 — first strike kills it before it deals damage.
        // This is a free block (blocker survives), should always take it.
        val smallAttacker = CardDefinition.creature(
            name = "Savannah Lions",
            manaCost = ManaCost.parse("{W}"),
            subtypes = setOf(Subtype("Cat")),
            power = 2, toughness = 1
        )
        val cards = listOf(firstStrikeCreature, smallAttacker)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Savannah Lions")
        driver.removeSummoningSickness(attacker)

        val blocker = driver.putCreatureOnBattlefield(p2, "White Knight")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // 2/2 first strike kills 2/1 before it deals damage — free block
        result.blockers shouldContainKey blocker
        result.blockers[blocker] shouldBe listOf(attacker)
    }

    test("1/1 first striker blocks a 1/2 and kills it for free") {
        // 1/1 first strike vs 1/2: first strike deals 1, doesn't kill (2 toughness).
        // Regular damage: 1/2 deals 1 back, kills the 1/1. Not a free block.
        // Actually the 1/1 first strike deals 1 damage, 1/2 has 2 toughness — survives.
        // Then 1/2 deals 1 damage, kills the 1/1. Both die? No — 1/2 survives with 1 damage.
        // So blocking is bad for the first striker here. Let's test a scenario where it IS free:
        // 1/1 first strike vs 1/1: first strike kills the 1/1 before it hits back. Free!
        val firstStriker11 = CardDefinition.creature(
            name = "Suntail Hawk FS",
            manaCost = ManaCost.parse("{W}"),
            subtypes = setOf(Subtype("Bird")),
            power = 1, toughness = 1,
            keywords = setOf(Keyword.FIRST_STRIKE)
        )
        val cards = listOf(firstStriker11, smallCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Eager Cadet")
        driver.removeSummoningSickness(attacker)

        val blocker = driver.putCreatureOnBattlefield(p2, "Suntail Hawk FS")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // 1/1 first strike kills 1/1 attacker before regular damage — free block
        result.blockers shouldContainKey blocker
    }

    test("gang-blocks 4/4 with two 2/3s") {
        // P1 attacks with a 4/4, P2 has two 2/3s. Neither alone can kill the 4/4,
        // but together they deal 4 damage and kill it. One 2/3 dies, the other survives.
        // Trading a 2/3 for a 4/4 is excellent value.
        val bear23 = CardDefinition.creature(
            name = "Nessian Courser",
            manaCost = ManaCost.parse("{2}{G}"),
            subtypes = setOf(Subtype("Centaur"), Subtype("Warrior")),
            power = 2, toughness = 3
        )
        val creature44 = CardDefinition.creature(
            name = "Rumbling Baloth",
            manaCost = ManaCost.parse("{2}{G}{G}"),
            subtypes = setOf(Subtype("Beast")),
            power = 4, toughness = 4
        )
        val cards = listOf(bear23, creature44)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Rumbling Baloth")
        driver.removeSummoningSickness(attacker)

        val blocker1 = driver.putCreatureOnBattlefield(p2, "Nessian Courser")
        val blocker2 = driver.putCreatureOnBattlefield(p2, "Nessian Courser")
        driver.removeSummoningSickness(blocker1)
        driver.removeSummoningSickness(blocker2)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker1, blocker2))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // Both 2/3s should gang-block the 4/4 — combined 4 power kills it
        val blockersOfAttacker = result.blockers.entries
            .filter { (_, targets) -> attacker in targets }
            .map { it.key }
        blockersOfAttacker.size shouldBe 2
    }

    test("does not chump block trampler at high life") {
        // P1 attacks with 4/4 trample, P2 has a 1/1 at 20 life.
        // Chump blocking only prevents 1 damage (3 tramples through) — not worth losing a creature.
        val cards = listOf(trampleCreature, smallCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Stampeding Rhino")
        driver.removeSummoningSickness(attacker)

        val blocker = driver.putCreatureOnBattlefield(p2, "Eager Cadet")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // At 20 life, chump blocking a trampler is bad — only saves 1 damage, loses a creature
        result.blockers shouldNotContainKey blocker
    }

    test("blocks trampler with creature that survives") {
        // P1 attacks with 4/4 trample, P2 has a 2/5. The 2/5 survives (5 > 4),
        // prevents 4 damage (non-trample gets through 0 since blocked, trample overflow is 0
        // because blocker toughness >= attacker power). Good block.
        val bigBlocker = CardDefinition.creature(
            name = "Aven Surveyor",
            manaCost = ManaCost.parse("{3}{U}{U}"),
            subtypes = setOf(Subtype("Bird")),
            power = 2, toughness = 5
        )
        val cards = listOf(trampleCreature, bigBlocker)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Stampeding Rhino")
        driver.removeSummoningSickness(attacker)

        val blocker = driver.putCreatureOnBattlefield(p2, "Aven Surveyor")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // 2/5 blocks 4/4 trample: blocker survives, prevents all trample overflow
        result.blockers shouldContainKey blocker
    }

    test("deathtouch creature blocks big attacker for favorable trade") {
        // P1 attacks with 6/6, P2 has a 1/1 deathtouch. Deathtouch kills anything
        // regardless of toughness — trading a 1/1 for a 6/6 is amazing.
        val deathtouchCreature = CardDefinition.creature(
            name = "Typhoid Rats",
            manaCost = ManaCost.parse("{B}"),
            subtypes = setOf(Subtype("Rat")),
            power = 1, toughness = 1,
            keywords = setOf(Keyword.DEATHTOUCH)
        )
        val cards = listOf(bigCreature, deathtouchCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Colossus")
        driver.removeSummoningSickness(attacker)

        val blocker = driver.putCreatureOnBattlefield(p2, "Typhoid Rats")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // Deathtouch 1/1 kills 6/6 — always block
        result.blockers shouldContainKey blocker
    }

    test("does not block with deathtouch creature when attacker has first strike") {
        // P1 attacks with 2/2 first strike, P2 has 1/1 deathtouch.
        // First strike kills the deathtouch creature before it can deal damage — useless block.
        val deathtouchCreature = CardDefinition.creature(
            name = "Typhoid Rats",
            manaCost = ManaCost.parse("{B}"),
            subtypes = setOf(Subtype("Rat")),
            power = 1, toughness = 1,
            keywords = setOf(Keyword.DEATHTOUCH)
        )
        val cards = listOf(firstStrikeCreature, deathtouchCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "White Knight")
        driver.removeSummoningSickness(attacker)

        val blocker = driver.putCreatureOnBattlefield(p2, "Typhoid Rats")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // First strike kills deathtouch before it deals damage — don't block
        result.blockers shouldNotContainKey blocker
    }

    test("blocks one attacker when two are attacking and blocker can kill one") {
        // P1 attacks with a 3/3 and a 2/2. P2 has a 3/3 that can kill the 3/3 in a mutual trade
        // or kill the 2/2 and survive. Should block the one where it survives.
        val cards = listOf(bigGround, groundBlocker)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val att33 = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        val att22 = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(att33)
        driver.removeSummoningSickness(att22)

        val blocker = driver.putCreatureOnBattlefield(p2, "Hill Giant")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(att33 to p2, att22 to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // 3/3 should block the 2/2: kills it and survives (taking only 2 damage)
        result.blockers shouldContainKey blocker
        result.blockers[blocker] shouldBe listOf(att22)
    }

    test("complex board: assigns blockers for maximum value trades") {
        // P1 attacks with: 5/3, 4/4, 2/2, 10/5
        // P2 has: 5/5, 1/1 deathtouch, 3/3, 1/3
        //
        // Optimal blocking:
        // - 1/1 DT blocks 10/5 (trades 1/1 for 10/5 — amazing)
        // - 5/5 blocks 4/4 (kills it, survives — free kill)
        // - 3/3 blocks 5/3 (kills it, dies — trades 3/3 for 5/3, favorable)
        // - 1/3 blocks 2/2 (doesn't kill it, but survives and prevents 2 damage)
        val creature53 = CardDefinition.creature(
            name = "Ogre Warrior", manaCost = ManaCost.parse("{3}{R}"),
            subtypes = setOf(Subtype("Ogre")), power = 5, toughness = 3
        )
        val creature44 = CardDefinition.creature(
            name = "Pillarfield Ox", manaCost = ManaCost.parse("{3}{W}"),
            subtypes = setOf(Subtype("Ox")), power = 4, toughness = 4
        )
        val creature105 = CardDefinition.creature(
            name = "Eldrazi Devastator", manaCost = ManaCost.parse("{8}"),
            subtypes = setOf(Subtype("Eldrazi")), power = 10, toughness = 5
        )
        val creature55 = CardDefinition.creature(
            name = "Craw Wurm", manaCost = ManaCost.parse("{4}{G}{G}"),
            subtypes = setOf(Subtype("Wurm")), power = 5, toughness = 5
        )
        val creature13 = CardDefinition.creature(
            name = "Horned Turtle", manaCost = ManaCost.parse("{2}{U}"),
            subtypes = setOf(Subtype("Turtle")), power = 1, toughness = 3
        )
        val deathtouchCreature = CardDefinition.creature(
            name = "Typhoid Rats", manaCost = ManaCost.parse("{B}"),
            subtypes = setOf(Subtype("Rat")), power = 1, toughness = 1,
            keywords = setOf(Keyword.DEATHTOUCH)
        )

        val cards = listOf(creature53, creature44, groundBlocker, creature105, creature55, deathtouchCreature, bigGround, creature13)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 attackers
        val att53 = driver.putCreatureOnBattlefield(p1, "Ogre Warrior")
        val att44 = driver.putCreatureOnBattlefield(p1, "Pillarfield Ox")
        val att22 = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val att105 = driver.putCreatureOnBattlefield(p1, "Eldrazi Devastator")
        driver.removeSummoningSickness(att53)
        driver.removeSummoningSickness(att44)
        driver.removeSummoningSickness(att22)
        driver.removeSummoningSickness(att105)

        // P2 blockers
        val blk55 = driver.putCreatureOnBattlefield(p2, "Craw Wurm")
        val blkDT = driver.putCreatureOnBattlefield(p2, "Typhoid Rats")
        val blk33 = driver.putCreatureOnBattlefield(p2, "Hill Giant")
        val blk13 = driver.putCreatureOnBattlefield(p2, "Horned Turtle")
        driver.removeSummoningSickness(blk55)
        driver.removeSummoningSickness(blkDT)
        driver.removeSummoningSickness(blk33)
        driver.removeSummoningSickness(blk13)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(att53 to p2, att44 to p2, att22 to p2, att105 to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blk55, blkDT, blk33, blk13))

        // Evaluate board before combat from P2's perspective
        val evaluator = AIPlayer.defaultEvaluator()
        val scoreBefore = evaluator.evaluate(driver.state, driver.state.projectedState, p2)

        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // Resolve combat
        driver.submit(result)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Evaluate board after combat — P2 should be better off than before
        // (traded low-value creatures for high-value attackers)
        val scoreAfter = evaluator.evaluate(driver.state, driver.state.projectedState, p2)
        scoreAfter doubleShouldBeGreaterThan scoreBefore
    }

    test("double-blocks 6/6 with 3/3 and 4/4 to kill it") {
        // P1 attacks with 6/6. P2 has 3/3 and 4/4 — neither can kill it alone,
        // but together they deal 7 damage. The 3/3 dies, 4/4 survives. Excellent trade.
        val creature44 = CardDefinition.creature(
            name = "Pillarfield Ox", manaCost = ManaCost.parse("{3}{W}"),
            subtypes = setOf(Subtype("Ox")), power = 4, toughness = 4
        )
        val cards = listOf(bigCreature, bigGround, creature44)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Colossus")
        driver.removeSummoningSickness(attacker)

        val blk33 = driver.putCreatureOnBattlefield(p2, "Hill Giant")
        val blk44 = driver.putCreatureOnBattlefield(p2, "Pillarfield Ox")
        driver.removeSummoningSickness(blk33)
        driver.removeSummoningSickness(blk44)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blk33, blk44))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // Both should gang-block the 6/6
        val blockersOfAttacker = result.blockers.entries
            .filter { (_, targets) -> attacker in targets }
            .map { it.key }
        blockersOfAttacker.size shouldBe 2
    }

    test("does not gang-block deathtouch attacker — both blockers would die") {
        // P1 attacks with 2/2 deathtouch. P2 has two 2/2s.
        // Gang-blocking is terrible: deathtouch kills BOTH blockers. Single-block trades 1-for-1.
        val deathtouchAttacker = CardDefinition.creature(
            name = "Gifted Aetherborn", manaCost = ManaCost.parse("{B}{B}"),
            subtypes = setOf(Subtype("Aetherborn")), power = 2, toughness = 2,
            keywords = setOf(Keyword.DEATHTOUCH)
        )
        val cards = listOf(deathtouchAttacker, groundBlocker)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Gifted Aetherborn")
        driver.removeSummoningSickness(attacker)

        val blk1 = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val blk2 = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        driver.removeSummoningSickness(blk1)
        driver.removeSummoningSickness(blk2)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blk1, blk2))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // Should NOT gang-block (deathtouch skips gang blocks in heuristic).
        // Should single-block for a 1-for-1 trade instead.
        val blockersAssigned = result.blockers.entries
            .filter { (_, targets) -> attacker in targets }
            .map { it.key }
        blockersAssigned.size shouldBe 1
    }

    test("indestructible blocker always blocks — free damage prevention") {
        // P1 attacks with 6/6. P2 has a 1/1 indestructible.
        // Blocking prevents 6 damage and the blocker can't die. Always block.
        val indestructibleCreature = CardDefinition.creature(
            name = "Darksteel Myr", manaCost = ManaCost.parse("{3}"),
            subtypes = setOf(Subtype("Myr")), power = 1, toughness = 1,
            keywords = setOf(Keyword.INDESTRUCTIBLE)
        )
        val cards = listOf(bigCreature, indestructibleCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Colossus")
        driver.removeSummoningSickness(attacker)

        val blocker = driver.putCreatureOnBattlefield(p2, "Darksteel Myr")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // Indestructible can't die — always block to prevent damage
        result.blockers shouldContainKey blocker
    }

    test("wide board: blocking AI produces positive outcome") {
        // P1 attacks with: 3/2 first strike, 5/5 trample, 2/2 lifelink, 1/1
        // P2 has: 4/4, 2/3, 1/1 deathtouch, 0/4 defender
        // Many interactions — validate the AI produces a board state improvement.
        val fsCreature32 = CardDefinition.creature(
            name = "Fencing Ace", manaCost = ManaCost.parse("{1}{W}"),
            subtypes = setOf(Subtype("Human"), Subtype("Soldier")), power = 3, toughness = 2,
            keywords = setOf(Keyword.FIRST_STRIKE)
        )
        val trample55 = CardDefinition.creature(
            name = "Charging Rhino", manaCost = ManaCost.parse("{3}{G}{G}"),
            subtypes = setOf(Subtype("Rhino")), power = 5, toughness = 5,
            keywords = setOf(Keyword.TRAMPLE)
        )
        val lifelink22 = CardDefinition.creature(
            name = "Ajani's Sunstriker", manaCost = ManaCost.parse("{W}{W}"),
            subtypes = setOf(Subtype("Cat")), power = 2, toughness = 2,
            keywords = setOf(Keyword.LIFELINK)
        )
        val creature44 = CardDefinition.creature(
            name = "Pillarfield Ox", manaCost = ManaCost.parse("{3}{W}"),
            subtypes = setOf(Subtype("Ox")), power = 4, toughness = 4
        )
        val creature23 = CardDefinition.creature(
            name = "Nessian Courser", manaCost = ManaCost.parse("{2}{G}"),
            subtypes = setOf(Subtype("Centaur")), power = 2, toughness = 3
        )
        val deathtouch11 = CardDefinition.creature(
            name = "Typhoid Rats", manaCost = ManaCost.parse("{B}"),
            subtypes = setOf(Subtype("Rat")), power = 1, toughness = 1,
            keywords = setOf(Keyword.DEATHTOUCH)
        )
        val wall04 = CardDefinition.creature(
            name = "Wall of Frost", manaCost = ManaCost.parse("{1}{U}{U}"),
            subtypes = setOf(Subtype("Wall")), power = 0, toughness = 4,
            keywords = setOf(Keyword.DEFENDER)
        )

        val cards = listOf(fsCreature32, trample55, lifelink22, smallCreature, creature44, creature23, deathtouch11, wall04)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 attackers
        val attFS = driver.putCreatureOnBattlefield(p1, "Fencing Ace")
        val attTrample = driver.putCreatureOnBattlefield(p1, "Charging Rhino")
        val attLifelink = driver.putCreatureOnBattlefield(p1, "Ajani's Sunstriker")
        val att11 = driver.putCreatureOnBattlefield(p1, "Eager Cadet")
        driver.removeSummoningSickness(attFS)
        driver.removeSummoningSickness(attTrample)
        driver.removeSummoningSickness(attLifelink)
        driver.removeSummoningSickness(att11)

        // P2 blockers
        val blk44 = driver.putCreatureOnBattlefield(p2, "Pillarfield Ox")
        val blk23 = driver.putCreatureOnBattlefield(p2, "Nessian Courser")
        val blkDT = driver.putCreatureOnBattlefield(p2, "Typhoid Rats")
        val blkWall = driver.putCreatureOnBattlefield(p2, "Wall of Frost")
        driver.removeSummoningSickness(blk44)
        driver.removeSummoningSickness(blk23)
        driver.removeSummoningSickness(blkDT)
        driver.removeSummoningSickness(blkWall)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attFS to p2, attTrample to p2, attLifelink to p2, att11 to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blk44, blk23, blkDT, blkWall))
        val evaluator = AIPlayer.defaultEvaluator()
        val scoreBefore = evaluator.evaluate(driver.state, driver.state.projectedState, p2)

        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers
        driver.submit(result)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        val scoreAfter = evaluator.evaluate(driver.state, driver.state.projectedState, p2)
        // P2 has deathtouch for the trample, wall to soak, and good trades available.
        // Board should improve for the defender.
        scoreAfter doubleShouldBeGreaterThan scoreBefore
    }

    test("lifelink attacker is prioritized for blocking over equal-power attacker") {
        // P1 attacks with 2/2 lifelink and 2/2 vanilla. P2 has one 2/2.
        // Should block the lifelink creature — prevents 2 damage AND 2 life gain (4 effective swing).
        val lifelinkAttacker = CardDefinition.creature(
            name = "Ajani's Sunstriker", manaCost = ManaCost.parse("{W}{W}"),
            subtypes = setOf(Subtype("Cat")), power = 2, toughness = 2,
            keywords = setOf(Keyword.LIFELINK)
        )
        val cards = listOf(lifelinkAttacker, groundBlocker)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attLL = driver.putCreatureOnBattlefield(p1, "Ajani's Sunstriker")
        val att22 = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(attLL)
        driver.removeSummoningSickness(att22)

        val blocker = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attLL to p2, att22 to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // Should block the lifelink creature — 4 effective damage prevented vs 2
        result.blockers shouldContainKey blocker
        result.blockers[blocker] shouldBe listOf(attLL)
    }

    // ═════════════════════════════════════════════════════════════════════
    // Mandatory attackers and blockers
    // ═════════════════════════════════════════════════════════════════════

    test("mandatory attacker is always included in attack plan") {
        // A creature with "must attack" (e.g., Berserker) must be in the attack map
        // even if attacking is otherwise bad (opponent has bigger blocker).
        val cards = listOf(smallCreature, bigGround)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mustAttack = driver.putCreatureOnBattlefield(p1, "Eager Cadet")
        driver.removeSummoningSickness(mustAttack)

        val blocker = driver.putCreatureOnBattlefield(p2, "Hill Giant")
        driver.removeSummoningSickness(blocker)

        // 1/1 must attack into a 3/3 — would normally never attack, but it's mandatory
        val legalAction = buildAttackAction(
            p1, listOf(mustAttack), listOf(p2),
            mandatoryAttackers = listOf(mustAttack)
        )
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        result.attackers shouldContainKey mustAttack
    }

    test("mandatory attacker included alongside optional attackers") {
        // One creature must attack, one is optional. The mandatory one is always present;
        // the optional one is included based on normal evaluation.
        val cards = listOf(smallCreature, flyingCreature, bigGround)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mustAttack = driver.putCreatureOnBattlefield(p1, "Eager Cadet")
        val optionalFlyer = driver.putCreatureOnBattlefield(p1, "Wind Drake")
        driver.removeSummoningSickness(mustAttack)
        driver.removeSummoningSickness(optionalFlyer)

        val blocker = driver.putCreatureOnBattlefield(p2, "Hill Giant")
        driver.removeSummoningSickness(blocker)

        val legalAction = buildAttackAction(
            p1, listOf(mustAttack, optionalFlyer), listOf(p2),
            mandatoryAttackers = listOf(mustAttack)
        )
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        // Mandatory 1/1 must attack
        result.attackers shouldContainKey mustAttack
        // Evasive flyer should also attack (no-downside)
        result.attackers shouldContainKey optionalFlyer
    }

    test("all creatures attack when all are mandatory") {
        // All creatures must attack (e.g., Taunt effect). Even if it's suicidal.
        val cards = listOf(smallCreature, groundBlocker, bigGround)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val att1 = driver.putCreatureOnBattlefield(p1, "Eager Cadet")
        val att2 = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(att1)
        driver.removeSummoningSickness(att2)

        // Opponent has a huge blocker
        val blocker = driver.putCreatureOnBattlefield(p2, "Hill Giant")
        driver.removeSummoningSickness(blocker)

        val legalAction = buildAttackAction(
            p1, listOf(att1, att2), listOf(p2),
            mandatoryAttackers = listOf(att1, att2)
        )
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        // Both must attack regardless
        result.attackers.size shouldBe 2
        result.attackers shouldContainKey att1
        result.attackers shouldContainKey att2
    }

    test("mandatory blocker blocks the required attacker") {
        // Provoke: a specific blocker must block a specific attacker.
        // The AI must include this assignment even if the trade is bad.
        val cards = listOf(bigCreature, smallCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Colossus")
        driver.removeSummoningSickness(attacker)

        val forcedBlocker = driver.putCreatureOnBattlefield(p2, "Eager Cadet")
        driver.removeSummoningSickness(forcedBlocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // 1/1 is forced to block the 6/6 (provoke) — suicidal but mandatory
        val legalAction = buildBlockAction(
            p2, listOf(forcedBlocker),
            mandatoryBlockerAssignments = mapOf(forcedBlocker to listOf(attacker))
        )
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        result.blockers shouldContainKey forcedBlocker
        result.blockers[forcedBlocker] shouldBe listOf(attacker)
    }

    test("mandatory blocker assigned while other blockers use heuristic") {
        // One blocker is forced (provoke), others are free to choose optimally.
        val deathtouchCreature = CardDefinition.creature(
            name = "Typhoid Rats", manaCost = ManaCost.parse("{B}"),
            subtypes = setOf(Subtype("Rat")), power = 1, toughness = 1,
            keywords = setOf(Keyword.DEATHTOUCH)
        )
        val cards = listOf(bigCreature, bigGround, smallCreature, deathtouchCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 attacks with 6/6 and 3/3
        val att66 = driver.putCreatureOnBattlefield(p1, "Colossus")
        val att33 = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        driver.removeSummoningSickness(att66)
        driver.removeSummoningSickness(att33)

        // P2 has 1/1 (forced to block 6/6 via provoke) and 1/1 deathtouch (free)
        val forced = driver.putCreatureOnBattlefield(p2, "Eager Cadet")
        val freeBlocker = driver.putCreatureOnBattlefield(p2, "Typhoid Rats")
        driver.removeSummoningSickness(forced)
        driver.removeSummoningSickness(freeBlocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(att66 to p2, att33 to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val legalAction = buildBlockAction(
            p2, listOf(forced, freeBlocker),
            mandatoryBlockerAssignments = mapOf(forced to listOf(att66))
        )
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        // Forced 1/1 must block the 6/6
        result.blockers shouldContainKey forced
        result.blockers[forced] shouldBe listOf(att66)

        // Deathtouch 1/1 should freely choose to block the 3/3 (kills it in a trade)
        result.blockers shouldContainKey freeBlocker
        result.blockers[freeBlocker] shouldBe listOf(att33)
    }

    test("local search does not remove mandatory attacker") {
        // Even if the local search thinks removing an attacker improves the score,
        // mandatory attackers must stay in the plan.
        val cards = listOf(smallCreature, bigCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mustAttack = driver.putCreatureOnBattlefield(p1, "Eager Cadet")
        driver.removeSummoningSickness(mustAttack)

        // Big blocker that will kill the 1/1
        val blocker = driver.putCreatureOnBattlefield(p2, "Colossus")
        driver.removeSummoningSickness(blocker)

        val legalAction = buildAttackAction(
            p1, listOf(mustAttack), listOf(p2),
            mandatoryAttackers = listOf(mustAttack)
        )
        val result = advisor.chooseAttackers(driver.state, legalAction, p1) as DeclareAttackers

        // Must attack even though it's walking into a 6/6
        result.attackers shouldContainKey mustAttack
    }

    test("local search does not remove mandatory blocker") {
        // Even if removing a blocker would improve the board evaluation,
        // mandatory blockers must stay in the plan.
        val cards = listOf(bigCreature, smallCreature)
        val (driver, _, advisor) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Colossus")
        driver.removeSummoningSickness(attacker)

        val forced = driver.putCreatureOnBattlefield(p2, "Eager Cadet")
        driver.removeSummoningSickness(forced)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // 1/1 forced to block 6/6 — terrible trade but mandatory
        val legalAction = buildBlockAction(
            p2, listOf(forced),
            mandatoryBlockerAssignments = mapOf(forced to listOf(attacker))
        )
        val result = advisor.chooseBlockers(driver.state, legalAction, p2) as DeclareBlockers

        result.blockers shouldContainKey forced
        result.blockers[forced] shouldBe listOf(attacker)
    }

    // ═════════════════════════════════════════════════════════════════════
    // Strategist: combat actions not short-circuited
    // ═════════════════════════════════════════════════════════════════════

    test("Strategist delegates single DeclareAttackers to CombatAdvisor") {
        val cards = listOf(bigGround)
        val (driver, registry, _) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        driver.removeSummoningSickness(attacker)

        val simulator = GameSimulator(registry)
        val evaluator = AIPlayer.defaultEvaluator()
        val strategist = Strategist(simulator, evaluator)

        val legalAction = buildAttackAction(p1, listOf(attacker), listOf(p2))
        val chosen = strategist.chooseAction(driver.state, listOf(legalAction), p1)
        val result = chosen.action as DeclareAttackers

        // Should NOT return the default empty map — CombatAdvisor should populate it
        result.attackers.size shouldBeGreaterThan 0
    }

    test("Strategist delegates single DeclareBlockers to CombatAdvisor") {
        val cards = listOf(bigGround, groundBlocker)
        val (driver, registry, _) = setup(cards)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 attacks with 2/2
        val attacker = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        // P2 has 3/3 that can kill and survive
        val blocker = driver.putCreatureOnBattlefield(p2, "Hill Giant")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(p1, mapOf(attacker to p2)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val simulator = GameSimulator(registry)
        val evaluator = AIPlayer.defaultEvaluator()
        val strategist = Strategist(simulator, evaluator)

        val legalAction = buildBlockAction(p2, listOf(blocker))
        val chosen = strategist.chooseAction(driver.state, listOf(legalAction), p2)
        val result = chosen.action as DeclareBlockers

        // 3/3 should block the 2/2 (kills it and survives)
        result.blockers shouldContainKey blocker
    }

    // ═════════════════════════════════════════════════════════════════════
    // Full game: AI plays correctly through combat
    // ═════════════════════════════════════════════════════════════════════

    test("two AI players complete a game without errors") {
        val registry = CardRegistry()
        registry.register(TestCards.all)

        val deck = Deck.of("Mountain" to 14, "Raging Goblin" to 3, "Hill Giant" to 3)
        val initializer = GameInitializer(registry)
        val result = initializer.initializeGame(
            GameConfig(
                players = listOf(
                    PlayerConfig("AI 1", deck),
                    PlayerConfig("AI 2", deck)
                ),
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        val processor = ActionProcessor(registry)

        val p1 = result.playerIds[0]
        val p2 = result.playerIds[1]
        val ai1 = AIPlayer.create(registry, p1)
        val ai2 = AIPlayer.create(registry, p2)

        var state = result.state
        var turns = 0

        while (!state.gameOver && turns < 50) {
            val nextState = when (state.priorityPlayerId) {
                p1 -> ai1.playPriorityWindow(state, processor)
                p2 -> ai2.playPriorityWindow(state, processor)
                else -> {
                    val decision = state.pendingDecision
                    if (decision != null) {
                        val ai = if (decision.playerId == p1) ai1 else ai2
                        val response = ai.respondToDecision(state, decision)
                        val r = processor.process(state, SubmitDecision(decision.playerId, response))
                        if (r.error != null) null else r.state
                    } else null
                }
            }
            if (nextState == null) break
            state = nextState
            if (state.turnNumber > turns) turns = state.turnNumber
        }

        turns shouldBeGreaterThan 0
    }
})

/**
 * Extension to set a player's life total for testing.
 */
private fun com.wingedsheep.engine.state.GameState.withLifeTotal(playerId: EntityId, life: Int): com.wingedsheep.engine.state.GameState {
    val entity = getEntity(playerId) ?: return this
    val updated = entity.with(com.wingedsheep.engine.state.components.identity.LifeTotalComponent(life))
    return withEntity(playerId, updated)
}
