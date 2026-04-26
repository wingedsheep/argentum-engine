package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWardToGroup
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for life-cost ward: Ward—Pay N life.
 *
 * Verifies both the intrinsic-ward path (KeywordAbility.WardLife) and the
 * static-grant path (GrantWardToGroup with WardCost.Life).
 */
class WardLifeCounterTest : FunSpec({

    val lifeWardedBear = card("Life-Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.WARD)
        keywordAbility(KeywordAbility.wardLife(2))
    }

    // Source of a static grant: gives every other creature its controller controls
    // "Ward—Pay 2 life." (mirrors Hexing Squelcher's group static).
    val lifeWardEmitter = card("Life Ward Emitter") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
        staticAbility {
            ability = GrantWardToGroup(
                cost = WardCost.Life(2),
                filter = GroupFilter(GameObjectFilter.Creature.youControl()).other()
            )
        }
    }

    val plainBear = card("Plain Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(lifeWardedBear, lifeWardEmitter, plainBear))
        return driver
    }

    test("intrinsic ward-life prompts caster with yes/no decision") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Life-Warded Bear")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<YesNoDecision>()
        decision.playerId shouldBe activePlayer
    }

    test("paying ward-life lets the targeting spell resolve") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Life-Warded Bear")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        val lifeBefore = driver.getLifeTotal(activePlayer)
        driver.submitYesNo(activePlayer, true)

        // Bolt resolves and kills the 2/2 bear.
        repeat(3) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        driver.findPermanent(opponent, "Life-Warded Bear") shouldBe null
        driver.getLifeTotal(activePlayer) shouldBe (lifeBefore - 2)
    }

    test("declining ward-life payment counters the spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Life-Warded Bear")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        val lifeBefore = driver.getLifeTotal(activePlayer)
        driver.submitYesNo(activePlayer, false)

        repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        // Bear survives, no life paid.
        driver.findPermanent(opponent, "Life-Warded Bear") shouldNotBe null
        driver.getLifeTotal(activePlayer) shouldBe lifeBefore
    }

    test("ward-life counters immediately when caster cannot pay") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 1)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Life-Warded Bear")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()

        // No decision: the caster is at 1 life, can't pay 2 → counter immediately.
        driver.pendingDecision shouldBe null
        driver.findPermanent(opponent, "Life-Warded Bear") shouldNotBe null
    }

    test("granted ward-life from another permanent fires for other creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent controls the emitter and a vanilla bear that should inherit ward.
        driver.putCreatureOnBattlefield(opponent, "Life Ward Emitter")
        val bear = driver.putCreatureOnBattlefield(opponent, "Plain Bear")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<YesNoDecision>()
    }
})
