package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dst.cards.VedalkenEngineer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Vedalken Engineer (DST #35) — {1}{U} Creature — Vedalken Artificer 1/1
 *
 *   {T}: Add two mana of any one color. Spend this mana only to cast artifact spells or activate
 *   abilities of artifacts.
 *
 *  1. Tapping adds two restricted mana of the one chosen colour.
 *  2. That mana pays for a two-pip artifact spell of the chosen colour.
 *  3. It can't pay for a non-artifact spell.
 */
class VedalkenEngineerScenarioTest : FunSpec({

    val testArtifact = CardDefinition.artifact(
        name = "Test Green Artifact",
        manaCost = ManaCost.parse("{G}{G}")
    )
    val testCreature = CardDefinition.creature(
        name = "Test Green Creature",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Human")),
        power = 1,
        toughness = 1
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + testArtifact + testCreature)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        return driver
    }

    val manaAbilityId = VedalkenEngineer.activatedAbilities[0].id

    fun GameTestDriver.tapForGreen(playerId: EntityId) {
        val engineer = putCreatureOnBattlefield(playerId, "Vedalken Engineer")
        removeSummoningSickness(engineer)
        submit(
            ActivateAbility(playerId, engineer, manaAbilityId, manaColorChoice = Color.GREEN)
        ).error shouldBe null
    }

    test("tapping adds two restricted mana of the chosen colour") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tapForGreen(p1)

        val pool = driver.state.getEntity(p1)?.get<ManaPoolComponent>()!!
        pool.restrictedMana.size shouldBe 2
        pool.restrictedMana.all { it.color == Color.GREEN } shouldBe true
    }

    test("the mana pays for an artifact spell") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tapForGreen(p1)

        val artifact = driver.putCardInHand(p1, "Test Green Artifact")
        val result = driver.submit(
            CastSpell(playerId = p1, cardId = artifact, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.outcome shouldBe Outcome.Done
    }

    test("the mana can't pay for a non-artifact spell") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tapForGreen(p1)

        val creature = driver.putCardInHand(p1, "Test Green Creature")
        val result = driver.submit(
            CastSpell(playerId = p1, cardId = creature, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.outcome shouldNotBe Outcome.Done
    }
})
