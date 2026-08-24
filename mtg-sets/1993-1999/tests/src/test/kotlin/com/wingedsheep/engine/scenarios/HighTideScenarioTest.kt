package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.IntrinsicManaAbilities
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.HighTide
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for High Tide (Fallen Empires).
 *
 * High Tide: {U}
 * Instant
 * Until end of turn, whenever a player taps an Island for mana, that player adds an additional {U}.
 *
 * The properties worth pinning are that it is open-ended and filtered: an Island that arrives
 * *after* the spell resolved is doubled too, and a non-Island is not. Both follow from the effect
 * being one filtered static rather than a set of per-Island grants — which is what needed the
 * engine to read granted mana statics at all, a spell having no permanent to carry one.
 */
class HighTideScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HighTide)
        return driver
    }

    fun tapForMana(driver: GameTestDriver, playerId: EntityId, landId: EntityId) {
        val ability = IntrinsicManaAbilities
            .forEntity(driver.state, projector.project(driver.state), landId)
            .first()
        driver.submitSuccess(
            ActivateAbility(playerId = playerId, sourceId = landId, abilityId = ability.id)
        )
    }

    fun pool(driver: GameTestDriver, playerId: EntityId): ManaPoolComponent? =
        driver.state.getEntity(playerId)?.get<ManaPoolComponent>()

    test("an Island that arrives after High Tide resolved is doubled too") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(alice, com.wingedsheep.sdk.core.Color.BLUE, 1)
        driver.castSpell(alice, driver.putCardInHand(alice, "High Tide"))
        driver.bothPass()

        // Put the Island onto the battlefield only *after* the spell resolved: the filter is
        // re-read on every tap, so this one is covered as well.
        val island = driver.putLandOnBattlefield(alice, "Island")
        tapForMana(driver, alice, island)

        withClue("one Island tapped for {U} produced {U}{U}") {
            pool(driver, alice)?.blue shouldBe 2
        }
    }

    test("a non-Island is untouched") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(alice, com.wingedsheep.sdk.core.Color.BLUE, 1)
        driver.castSpell(alice, driver.putCardInHand(alice, "High Tide"))
        driver.bothPass()

        val forest = driver.putLandOnBattlefield(alice, "Forest")
        tapForMana(driver, alice, forest)

        withClue("the Forest produced a single {G} and no blue") {
            pool(driver, alice)?.green shouldBe 1
            pool(driver, alice)?.blue shouldBe 0
        }
    }

    test("an opponent's Island is doubled — the effect is symmetrical") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(alice, com.wingedsheep.sdk.core.Color.BLUE, 1)
        driver.castSpell(alice, driver.putCardInHand(alice, "High Tide"))
        driver.bothPass()

        val island = driver.putLandOnBattlefield(bob, "Island")
        // Bob gets priority once Alice passes with an empty stack.
        driver.passPriority(alice)
        tapForMana(driver, bob, island)

        withClue("Bob taps his own Island and gets the extra blue") {
            pool(driver, bob)?.blue shouldBe 2
        }
    }
})
