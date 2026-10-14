package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DonorCards
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfCards
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `DonorCards.ALL_EXILE` — the "all cards in exile" pool of `HasAllActivatedAbilitiesOfCards`
 * (Rex, Cyber-Hound). Unlike `LINKED_EXILE` it sees every card in exile whoever exiled it, it is
 * re-read live (a card leaving exile or losing its counter stops donating), a `withCounter` card
 * filter reads the exiled card's own counters, and a face-down exiled card donates nothing because
 * it has no characteristics (CR 406.3a).
 */
class AllExileDonorPoolTest : FunSpec({

    val relic = card("Exile Pool Test Relic") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{T}: You gain 2 life."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(2)
        }
    }
    val relicAbilityId = relic.activatedAbilities[0].id

    val brainHound = card("Exile Pool Test Hound") {
        manaCost = "{2}"
        typeLine = "Artifact Creature — Dog"
        power = 2
        toughness = 2
        oracleText = "This creature has all activated abilities of all cards in exile with brain counters on them."
        staticAbility {
            ability = HasAllActivatedAbilitiesOfCards(
                donors = DonorCards.ALL_EXILE,
                cardFilter = GameObjectFilter.Any.withCounter(CounterType.BRAIN)
            )
        }
    }

    val vault = card("Exile Pool Test Vault") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "This artifact has all activated abilities of all cards in exile."
        staticAbility { ability = HasAllActivatedAbilitiesOfCards(donors = DonorCards.ALL_EXILE) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(relic, brainHound, vault))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.addBrainCounter(card: EntityId) = replaceState(
        state.updateEntity(card) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.BRAIN, 1))
        }
    )

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && guard++ < 20) {
            driver.bothPass()
        }
    }

    test("a brain-countered card in an opponent's exile lends its ability, and {T} taps the receiver") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val hound = driver.putPermanentOnBattlefield(you, "Exile Pool Test Hound")
        driver.addBrainCounter(driver.putCardInExile(opponent, "Exile Pool Test Relic"))

        val lifeBefore = driver.getLifeTotal(you)
        driver.submitSuccess(ActivateAbility(playerId = you, sourceId = hound, abilityId = relicAbilityId))
        resolveStack(driver)

        driver.getLifeTotal(you) shouldBe lifeBefore + 2
        driver.state.getEntity(hound)?.has<TappedComponent>() shouldBe true
    }

    test("an exiled card without the filtered counter lends nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val hound = driver.putPermanentOnBattlefield(you, "Exile Pool Test Hound")
        driver.putCardInExile(you, "Exile Pool Test Relic")

        driver.submitExpectFailure(ActivateAbility(playerId = you, sourceId = hound, abilityId = relicAbilityId))
    }

    test("the pool is live: losing the counter or leaving exile stops the grant") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val hound = driver.putPermanentOnBattlefield(you, "Exile Pool Test Hound")
        val exiled = driver.putCardInExile(you, "Exile Pool Test Relic")
        driver.addBrainCounter(exiled)

        driver.replaceState(
            driver.state.updateEntity(exiled) { c ->
                c.with(c.get<CountersComponent>()!!.withRemoved(CounterType.BRAIN, 1))
            }
        )
        driver.submitExpectFailure(ActivateAbility(playerId = you, sourceId = hound, abilityId = relicAbilityId))

        driver.addBrainCounter(exiled)
        driver.moveToGraveyard(exiled)
        driver.submitExpectFailure(ActivateAbility(playerId = you, sourceId = hound, abilityId = relicAbilityId))
    }

    test("a face-down card in exile donates nothing, even with a brain counter") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val hound = driver.putPermanentOnBattlefield(you, "Exile Pool Test Hound")
        val exiled = driver.putCardInExile(you, "Exile Pool Test Relic")
        driver.addBrainCounter(exiled)
        driver.replaceState(driver.state.updateEntity(exiled) { it.with(FaceDownComponent) })

        driver.submitExpectFailure(ActivateAbility(playerId = you, sourceId = hound, abilityId = relicAbilityId))
    }

    test("unfiltered, every card in either player's exile donates") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val vaultId = driver.putPermanentOnBattlefield(you, "Exile Pool Test Vault")
        driver.submitExpectFailure(ActivateAbility(playerId = you, sourceId = vaultId, abilityId = relicAbilityId))

        driver.putCardInExile(opponent, "Exile Pool Test Relic")
        val lifeBefore = driver.getLifeTotal(you)
        driver.submitSuccess(ActivateAbility(playerId = you, sourceId = vaultId, abilityId = relicAbilityId))
        resolveStack(driver)

        driver.getLifeTotal(you) shouldBe lifeBefore + 2
    }
})
