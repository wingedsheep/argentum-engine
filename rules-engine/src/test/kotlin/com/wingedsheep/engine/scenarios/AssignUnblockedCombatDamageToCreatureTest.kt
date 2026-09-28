package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AssignUnblockedCombatDamageToDefendingCreature
import com.wingedsheep.sdk.scripting.costs.PayCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Engine contract for [AssignUnblockedCombatDamageToDefendingCreature] (Cunning Giant): an
 * unblocked attacker's controller may send all of its combat damage to one creature the defending
 * player controls. The choice is made per combat damage step (CR 510.1), and a face-down creature
 * has no abilities (CR 708.2a).
 */
class AssignUnblockedCombatDamageToCreatureTest : FunSpec({

    val sneak = CardDefinition.creature(
        name = "Test Flanker",
        manaCost = ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype("Giant")),
        power = 3,
        toughness = 3,
        script = CardScript(staticAbilities = listOf(AssignUnblockedCombatDamageToDefendingCreature())),
    )
    val doubleStriker = CardDefinition.creature(
        name = "Test Double Flanker",
        manaCost = ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype("Giant")),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.DOUBLE_STRIKE),
        script = CardScript(staticAbilities = listOf(AssignUnblockedCombatDamageToDefendingCreature())),
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(sneak, doubleStriker))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.attackUnblocked(attacker: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(attacker), player2)
        bothPass()
        declareNoBlockers(player2)
        bothPass()
    }

    test("the chosen creature is dealt all the damage and the player none") {
        val driver = createDriver()
        val attacker = driver.putCreatureOnBattlefield(driver.player1, "Test Flanker")
        driver.removeSummoningSickness(attacker)
        val bears = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        driver.attackUnblocked(attacker)
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldBe listOf(bears)
        driver.submitCardSelection(driver.player1, listOf(bears))

        driver.getGraveyardCardNames(driver.player2) shouldContain "Grizzly Bears"
        driver.getLifeTotal(driver.player2) shouldBe 20
    }

    test("a face-down creature has no abilities, so it is never asked (CR 708.2a)") {
        val driver = createDriver()
        val attacker = driver.putCreatureOnBattlefield(driver.player1, "Test Flanker")
        driver.replaceState(driver.state.updateEntity(attacker) { container ->
            val defId = container.get<CardComponent>()?.cardDefinitionId ?: ""
            container.with(FaceDownComponent).with(MorphDataComponent(PayCost.OwnManaCost, defId))
        })
        driver.removeSummoningSickness(attacker)
        driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        driver.attackUnblocked(attacker)

        driver.pendingDecision.shouldBeNull()
        driver.getLifeTotal(driver.player2) shouldBe 18
    }

    test("double strike asks again in the regular damage step (CR 510.1 — each step is a fresh assignment)") {
        val driver = createDriver()
        val attacker = driver.putCreatureOnBattlefield(driver.player1, "Test Double Flanker")
        driver.removeSummoningSickness(attacker)
        val bears = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        driver.attackUnblocked(attacker)
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(driver.player1, listOf(bears))
        driver.getGraveyardCardNames(driver.player2) shouldContain "Grizzly Bears"
        driver.getLifeTotal(driver.player2) shouldBe 20

        // No creature left to choose in the regular step, so the damage goes to the player.
        driver.bothPass()
        driver.pendingDecision.shouldBeNull()
        driver.getLifeTotal(driver.player2) shouldBe 18
    }

    test("declining in the first-strike step still offers the choice in the regular step") {
        val driver = createDriver()
        val attacker = driver.putCreatureOnBattlefield(driver.player1, "Test Double Flanker")
        driver.removeSummoningSickness(attacker)
        val bears = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        driver.attackUnblocked(attacker)
        driver.submitCardSelection(driver.player1, emptyList())
        driver.getLifeTotal(driver.player2) shouldBe 18

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(driver.player1, listOf(bears))
        driver.getGraveyardCardNames(driver.player2) shouldContain "Grizzly Bears"
        driver.getLifeTotal(driver.player2) shouldBe 18
        driver.getGraveyardCardNames(driver.player1) shouldNotContain "Test Double Flanker"
    }
})
