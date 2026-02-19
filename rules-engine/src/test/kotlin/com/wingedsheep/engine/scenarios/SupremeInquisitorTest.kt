package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.EffectPatterns
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Supreme Inquisitor.
 *
 * Supreme Inquisitor: {3}{U}{U}
 * Creature — Human Wizard
 * 1/3
 * Tap five untapped Wizards you control: Search target player's library for up to five cards
 * and exile them. Then that player shuffles.
 */
class SupremeInquisitorTest : FunSpec({

    val SupremeInquisitor = card("Supreme Inquisitor") {
        manaCost = "{3}{U}{U}"
        typeLine = "Creature — Human Wizard"
        power = 1
        toughness = 3
        oracleText = "Tap five untapped Wizards you control: Search target player's library for up to five cards and exile them. Then that player shuffles."

        activatedAbility {
            cost = Costs.TapPermanents(5, GameObjectFilter.Creature.withSubtype("Wizard"))
            target = Targets.Player
            effect = EffectPatterns.searchTargetLibraryExile(5)
        }
    }

    val abilityId = SupremeInquisitor.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SupremeInquisitor))
        return driver
    }

    fun GameTestDriver.putWizard(playerId: EntityId): EntityId {
        val id = putCreatureOnBattlefield(playerId, "Supreme Inquisitor")
        removeSummoningSickness(id)
        return id
    }

    test("tapping five wizards searches opponent library and exiles selected cards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 5 Supreme Inquisitors (they are Wizards) on battlefield
        val wizards = (1..5).map { driver.putWizard(activePlayer) }

        // Put known cards on top of opponent's library to exile
        val targetCard1 = driver.putCardOnTopOfLibrary(opponent, "Grizzly Bears")
        val targetCard2 = driver.putCardOnTopOfLibrary(opponent, "Grizzly Bears")

        // Activate the ability targeting opponent, paying the tap cost with all 5 wizards
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wizards[0],
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(tappedPermanents = wizards)
            )
        )
        result.isSuccess shouldBe true

        // All 5 wizards should be tapped
        wizards.forEach { driver.isTapped(it) shouldBe true }

        // Both pass to resolve the ability
        driver.bothPass()

        // Should have a search library decision
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.maxSelections shouldBe 5

        // Select 2 cards to exile
        val toExile = listOf(targetCard1, targetCard2)
        driver.submitDecision(
            activePlayer,
            CardsSelectedResponse(decision.id, toExile)
        )

        // The selected cards should be in exile
        val exileZone = ZoneKey(opponent, Zone.EXILE)
        val exile = driver.state.getZone(exileZone)
        exile.contains(targetCard1) shouldBe true
        exile.contains(targetCard2) shouldBe true

        // The cards should NOT be in the library anymore
        val libraryZone = ZoneKey(opponent, Zone.LIBRARY)
        val library = driver.state.getZone(libraryZone)
        library.contains(targetCard1) shouldBe false
        library.contains(targetCard2) shouldBe false
    }

    test("can choose zero cards (fail to find)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wizards = (1..5).map { driver.putWizard(activePlayer) }

        val opponentLibraryBefore = driver.state.getZone(ZoneKey(opponent, Zone.LIBRARY)).size

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wizards[0],
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(tappedPermanents = wizards)
            )
        )

        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.minSelections shouldBe 0

        // Select zero cards
        driver.submitDecision(
            activePlayer,
            CardsSelectedResponse(decision.id, emptyList())
        )

        // Exile should be empty
        val exileZone = ZoneKey(opponent, Zone.EXILE)
        driver.state.getZone(exileZone).size shouldBe 0

        // Library should still have cards (just shuffled)
        val opponentLibraryAfter = driver.state.getZone(ZoneKey(opponent, Zone.LIBRARY)).size
        opponentLibraryAfter shouldBe opponentLibraryBefore
    }

    test("cannot activate with fewer than five wizards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Only put 4 wizards on battlefield
        val wizards = (1..4).map { driver.putWizard(activePlayer) }

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wizards[0],
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(tappedPermanents = wizards)
            )
        )
        result.isSuccess shouldBe false
    }
})
