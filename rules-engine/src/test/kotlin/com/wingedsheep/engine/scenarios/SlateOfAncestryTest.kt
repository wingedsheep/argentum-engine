package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Slate of Ancestry.
 *
 * Slate of Ancestry
 * {4}
 * Artifact
 * {4}, {T}, Discard your hand: Draw a card for each creature you control.
 */
class SlateOfAncestryTest : FunSpec({

    val SlateOfAncestry = card("Slate of Ancestry") {
        manaCost = "{4}"
        typeLine = "Artifact"
        oracleText = "{4}, {T}, Discard your hand: Draw a card for each creature you control."

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{4}"),
                Costs.Tap,
                Costs.DiscardHand
            )
            effect = DrawCardsEffect(
                count = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature),
                target = EffectTarget.Controller
            )
        }
    }

    val abilityId = SlateOfAncestry.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SlateOfAncestry)
        return driver
    }

    test("draw cards equal to number of creatures controlled") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 30, "Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Slate of Ancestry on the battlefield
        val slate = driver.putPermanentOnBattlefield(activePlayer, "Slate of Ancestry")

        // Put 3 creatures on the battlefield
        driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")

        // Remember hand size before activation (7 from initial draw)
        val handSizeBefore = driver.getHandSize(activePlayer)

        // Give mana to activate ({4} activation cost)
        driver.giveMana(activePlayer, Color.GREEN, 4)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = slate,
                abilityId = abilityId
            )
        )
        result.isSuccess shouldBe true

        // Let the ability resolve
        driver.bothPass()

        // Hand was discarded entirely, then drew 3 (one per creature)
        driver.getHandSize(activePlayer) shouldBe 3
    }

    test("draw zero cards when no creatures controlled") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 30, "Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val slate = driver.putPermanentOnBattlefield(activePlayer, "Slate of Ancestry")

        // No creatures on the battlefield
        driver.giveMana(activePlayer, Color.GREEN, 4)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = slate,
                abilityId = abilityId
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Hand was discarded and drew 0 (no creatures)
        driver.getHandSize(activePlayer) shouldBe 0
    }

    test("discarded cards go to graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 30, "Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val slate = driver.putPermanentOnBattlefield(activePlayer, "Slate of Ancestry")
        driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val handSizeBefore = driver.getHandSize(activePlayer)
        val graveyardSizeBefore = driver.getGraveyard(activePlayer).size

        driver.giveMana(activePlayer, Color.GREEN, 4)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = slate,
                abilityId = abilityId
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // All cards from hand should be in graveyard now
        driver.getGraveyard(activePlayer).size shouldBe graveyardSizeBefore + handSizeBefore

        // Drew 1 card (one creature)
        driver.getHandSize(activePlayer) shouldBe 1
    }

    test("cannot activate without enough mana") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 30, "Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val slate = driver.putPermanentOnBattlefield(activePlayer, "Slate of Ancestry")

        // Only give 2 mana (need 4)
        driver.giveMana(activePlayer, Color.GREEN, 2)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = slate,
                abilityId = abilityId
            )
        )
        result.isSuccess shouldBe false
    }

    test("cannot activate when already tapped") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 30, "Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val slate = driver.putPermanentOnBattlefield(activePlayer, "Slate of Ancestry")
        driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // First activation should succeed
        driver.giveMana(activePlayer, Color.GREEN, 4)
        val result1 = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = slate,
                abilityId = abilityId
            )
        )
        result1.isSuccess shouldBe true
        driver.bothPass()

        // Second activation should fail (slate is tapped)
        driver.giveMana(activePlayer, Color.GREEN, 4)
        val result2 = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = slate,
                abilityId = abilityId
            )
        )
        result2.isSuccess shouldBe false
    }
})
