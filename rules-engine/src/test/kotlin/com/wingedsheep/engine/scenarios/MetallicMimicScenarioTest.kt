package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.MetallicMimic
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val projector = StateProjector()

/**
 * Metallic Mimic (AER #164) — {2} 2/1 Artifact Creature — Shapeshifter
 *
 * "As this creature enters, choose a creature type.
 *  This creature is the chosen type in addition to its other types.
 *  Each other creature you control of the chosen type enters with an additional +1/+1 counter on it."
 *
 * Exercises the counter-granting replacement through a *real* cast, since the entering-permanent
 * replacement path is what decides whether the chosen type is read off the Mimic (correct) or off
 * the entering creature.
 */
class MetallicMimicScenarioTest : FunSpec({

    val goblin = CardDefinition.creature(
        name = "Test Goblin",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 1,
        toughness = 1
    )

    val bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MetallicMimic, goblin, bear))
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /**
     * Cast [cardName] from hand off injected mana and let it resolve. Casting for real (rather than
     * `putCreatureOnBattlefield`) is the point: only the resolution path runs the enters-with
     * replacements this card is about.
     */
    fun castAndResolve(driver: GameTestDriver, playerId: EntityId, cardName: String): EntityId {
        val cardId = driver.putCardInHand(playerId, cardName)
        driver.giveColorlessMana(playerId, 2)
        driver.submitSuccess(
            com.wingedsheep.engine.core.CastSpell(
                playerId = playerId,
                cardId = cardId,
                paymentStrategy = com.wingedsheep.engine.core.PaymentStrategy.FromPool,
            )
        )
        driver.bothPass()
        return cardId
    }

    /** Answer the "choose a creature type" entry choice with [type]. */
    fun chooseCreatureType(driver: GameTestDriver, playerId: EntityId, type: String) {
        val decision = driver.pendingDecision as ChooseOptionDecision
        val index = decision.options.indexOfFirst { it.equals(type, ignoreCase = true) }
        require(index >= 0) { "No option '$type' among ${decision.options.take(20)}…" }
        driver.submitDecision(playerId, OptionChosenResponse(decision.id, index))
    }

    test("other creatures you control of the chosen type enter with an extra +1/+1 counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mimic = castAndResolve(driver, you, "Metallic Mimic")
        chooseCreatureType(driver, you, "Goblin")

        // The Mimic is the chosen type in addition to Shapeshifter…
        val projected = projector.project(driver.state)
        projected.hasSubtype(mimic, "Goblin") shouldBe true
        projected.hasSubtype(mimic, "Shapeshifter") shouldBe true
        // …and "each OTHER creature" excludes itself: no counter of its own.
        plusOneCounters(driver, mimic) shouldBe 0
        projected.getPower(mimic) shouldBe 2
        projected.getToughness(mimic) shouldBe 1

        // A Goblin entering afterwards gets the counter.
        val goblinId = castAndResolve(driver, you, "Test Goblin")
        plusOneCounters(driver, goblinId) shouldBe 1
        projector.project(driver.state).getPower(goblinId) shouldBe 2

        // A creature of another type does not.
        val bearId = castAndResolve(driver, you, "Test Bear")
        plusOneCounters(driver, bearId) shouldBe 0
        projector.project(driver.state).getPower(bearId) shouldBe 2
    }

    test("only creatures YOU control get the counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        castAndResolve(driver, you, "Metallic Mimic")
        chooseCreatureType(driver, you, "Goblin")

        // Advance to the opponent's turn so they can cast a creature at sorcery speed. Their Goblin
        // gets no counter — it isn't a creature the Mimic's controller controls.
        while (driver.activePlayer != opponent) {
            driver.passPriorityUntil(Step.END)
            driver.bothPass()
        }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val theirGoblin = castAndResolve(driver, opponent, "Test Goblin")

        plusOneCounters(driver, theirGoblin) shouldBe 0
    }
})
