package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.PutCreatureFromHandSharingTypeWithTappedEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Cryptic Gateway:
 * {5}
 * Artifact
 * Tap two untapped creatures you control: You may put a creature card from your hand
 * that shares a creature type with each creature tapped this way onto the battlefield.
 */
class CrypticGatewayTest : FunSpec({

    val abilityId = AbilityId("cryptic-gateway-0")

    val CrypticGateway = CardDefinition.artifact(
        name = "Cryptic Gateway",
        manaCost = ManaCost.parse("{5}"),
        oracleText = "Tap two untapped creatures you control: You may put a creature card from your hand that shares a creature type with each creature tapped this way onto the battlefield.",
        script = CardScript(
            activatedAbilities = listOf(
                ActivatedAbility(
                    id = abilityId,
                    cost = AbilityCost.TapPermanents(2, GameObjectFilter.Creature),
                    effect = PutCreatureFromHandSharingTypeWithTappedEffect
                )
            )
        )
    )

    // Goblin Warrior for testing shared types
    val GoblinWarrior = CardDefinition.creature(
        name = "Goblin Warrior",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Warrior")),
        power = 2,
        toughness = 2
    )

    // Elf Warrior for testing cross-type matching
    val ElfWarrior = CardDefinition.creature(
        name = "Elf Warrior",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Elf"), Subtype("Warrior")),
        power = 2,
        toughness = 2
    )

    // Human Warrior shares Warrior with both Goblin Warrior and Elf Warrior
    val HumanWarrior = CardDefinition.creature(
        name = "Human Warrior",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Warrior")),
        power = 2,
        toughness = 2
    )

    // Pure Elf (no Warrior) — shares Elf with Elf Warrior but not with Goblin Warrior
    val PureElf = CardDefinition.creature(
        name = "Pure Elf",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Elf")),
        power = 1,
        toughness = 1
    )

    val allCards = TestCards.all + listOf(
        CrypticGateway, GoblinWarrior, ElfWarrior, HumanWarrior, PureElf
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    test("put creature from hand that shares type with both tapped creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Set up: Cryptic Gateway on battlefield, two Goblin Warriors on battlefield
        val gateway = driver.putPermanentOnBattlefield(activePlayer, "Cryptic Gateway")
        val goblin1 = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val goblin2 = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")

        // Put a Goblin Warrior in hand (shares Goblin with both tapped creatures)
        val handGoblin = driver.putCardInHand(activePlayer, "Goblin Warrior")

        // Activate Cryptic Gateway by tapping two Goblin Warriors
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = gateway,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(goblin1, goblin2)
                )
            )
        )
        result.isSuccess shouldBe true

        // Resolve the ability (pass priority)
        driver.bothPass()

        // Now we should have a decision to select a creature from hand
        (driver.pendingDecision != null) shouldBe true

        // Select the goblin from hand
        driver.submitCardSelection(activePlayer, listOf(handGoblin))

        // Goblin should now be on the battlefield
        driver.getPermanents(activePlayer).contains(handGoblin) shouldBe true
        driver.getHand(activePlayer).contains(handGoblin) shouldBe false

        // The two tapped creatures should be tapped
        driver.isTapped(goblin1) shouldBe true
        driver.isTapped(goblin2) shouldBe true
    }

    test("creature must share type with EACH tapped creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gateway = driver.putPermanentOnBattlefield(activePlayer, "Cryptic Gateway")
        // Tap a Goblin Warrior and an Elf Warrior
        val goblinWarrior = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val elfWarrior = driver.putCreatureOnBattlefield(activePlayer, "Elf Warrior")

        // Put Human Warrior in hand (shares Warrior with both) — should be valid
        val humanWarrior = driver.putCardInHand(activePlayer, "Human Warrior")
        // Put Pure Elf in hand (shares Elf with Elf Warrior but nothing with Goblin Warrior)
        val pureElf = driver.putCardInHand(activePlayer, "Pure Elf")

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = gateway,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(goblinWarrior, elfWarrior)
                )
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Should have a decision — only Human Warrior should be a valid option
        (driver.pendingDecision != null) shouldBe true

        // Select Human Warrior (shares Warrior with both)
        driver.submitCardSelection(activePlayer, listOf(humanWarrior))

        // Human Warrior should be on battlefield
        driver.getPermanents(activePlayer).contains(humanWarrior) shouldBe true

        // Pure Elf should still be in hand (wasn't a valid choice)
        driver.getHand(activePlayer).contains(pureElf) shouldBe true
    }

    test("no valid creature in hand — effect does nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gateway = driver.putPermanentOnBattlefield(activePlayer, "Cryptic Gateway")
        val goblin1 = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val goblin2 = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")

        // Put Pure Elf in hand — doesn't share a type with Goblins
        val pureElf = driver.putCardInHand(activePlayer, "Pure Elf")

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = gateway,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(goblin1, goblin2)
                )
            )
        )
        result.isSuccess shouldBe true

        // Resolve ability — no decision since no valid creatures
        driver.bothPass()

        // Pure Elf should still be in hand
        driver.getHand(activePlayer).contains(pureElf) shouldBe true

        // Should not have a pending decision
        (driver.pendingDecision == null) shouldBe true
    }

    test("player may decline to put a creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gateway = driver.putPermanentOnBattlefield(activePlayer, "Cryptic Gateway")
        val goblin1 = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val goblin2 = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")

        val handGoblin = driver.putCardInHand(activePlayer, "Goblin Warrior")

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = gateway,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(goblin1, goblin2)
                )
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Decline by selecting nothing
        driver.submitCardSelection(activePlayer, emptyList())

        // Goblin should still be in hand
        driver.getHand(activePlayer).contains(handGoblin) shouldBe true
        driver.getPermanents(activePlayer).contains(handGoblin) shouldBe false
    }

    test("cannot activate with fewer than two creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gateway = driver.putPermanentOnBattlefield(activePlayer, "Cryptic Gateway")
        val goblin1 = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = gateway,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(goblin1)
                )
            )
        )
        result.isSuccess shouldBe false
    }
})
