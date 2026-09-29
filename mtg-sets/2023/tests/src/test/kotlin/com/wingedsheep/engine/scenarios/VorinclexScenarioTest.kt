package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.Vorinclex
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vorinclex // The Grand Evolution (MOM #213).
 *
 * Pins the front face's up-to-two Forest search, the sorcery-speed transform, chapter I picking
 * creature cards only from among the ten milled cards, chapter II's player-chosen uneven split of
 * seven +1/+1 counters, and chapter III's fight grant — locked to the creatures you control as it
 * resolves, so Vorinclex, back on the battlefield afterwards, doesn't get it.
 */
class VorinclexScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PredefinedTokens.allTokens + listOf(Vorinclex))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun nameOf(driver: GameTestDriver, id: EntityId): String =
        driver.state.getEntity(id)!!.get<CardComponent>()!!.name

    fun plusOnes(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun lore(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.LORE)

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && (driver.state.stack.isNotEmpty() || driver.isPaused)) {
            if (driver.isPaused) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    fun toNextTurnMain(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
    }

    fun activateTransform(driver: GameTestDriver, you: EntityId, vorinclex: EntityId): Outcome {
        driver.giveMana(you, Color.GREEN, 2)
        driver.giveColorlessMana(you, 6)
        return driver.submit(
            ActivateAbility(playerId = you, sourceId = vorinclex, abilityId = Vorinclex.activatedAbilities.first().id)
        ).outcome
    }

    test("has reach and trample; entering searches for up to two Forest cards into hand") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val handBefore = driver.getHand(you).size

        val card = driver.putCardInHand(you, "Vorinclex")
        driver.giveMana(you, Color.GREEN, 2)
        driver.giveColorlessMana(you, 3)
        driver.castSpell(you, card).outcome shouldBe Outcome.Done
        driver.bothPass() // creature spell resolves; ETB trigger goes on the stack
        driver.bothPass() // trigger resolves into the search

        val search = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        search.maxSelections shouldBe 2
        driver.submitCardSelection(you, search.options.take(2))
        resolveStack(driver)

        val vorinclex = driver.findPermanent(you, "Vorinclex")!!
        driver.state.projectedState.hasKeyword(vorinclex, Keyword.REACH) shouldBe true
        driver.state.projectedState.hasKeyword(vorinclex, Keyword.TRAMPLE) shouldBe true
        driver.getHand(you).size shouldBe handBefore + 2
        driver.getHand(you).map { nameOf(driver, it) }.all { it == "Forest" } shouldBe true
    }

    test("the transform is sorcery-speed only") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        val vorinclex = driver.putCreatureOnBattlefield(you, "Vorinclex")

        // On the opponent's turn it can't be activated.
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        driver.activePlayer shouldBe opp
        driver.giveMana(you, Color.GREEN, 2)
        driver.giveColorlessMana(you, 6)
        driver.submit(
            ActivateAbility(playerId = you, sourceId = vorinclex, abilityId = Vorinclex.activatedAbilities.first().id)
        ).outcome shouldNotBe Outcome.Done
        nameOf(driver, vorinclex) shouldBe "Vorinclex"
    }

    test("the Saga: creatures from the milled ten, an uneven split of seven counters, then fights") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        val vorinclex = driver.putCreatureOnBattlefield(you, "Vorinclex")
        val oppGiant = driver.putCreatureOnBattlefield(opp, "Hill Giant")

        // A creature card already in the graveyard is not "among the milled cards".
        val oldBears = driver.putCardInGraveyard(you, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(you, "Savannah Lions")
        driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(you, "Centaur Courser")
        val graveyardBefore = driver.getGraveyard(you).size

        // Chapter I.
        activateTransform(driver, you, vorinclex) shouldBe Outcome.Done
        driver.bothPass() // transform ability resolves; chapter I triggers
        driver.bothPass() // chapter I resolves into the mill + choice
        nameOf(driver, vorinclex) shouldBe "The Grand Evolution"
        lore(driver, vorinclex) shouldBe 1

        val pick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.maxSelections shouldBe 2
        withClue("only creature cards from among the milled cards are eligible") {
            pick.options.contains(oldBears) shouldBe false
            pick.options.map { nameOf(driver, it) } shouldContainExactlyInAnyOrder
                listOf("Centaur Courser", "Grizzly Bears", "Savannah Lions")
        }
        val courser = pick.options.single { nameOf(driver, it) == "Centaur Courser" }
        val lions = pick.options.single { nameOf(driver, it) == "Savannah Lions" }
        driver.submitCardSelection(you, listOf(courser, lions))
        resolveStack(driver)

        driver.state.getBattlefield(you).contains(courser) shouldBe true
        driver.state.getBattlefield(you).contains(lions) shouldBe true
        driver.getGraveyard(you).size shouldBe graveyardBefore + 8

        // Chapter II: two targets, split 5/2.
        toNextTurnMain(driver) // opponent's turn
        toNextTurnMain(driver) // your turn: lore 2
        lore(driver, vorinclex) shouldBe 2
        var guard = 0
        while (guard++ < 20 && plusOnes(driver, courser) == 0) {
            when (val decision = driver.pendingDecision) {
                is ChooseTargetsDecision -> {
                    val req = decision.targetRequirements.single()
                    req.maxTargets shouldBe 7
                    withClue("only creatures you control") {
                        decision.legalTargets[req.index].orEmpty().contains(oppGiant) shouldBe false
                    }
                    driver.submitDecision(
                        decision.playerId,
                        TargetsResponse(decision.id, mapOf(req.index to listOf(courser, lions)))
                    )
                }
                is DistributeDecision -> {
                    decision.totalAmount shouldBe 7
                    decision.minPerTarget shouldBe 1
                    driver.submitDecision(
                        decision.playerId,
                        DistributionResponse(decision.id, mapOf(courser to 5, lions to 2))
                    )
                }
                null -> driver.bothPass()
                else -> driver.autoResolveDecision()
            }
        }
        plusOnes(driver, courser) shouldBe 5
        plusOnes(driver, lions) shouldBe 2

        // Chapter III: the fight grant, then back to Vorinclex.
        toNextTurnMain(driver) // opponent's turn
        toNextTurnMain(driver) // your turn: lore 3
        resolveStack(driver)
        withClue("chapter III exiled the Saga and returned it front face up") {
            nameOf(driver, vorinclex) shouldBe "Vorinclex"
            driver.state.getBattlefield(you).contains(vorinclex) shouldBe true
        }
        withClue("Vorinclex wasn't on the battlefield when the grant resolved") {
            driver.state.grantedActivatedAbilities.none { it.entityId == vorinclex } shouldBe true
        }
        val grant = driver.state.grantedActivatedAbilities.single { it.entityId == courser }
        driver.state.grantedActivatedAbilities.any { it.entityId == lions } shouldBe true
        driver.state.grantedActivatedAbilities.none { it.entityId == oppGiant } shouldBe true

        driver.giveColorlessMana(you, 1)
        driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = courser,
                abilityId = grant.ability.id,
                targets = listOf(ChosenTarget.Permanent(oppGiant))
            )
        ).outcome shouldBe Outcome.Done
        resolveStack(driver)

        withClue("an 8/8 Courser fights the 3/3 Hill Giant") {
            driver.state.getBattlefield().contains(oppGiant) shouldBe false
            driver.state.getBattlefield().contains(courser) shouldBe true
        }
    }
})
