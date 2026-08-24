package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.mtg.sets.definitions.mkm.cards.NivMizzetGuildpact
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Niv-Mizzet, Guildpact (MKM) — {W}{U}{B}{R}{G} Legendary Creature — Dragon Avatar 6/6.
 *
 * Flying, hexproof from multicolored.
 * Whenever Niv-Mizzet deals combat damage to a player, it deals X damage to any target, target
 * player draws X cards, and you gain X life, where X is the number of different color pairs among
 * permanents you control that are exactly two colors.
 *
 * Covers both primitives the card introduced: [com.wingedsheep.sdk.scripting.values.Aggregation.DISTINCT_COLOR_PAIRS]
 * (which permanents count, and that duplicates collapse) and
 * [com.wingedsheep.sdk.scripting.GrantHexproofFromMulticoloredToGroup].
 */
class NivMizzetGuildpactScenarioTest : FunSpec({

    // Vanilla creatures whose colors derive from their mana cost.
    val izzetDrake = CardDefinition.creature("Test Izzet Drake", ManaCost.parse("{U}{R}"), emptySet(), 2, 2)
    val borosSoldier = CardDefinition.creature("Test Boros Soldier", ManaCost.parse("{R}{W}"), emptySet(), 2, 2)
    val izzetWeird = CardDefinition.creature("Test Izzet Weird", ManaCost.parse("{U}{R}"), emptySet(), 1, 1)
    val monoBear = CardDefinition.creature("Test Mono Bear", ManaCost.parse("{G}"), emptySet(), 2, 2)
    val nayaHydra = CardDefinition.creature("Test Naya Hydra", ManaCost.parse("{R}{G}{W}"), emptySet(), 3, 3)
    val colorlessGolem = CardDefinition.creature("Test Golem", ManaCost.parse("{2}"), emptySet(), 2, 2)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                NivMizzetGuildpact, izzetDrake, borosSoldier, izzetWeird, monoBear, nayaHydra, colorlessGolem,
            )
        )
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Attack the opponent with Niv-Mizzet and answer the resulting target prompt. */
    fun GameTestDriver.connectAndTarget(
        niv: EntityId,
        me: EntityId,
        opponent: EntityId,
        damageTarget: EntityId,
        drawer: EntityId,
    ) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(me, listOf(niv), defendingPlayer = opponent).error shouldBe null
        passPriorityUntil(Step.COMBAT_DAMAGE)
        bothPass() // combat damage — 6 to the opponent

        var guard = 0
        while (pendingDecision !is ChooseTargetsDecision && state.stack.isNotEmpty() && guard++ < 10) bothPass()
        (pendingDecision as ChooseTargetsDecision)
        submitMultiTargetSelection(me, mapOf(0 to listOf(damageTarget), 1 to listOf(drawer))).error shouldBe null
        guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    test("X counts distinct color pairs — two Izzet permanents and one Boros permanent give X = 2") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val niv = driver.putCreatureOnBattlefield(me, "Niv-Mizzet, Guildpact")
        driver.removeSummoningSickness(niv)
        // Two Izzet permanents collapse to one pair; Boros adds a second.
        driver.putCreatureOnBattlefield(me, "Test Izzet Drake")
        driver.putCreatureOnBattlefield(me, "Test Izzet Weird")
        driver.putCreatureOnBattlefield(me, "Test Boros Soldier")
        // None of these contribute: one color, three colors, colorless — and Niv itself is five.
        driver.putCreatureOnBattlefield(me, "Test Mono Bear")
        driver.putCreatureOnBattlefield(me, "Test Naya Hydra")
        driver.putCreatureOnBattlefield(me, "Test Golem")

        val myHandBefore = driver.getHandSize(me)
        driver.connectAndTarget(niv, me, opponent, damageTarget = opponent, drawer = me)

        // 6 combat damage + X = 2 from the trigger.
        driver.getLifeTotal(opponent) shouldBe 12
        driver.getLifeTotal(me) shouldBe 22
        driver.getHandSize(me) shouldBe myHandBefore + 2
    }

    test("X is 0 when no permanent you control is exactly two colors — no draw, no life, no damage") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val niv = driver.putCreatureOnBattlefield(me, "Niv-Mizzet, Guildpact")
        driver.removeSummoningSickness(niv)
        driver.putCreatureOnBattlefield(me, "Test Mono Bear")
        driver.putCreatureOnBattlefield(me, "Test Naya Hydra")
        // A two-color permanent the *opponent* controls must not count.
        driver.putCreatureOnBattlefield(opponent, "Test Izzet Drake")

        val myHandBefore = driver.getHandSize(me)
        driver.connectAndTarget(niv, me, opponent, damageTarget = opponent, drawer = me)

        driver.getLifeTotal(opponent) shouldBe 14 // combat damage only
        driver.getLifeTotal(me) shouldBe 20
        driver.getHandSize(me) shouldBe myHandBefore
    }

    test("the damage and the draw can point at different players") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val niv = driver.putCreatureOnBattlefield(me, "Niv-Mizzet, Guildpact")
        driver.removeSummoningSickness(niv)
        driver.putCreatureOnBattlefield(me, "Test Izzet Drake")
        val bear = driver.putCreatureOnBattlefield(opponent, "Test Mono Bear")

        val oppHandBefore = driver.getHandSize(opponent)
        // X = 1: one damage onto the opponent's bear, the opponent draws.
        driver.connectAndTarget(niv, me, opponent, damageTarget = bear, drawer = opponent)

        driver.getLifeTotal(opponent) shouldBe 14 // combat damage only — the X damage hit the bear
        driver.getLifeTotal(me) shouldBe 21
        driver.getHandSize(opponent) shouldBe oppHandBefore + 1
    }

    test("hexproof from multicolored — a two-or-more-color opponent source can't target Niv-Mizzet") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val niv = driver.putCreatureOnBattlefield(me, "Niv-Mizzet, Guildpact")

        val validator = TargetValidator()
        val target = listOf<ChosenTarget>(ChosenTarget.Permanent(niv))
        val req = listOf(TargetCreature())

        // A multicolored opponent source is blocked.
        validator.validateTargets(
            driver.state, target, req, casterId = opponent,
            sourceColors = setOf(Color.RED, Color.WHITE),
            targetingSourceType = TargetingSourceType.SPELL
        ).shouldNotBeNull()
        // A monocolored one isn't — CR 105.2b, multicolored is two *or more* colors.
        validator.validateTargets(
            driver.state, target, req, casterId = opponent,
            sourceColors = setOf(Color.RED),
            targetingSourceType = TargetingSourceType.SPELL
        ).shouldBeNull()
        // Nor is a colorless one.
        validator.validateTargets(
            driver.state, target, req, casterId = opponent,
            sourceColors = emptySet(),
            targetingSourceType = TargetingSourceType.SPELL
        ).shouldBeNull()
        // Hexproof never stops the permanent's own controller.
        validator.validateTargets(
            driver.state, target, req, casterId = me,
            sourceColors = setOf(Color.RED, Color.WHITE),
            targetingSourceType = TargetingSourceType.SPELL
        ).shouldBeNull()

        // The client DTO carries the quality so the FE renders the shield chip.
        val view = ClientStateTransformer(cardRegistry = driver.cardRegistry).transform(driver.state, viewingPlayerId = opponent)
        view.cards[niv]?.hexproofFromMulticolored shouldBe true
    }
})
