package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.HallOfTheBanditLord
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Hall of the Bandit Lord — Champions of Kamigawa #277.
 *
 * Hall of the Bandit Lord enters tapped.
 * {T}, Pay 3 life: Add {C}. If that mana is spent on a creature spell, it gains haste.
 *
 * The haste "doesn't wear off at end of turn" (2004-12-01 ruling), which is what separates this
 * rider from Carnelian Orb of Dragonkind's end-of-turn one.
 */
class HallOfTheBanditLordScenarioTest : FunSpec({

    val projector = StateProjector()
    val hallAbilityId = HallOfTheBanditLord.activatedAbilities[0].id

    /** A {1} creature — one Hall activation pays its whole cost. */
    val bandit = CardDefinition.creature(
        name = "Test Hall Bandit",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Human")),
        power = 2,
        toughness = 2,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HallOfTheBanditLord, bandit))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        return driver
    }

    fun tapHall(driver: GameTestDriver, you: EntityId, hall: EntityId) {
        driver.submitSuccess(ActivateAbility(playerId = you, sourceId = hall, abilityId = hallAbilityId))
    }

    test("enters tapped") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val hall = driver.putCardInHand(you, "Hall of the Bandit Lord")
        driver.playLand(you, hall).error shouldBe null

        driver.state.getEntity(hall)?.has<TappedComponent>() shouldBe true
    }

    test("a creature paid with its mana has haste, attacks at once, and keeps haste past end of turn") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val hall = driver.putPermanentOnBattlefield(you, "Hall of the Bandit Lord")
        val creature = driver.putCardInHand(you, "Test Hall Bandit")
        tapHall(driver, you, hall)
        driver.getLifeTotal(you) shouldBe 17

        driver.castSpell(you, creature).error shouldBe null
        driver.bothPass()

        projector.project(driver.state).hasKeyword(creature, Keyword.HASTE) shouldBe true
        val turn = driver.state.turnNumber
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.state.turnNumber shouldBe turn
        driver.declareAttackers(you, listOf(creature), opponent).error shouldBe null

        // Into the opponent's turn: an end-of-turn grant would be gone by now.
        driver.passPriorityUntil(Step.UPKEEP)
        (driver.state.turnNumber > turn) shouldBe true
        projector.project(driver.state).hasKeyword(creature, Keyword.HASTE) shouldBe true
    }

    test("the same creature cast with ordinary mana has no haste") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCardInHand(you, "Test Hall Bandit")
        driver.giveMana(you, Color.BLUE, 1)
        driver.castSpell(you, creature).error shouldBe null
        driver.bothPass()

        projector.project(driver.state).hasKeyword(creature, Keyword.HASTE) shouldBe false
    }

    test("a countered creature spell carries no haste grant into the graveyard") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val hall = driver.putPermanentOnBattlefield(you, "Hall of the Bandit Lord")
        val creature = driver.putCardInHand(you, "Test Hall Bandit")
        val counter = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(opponent, Color.BLUE, 2)
        tapHall(driver, you, hall)

        driver.castSpell(you, creature).error shouldBe null
        driver.passPriority(you)
        driver.submit(
            CastSpell(
                opponent, counter,
                targets = listOf(ChosenTarget.Spell(creature)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.bothPass()

        driver.getGraveyardCardNames(you).contains("Test Hall Bandit") shouldBe true
        // The card keeps its entity id in the graveyard, so a grant left floating on it would
        // reach it if it were later put onto the battlefield (CR 400.7 says it must not).
        driver.state.floatingEffects.none { creature in it.effect.affectedEntities } shouldBe true
    }
})
