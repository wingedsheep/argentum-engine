package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Siege-Gang Commander.
 *
 * Siege-Gang Commander
 * {3}{R}{R}
 * Creature — Goblin
 * 2/2
 * When Siege-Gang Commander enters the battlefield, create three 1/1 red Goblin creature tokens.
 * {1}{R}, Sacrifice a Goblin: Siege-Gang Commander deals 2 damage to any target.
 */
class SiegeGangCommanderTest : FunSpec({

    val SiegeGangCommander = card("Siege-Gang Commander") {
        manaCost = "{3}{R}{R}"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
        oracleText = "When Siege-Gang Commander enters the battlefield, create three 1/1 red Goblin creature tokens.\n{1}{R}, Sacrifice a Goblin: Siege-Gang Commander deals 2 damage to any target."

        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = CreateTokenEffect(
                count = 3,
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Goblin")
            )
        }

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{1}{R}"),
                Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Goblin"))
            )
            target = AnyTarget()
            effect = DealDamageEffect(2, EffectTarget.ContextTarget(0))
        }
    }

    val damageAbilityId = SiegeGangCommander.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SiegeGangCommander))
        return driver
    }

    /**
     * Cast Siege-Gang Commander from hand and resolve it + ETB trigger.
     * Returns the entity ID of the commander on the battlefield.
     */
    fun castAndResolveCommander(driver: GameTestDriver, playerId: com.wingedsheep.sdk.model.EntityId): com.wingedsheep.sdk.model.EntityId {
        val cardInHand = driver.putCardInHand(playerId, "Siege-Gang Commander")
        driver.giveMana(playerId, Color.RED, 5)
        driver.castSpell(playerId, cardInHand)
        // Resolve spell on the stack
        driver.bothPass()
        // Resolve ETB trigger (creates tokens)
        driver.bothPass()
        return driver.findPermanent(playerId, "Siege-Gang Commander")!!
    }

    test("ETB creates three 1/1 Goblin tokens") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        castAndResolveCommander(driver, activePlayer)

        // Should have Siege-Gang Commander + 3 Goblin tokens = 4 creatures
        val creatures = driver.getCreatures(activePlayer)
        creatures.size shouldBe 4
    }

    test("sacrifice a Goblin token to deal 2 damage to opponent") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val commander = castAndResolveCommander(driver, activePlayer)

        // Find a Goblin token to sacrifice
        val creatures = driver.getCreatures(activePlayer)
        val goblinToken = creatures.first { it != commander }

        // Activate: {1}{R}, Sacrifice a Goblin: deal 2 damage to target
        driver.giveMana(activePlayer, Color.RED, 2)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = commander,
                abilityId = damageAbilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(goblinToken))
            )
        )
        result.isSuccess shouldBe true

        // Resolve the ability
        driver.bothPass()

        // Opponent should have taken 2 damage
        driver.getLifeTotal(opponent) shouldBe 18

        // Should now have 3 creatures (commander + 2 remaining tokens)
        driver.getCreatures(activePlayer).size shouldBe 3
    }

    test("can sacrifice Siege-Gang Commander itself as a Goblin") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val commander = castAndResolveCommander(driver, activePlayer)

        // Sacrifice the Commander itself (it's a Goblin too)
        driver.giveMana(activePlayer, Color.RED, 2)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = commander,
                abilityId = damageAbilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(commander))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 18
        // Only 3 tokens remain
        driver.getCreatures(activePlayer).size shouldBe 3
    }

    test("cannot sacrifice a non-Goblin creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val commander = castAndResolveCommander(driver, activePlayer)

        // Put a non-Goblin creature on the battlefield
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        driver.giveMana(activePlayer, Color.RED, 2)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = commander,
                abilityId = damageAbilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear))
            )
        )
        result.isSuccess shouldBe false

        // Life should be unchanged
        driver.getLifeTotal(opponent) shouldBe 20
    }
})
