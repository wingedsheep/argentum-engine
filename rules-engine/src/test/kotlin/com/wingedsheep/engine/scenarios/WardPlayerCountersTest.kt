package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.effects.WardCounterEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Engine tests for `WardCost.PlayerCounters` — "Ward—Get five poison counters" (The Serpent
 * Society), a ward cost paid in counters placed on the *paying player* (CR 702.21a, CR 122.1).
 *
 * The distinguishing rules, each pinned below:
 * - the counters land on the payer, not on the warded permanent or its controller;
 * - it is the one ward cost that is **always payable** — no board or hand state can make it
 *   unpayable, so it always prompts rather than countering silently;
 * - declining still counters, and no counters are placed;
 * - ten or more poison counters lose the game as a state-based action (CR 122.1f), which follows
 *   from the counters going through the ordinary counter-placement path;
 * - it composes inside a `WardCost.Composite` like any other atomic ward cost;
 * - it works identically when granted by a static ability (`GrantWard`).
 */
class WardPlayerCountersTest : FunSpec({

    val poisonWardedBear = card("Poison-Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.WARD)
        keywordAbility(KeywordAbility.wardPlayerCounters(Counters.POISON, 5))
    }

    // "Ward—{1}, Get one poison counter" — the counter cost as one part of an AND.
    val compositeWardedBear = card("Composite-Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.WARD)
        keywordAbility(
            KeywordAbility.wardComposite(
                WardCost.Mana("{1}"),
                WardCost.PlayerCounters(Counters.POISON, 1),
            )
        )
    }

    val poisonWardEmitter = card("Poison Ward Emitter") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
        staticAbility {
            ability = GrantWard(
                cost = WardCost.PlayerCounters(Counters.POISON, 5),
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
        driver.registerCards(
            TestCards.all + listOf(poisonWardedBear, compositeWardedBear, poisonWardEmitter, plainBear)
        )
        return driver
    }

    fun poison(driver: GameTestDriver, playerId: EntityId): Int =
        driver.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    test("paying the counter cost puts the counters on the paying player and the spell resolves") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Poison-Warded Bear")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()
        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<YesNoDecision>()
        decision.playerId shouldBe activePlayer

        driver.submitYesNo(activePlayer, true)
        repeat(3) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        withClue("the counters go on the payer, not on the warded permanent's controller") {
            poison(driver, activePlayer) shouldBe 5
            poison(driver, opponent) shouldBe 0
        }
        withClue("paying lets the bolt through") {
            driver.findPermanent(opponent, "Poison-Warded Bear") shouldBe null
        }
    }

    test("declining counters the spell and places no counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Poison-Warded Bear")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(activePlayer, false)
        repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        poison(driver, activePlayer) shouldBe 0
        driver.findPermanent(opponent, "Poison-Warded Bear") shouldNotBe null
    }

    test("the cost is always payable — an empty hand, empty board and 1 life still prompt") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 1)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Poison-Warded Bear")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()

        withClue("no resource is spent, so nothing can make this cost unpayable") {
            driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        }
    }

    test("a payer already on five poison loses to the ten-poison state-based action") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.addComponent(activePlayer, CountersComponent().withAdded(CounterType.POISON, 5))

        val bear = driver.putCreatureOnBattlefield(opponent, "Poison-Warded Bear")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(activePlayer, true)
        repeat(3) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        poison(driver, activePlayer) shouldBe 10
        withClue("CR 122.1f — ten or more poison counters lose the game") {
            driver.state.gameOver shouldBe true
            driver.state.winnerId shouldBe opponent
        }
    }

    test("as one part of a composite ward, both parts are charged in order") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Composite-Warded Bear")

        // {R} for the bolt plus {1} for the ward's mana component.
        driver.giveMana(activePlayer, Color.RED, 2)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()

        // First component: the {1} mana payment (floating mana covers it).
        driver.pendingDecision.shouldNotBeNull()
        driver.submitManaAutoPayOrDecline(activePlayer, true)

        // Second component: the poison counters.
        val second = driver.pendingDecision
        second.shouldNotBeNull()
        second.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(activePlayer, true)

        repeat(3) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        poison(driver, activePlayer) shouldBe 1
        driver.findPermanent(opponent, "Composite-Warded Bear") shouldBe null
    }

    test("a statically granted counter ward prompts the same way") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Poison Ward Emitter")
        val bear = driver.putCreatureOnBattlefield(opponent, "Plain Bear")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))

        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(activePlayer, true)
        repeat(3) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        poison(driver, activePlayer) shouldBe 5
    }

    test("the printed wording renders as oracle text") {
        KeywordAbility.wardPlayerCounters(Counters.POISON, 5).description shouldBe
            "Ward—Get five poison counters"
        KeywordAbility.wardPlayerCounters(Counters.POISON, 1).description shouldBe
            "Ward—Get a poison counter"
    }

    test("the trigger's own effect and a static grant render the cost too") {
        // Three renderings share the WardCost taxonomy: the keyword line above, the third-person
        // "Counter it unless its controller ~" on the trigger's effect, and the granted-ward line.
        WardCounterEffect(WardCost.PlayerCounters(Counters.POISON, 5)).description shouldBe
            "Counter it unless its controller gets five poison counters"
        GrantWard(
            cost = WardCost.PlayerCounters(Counters.POISON, 5),
            filter = GroupFilter(GameObjectFilter.Creature.youControl()).other()
        ).description shouldContain "have \"Ward—Get five poison counters.\""
    }
})
