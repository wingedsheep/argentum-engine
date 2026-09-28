package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.Urabrask
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Urabrask // The Great Work (MOM #169).
 *
 * Pins the front face's cast trigger (1 damage to target opponent, add {R}), its "three or more
 * instant and/or sorcery spells this turn" transform gate, chapter I's "target opponent and each
 * creature they control", and — the new capability — chapter III's player-anchored
 * `MayCastFromGraveyard(fromAnyGraveyard = true, exileInsteadOfGraveyard = true)`: instants and
 * sorceries castable out of *any* graveyard until end of turn, exiled instead of returning to their
 * owner's graveyard, still usable after the Saga has exiled itself and come back as Urabrask.
 */
class UrabraskScenarioTest : FunSpec({

    // A {1} sorcery and a {1} instant with no targets, so a graveyard cast needs no decision.
    val sorcery = card("Urabrask Test Sorcery") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "You gain 3 life."
        spell { effect = Effects.GainLife(3) }
    }
    val instant = card("Urabrask Test Instant") {
        manaCost = "{1}"
        typeLine = "Instant"
        oracleText = "You gain 2 life."
        spell { effect = Effects.GainLife(2) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PredefinedTokens.allTokens + listOf(Urabrask, sorcery, instant))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Answer every pending target prompt with its minimum legal targets (the sole opponent). */
    fun answerTargets(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 12 && driver.isPaused) {
            when (val decision = driver.pendingDecision) {
                is ChooseTargetsDecision -> {
                    val chosen = decision.targetRequirements.associate { req ->
                        req.index to decision.legalTargets[req.index].orEmpty().take(req.minTargets)
                    }
                    driver.submitDecision(decision.playerId, TargetsResponse(decision.id, chosen))
                }
                else -> driver.autoResolveDecision()
            }
        }
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && (driver.state.stack.isNotEmpty() || driver.isPaused)) {
            answerTargets(driver)
            if (driver.state.stack.isNotEmpty()) driver.bothPass()
        }
    }

    fun advanceToNextTurnMain(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        resolveStack(driver)
    }

    fun castFromHand(driver: GameTestDriver, player: EntityId, name: String) {
        val card = driver.putCardInHand(player, name)
        driver.giveColorlessMana(player, 1)
        driver.castSpell(player, card).outcome shouldBe Outcome.Done
        answerTargets(driver)
        resolveStack(driver)
    }

    fun redInPool(driver: GameTestDriver, player: EntityId): Int =
        driver.state.getEntity(player)?.get<ManaPoolComponent>()?.red ?: 0

    fun nameOf(driver: GameTestDriver, id: EntityId): String =
        driver.state.getEntity(id)!!.get<CardComponent>()!!.name

    /** Urabrask on the battlefield, three instants cast, then transformed into The Great Work. */
    fun transformIntoTheGreatWork(driver: GameTestDriver, you: EntityId): EntityId {
        val urabrask = driver.putCreatureOnBattlefield(you, "Urabrask")
        repeat(3) { castFromHand(driver, you, "Urabrask Test Instant") }
        driver.giveMana(you, Color.RED, 1)
        driver.submit(
            ActivateAbility(playerId = you, sourceId = urabrask, abilityId = Urabrask.activatedAbilities.first().id)
        ).outcome shouldBe Outcome.Done
        resolveStack(driver)
        return urabrask
    }

    test("casting an instant or sorcery deals 1 damage to target opponent and adds {R}") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        driver.putCreatureOnBattlefield(you, "Urabrask")

        val oppLife = driver.getLifeTotal(opp)
        val instantCard = driver.putCardInHand(you, "Urabrask Test Instant")
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, instantCard).outcome shouldBe Outcome.Done
        answerTargets(driver)
        // Resolve only the trigger, which sits above the instant.
        driver.bothPass()

        driver.getLifeTotal(opp) shouldBe oppLife - 1
        withClue("the trigger's Add {R} is not a mana ability — it lands on resolution") {
            redInPool(driver, you) shouldBe 1
        }
    }

    test("the transform ability needs three instant/sorcery spells cast this turn") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val urabrask = driver.putCreatureOnBattlefield(you, "Urabrask")
        val abilityId = Urabrask.activatedAbilities.first().id
        repeat(2) { castFromHand(driver, you, "Urabrask Test Instant") }

        driver.giveMana(you, Color.RED, 1)
        driver.submit(ActivateAbility(playerId = you, sourceId = urabrask, abilityId = abilityId))
            .outcome shouldNotBe Outcome.Done

        castFromHand(driver, you, "Urabrask Test Sorcery")
        driver.giveMana(you, Color.RED, 1)
        driver.submit(ActivateAbility(playerId = you, sourceId = urabrask, abilityId = abilityId))
            .outcome shouldBe Outcome.Done
        resolveStack(driver)
        nameOf(driver, urabrask) shouldBe "The Great Work"
    }

    test("chapter I deals 3 damage to the target opponent and each creature they control") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        val theirBear = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val yourBear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        val oppLifeBefore = driver.getLifeTotal(opp)
        val saga = transformIntoTheGreatWork(driver, you)

        nameOf(driver, saga) shouldBe "The Great Work"
        driver.state.getEntity(saga)!!.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 1
        // 3 cast-trigger pings + 3 from chapter I.
        driver.getLifeTotal(opp) shouldBe oppLifeBefore - 6
        driver.findPermanent(opp, "Grizzly Bears") shouldBe null
        withClue("only the target opponent's creatures are hit") {
            driver.state.getBattlefield().contains(yourBear) shouldBe true
        }
        driver.state.getBattlefield().contains(theirBear) shouldBe false
    }

    test("chapter III: cast instants and sorceries from any graveyard this turn, exiled instead") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        val saga = transformIntoTheGreatWork(driver, you)

        advanceToNextTurnMain(driver) // opponent's turn
        advanceToNextTurnMain(driver) // lore 2 — three Treasures
        driver.state.getEntity(saga)!!.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 2
        driver.state.getBattlefield(you).count { nameOf(driver, it) == "Treasure" } shouldBe 3

        val theirSorcery = driver.putCardInGraveyard(opp, "Urabrask Test Sorcery")
        val yourInstant = driver.putCardInGraveyard(you, "Urabrask Test Instant")
        val theirCreature = driver.putCardInGraveyard(opp, "Grizzly Bears")

        advanceToNextTurnMain(driver) // opponent's turn
        advanceToNextTurnMain(driver) // lore 3 — chapter III, then back to Urabrask

        withClue("chapter III exiled the Saga and returned it front face up") {
            nameOf(driver, saga) shouldBe "Urabrask"
        }

        // The grant outlives the Saga: a sorcery from the *opponent's* graveyard at sorcery speed.
        val lifeBefore = driver.getLifeTotal(you)
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, theirSorcery).outcome shouldBe Outcome.Done
        resolveStack(driver)
        driver.getLifeTotal(you) shouldBe lifeBefore + 3
        withClue("exiled instead of going to its owner's graveyard") {
            driver.getGraveyard(opp).contains(theirSorcery) shouldBe false
            driver.getExile(opp).contains(theirSorcery) shouldBe true
        }

        // And an instant from your own graveyard.
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, yourInstant).outcome shouldBe Outcome.Done
        resolveStack(driver)
        driver.getExile(you).contains(yourInstant) shouldBe true

        withClue("only instants and sorceries — a creature card stays put") {
            driver.giveColorlessMana(you, 2)
            driver.castSpell(you, theirCreature).outcome shouldNotBe Outcome.Done
            driver.getGraveyard(opp).contains(theirCreature) shouldBe true
        }
    }

    test("the chapter III permission ends at end of turn") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        transformIntoTheGreatWork(driver, you)
        repeat(4) { advanceToNextTurnMain(driver) } // through chapter III on your turn

        advanceToNextTurnMain(driver) // opponent's turn
        advanceToNextTurnMain(driver) // your next turn
        val theirInstant = driver.putCardInGraveyard(opp, "Urabrask Test Instant")
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, theirInstant).outcome shouldNotBe Outcome.Done
        driver.getGraveyard(opp).contains(theirInstant) shouldBe true
    }
})
