package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

/**
 * Tests for Quicksilver Dragon (ONS)
 * {4}{U}{U}
 * Creature — Dragon
 * 5/5
 * Flying
 * {U}: If target spell has only one target and that target is this creature,
 *       change that spell's target to another creature.
 * Morph {4}{U}
 */
class QuicksilverDragonTest : FunSpec({

    val abilityId = AbilityId(UUID.randomUUID().toString())

    val QuicksilverDragon = CardDefinition(
        name = "Quicksilver Dragon",
        manaCost = ManaCost.parse("{4}{U}{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Dragon"))),
        oracleText = "Flying\n{U}: If target spell has only one target and that target is this creature, change that spell's target to another creature.\nMorph {4}{U}",
        creatureStats = CreatureStats(5, 5),
        keywords = setOf(Keyword.FLYING),
        keywordAbilities = listOf(KeywordAbility.Morph(ManaCost.parse("{4}{U}"))),
        script = CardScript.permanent(
            ActivatedAbility(
                id = abilityId,
                cost = AbilityCost.Mana(ManaCost.parse("{U}")),
                effect = Effects.ChangeSpellTarget(targetMustBeSource = true),
                targetRequirement = Targets.Spell
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(QuicksilverDragon))
        return driver
    }

    test("redirects a spell that targets the Dragon to another creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Setup: Dragon and another creature on battlefield
        val dragon = driver.putCreatureOnBattlefield(activePlayer, "Quicksilver Dragon")
        driver.removeSummoningSickness(dragon)
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Opponent casts Lightning Bolt targeting the Dragon
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)

        driver.passPriority(activePlayer)

        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = bolt,
                targets = listOf(ChosenTarget.Permanent(dragon)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        val boltOnStack = driver.getTopOfStack()!!
        driver.stackSize shouldBe 1

        // Opponent passes priority
        driver.passPriority(opponent)

        // Dragon's controller activates the redirect ability targeting the bolt
        driver.giveMana(activePlayer, Color.BLUE, 1)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = dragon,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Spell(boltOnStack))
            )
        )

        driver.stackSize shouldBe 2

        // Both pass — ability resolves, presents choice of new target
        driver.bothPass()

        // Dragon's controller chooses the bear as new target
        driver.submitCardSelection(activePlayer, listOf(bear))

        // Stack should now have just the bolt with changed target
        driver.stackSize shouldBe 1

        // Both pass — bolt resolves, dealing damage to the bear instead
        driver.bothPass()

        driver.stackSize shouldBe 0

        // Dragon should still be alive
        driver.findPermanent(activePlayer, "Quicksilver Dragon") shouldNotBe null

        // Grizzly Bears (2/2) took 3 damage and should be dead
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.assertInGraveyard(opponent, "Grizzly Bears")
    }

    test("does nothing if spell targets a different creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Setup: Dragon and another creature both controlled by active player
        val dragon = driver.putCreatureOnBattlefield(activePlayer, "Quicksilver Dragon")
        driver.removeSummoningSickness(dragon)
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Opponent casts Lightning Bolt targeting the bear (NOT the Dragon)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)

        driver.passPriority(activePlayer)

        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = bolt,
                targets = listOf(ChosenTarget.Permanent(bear)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        val boltOnStack = driver.getTopOfStack()!!
        driver.stackSize shouldBe 1

        // Opponent passes priority
        driver.passPriority(opponent)

        // Dragon's controller activates the redirect ability targeting the bolt
        driver.giveMana(activePlayer, Color.BLUE, 1)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = dragon,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Spell(boltOnStack))
            )
        )

        driver.stackSize shouldBe 2

        // Both pass — ability resolves but does nothing (spell targets bear, not the Dragon)
        driver.bothPass()

        // No decision should be presented — the condition failed
        driver.stackSize shouldBe 1

        // Both pass — bolt resolves normally, killing the bear
        driver.bothPass()

        driver.stackSize shouldBe 0
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        driver.assertInGraveyard(activePlayer, "Grizzly Bears")

        // Dragon should still be alive
        driver.findPermanent(activePlayer, "Quicksilver Dragon") shouldNotBe null
    }
})
