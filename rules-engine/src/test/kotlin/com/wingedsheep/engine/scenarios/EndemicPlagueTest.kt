package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.effects.DestroyAllSharingTypeWithSacrificedEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Endemic Plague.
 *
 * Endemic Plague: {3}{B}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a creature.
 * Destroy all creatures that share a creature type with the sacrificed creature.
 * They can't be regenerated.
 */
class EndemicPlagueTest : FunSpec({

    // Test creatures with specific types
    val GoblinWarrior = CardDefinition.creature(
        name = "Goblin Warrior",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Warrior")),
        power = 1,
        toughness = 1
    )

    val GoblinShaman = CardDefinition.creature(
        name = "Goblin Shaman",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Shaman")),
        power = 2,
        toughness = 2
    )

    val ElfWarrior = CardDefinition.creature(
        name = "Elf Warrior",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Elf"), Subtype("Warrior")),
        power = 1,
        toughness = 1
    )

    val HumanKnight = CardDefinition.creature(
        name = "Human Knight",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Knight")),
        power = 2,
        toughness = 2
    )

    val EndemicPlague = CardDefinition.sorcery(
        name = "Endemic Plague",
        manaCost = ManaCost.parse("{3}{B}"),
        oracleText = "As an additional cost to cast this spell, sacrifice a creature. Destroy all creatures that share a creature type with the sacrificed creature. They can't be regenerated.",
        script = com.wingedsheep.sdk.model.CardScript.spell(
            effect = DestroyAllSharingTypeWithSacrificedEffect(noRegenerate = true),
            additionalCosts = listOf(
                com.wingedsheep.sdk.scripting.AdditionalCost.SacrificePermanent(
                    com.wingedsheep.sdk.scripting.GameObjectFilter.Creature
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                EndemicPlague, GoblinWarrior, GoblinShaman, ElfWarrior, HumanKnight
            )
        )
        return driver
    }

    test("Endemic Plague destroys all creatures sharing a type with the sacrificed creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Goblins on both sides and a non-Goblin
        val myGoblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val opponentGoblin = driver.putCreatureOnBattlefield(opponent, "Goblin Shaman")
        val knight = driver.putCreatureOnBattlefield(opponent, "Human Knight")

        // Put Endemic Plague in hand and give mana
        val plague = driver.putCardInHand(activePlayer, "Endemic Plague")
        driver.giveMana(activePlayer, Color.BLACK, 4)

        // Cast Endemic Plague, sacrificing Goblin Warrior
        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = plague,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(myGoblin)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Goblin Warrior was sacrificed as cost (should be in graveyard)
        driver.findPermanent(activePlayer, "Goblin Warrior") shouldBe null

        // Goblin Shaman shares Goblin type - should be destroyed
        driver.findPermanent(opponent, "Goblin Shaman") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Goblin Shaman"

        // Human Knight does not share any type with Goblin Warrior - should survive
        driver.findPermanent(opponent, "Human Knight") shouldNotBe null
    }

    test("Endemic Plague destroys creatures sharing any of the sacrificed creature's types") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Goblin Warrior has types: Goblin, Warrior
        // Elf Warrior has types: Elf, Warrior -> shares "Warrior"
        // Human Knight has types: Human, Knight -> shares nothing
        val myGoblinWarrior = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val elfWarrior = driver.putCreatureOnBattlefield(opponent, "Elf Warrior")
        val knight = driver.putCreatureOnBattlefield(opponent, "Human Knight")

        val plague = driver.putCardInHand(activePlayer, "Endemic Plague")
        driver.giveMana(activePlayer, Color.BLACK, 4)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = plague,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(myGoblinWarrior)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Elf Warrior shares "Warrior" type - should be destroyed
        driver.findPermanent(opponent, "Elf Warrior") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Elf Warrior"

        // Human Knight shares nothing - should survive
        driver.findPermanent(opponent, "Human Knight") shouldNotBe null
    }

    test("Endemic Plague does not destroy creatures with no shared types") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Sacrifice a Human Knight - only Human and Knight types
        val myKnight = driver.putCreatureOnBattlefield(activePlayer, "Human Knight")
        val opponentGoblin = driver.putCreatureOnBattlefield(opponent, "Goblin Shaman")

        val plague = driver.putCardInHand(activePlayer, "Endemic Plague")
        driver.giveMana(activePlayer, Color.BLACK, 4)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = plague,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(myKnight)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Goblin Shaman has no shared types with Human Knight - should survive
        driver.findPermanent(opponent, "Goblin Shaman") shouldNotBe null
    }
})
