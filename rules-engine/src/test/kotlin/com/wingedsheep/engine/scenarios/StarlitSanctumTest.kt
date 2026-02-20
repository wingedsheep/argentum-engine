package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Starlit Sanctum.
 *
 * Starlit Sanctum
 * Land
 * {T}: Add {C}.
 * {W}, {T}, Sacrifice a Cleric creature: You gain life equal to the sacrificed creature's toughness.
 * {B}, {T}, Sacrifice a Cleric creature: Target player loses life equal to the sacrificed creature's power.
 */
class StarlitSanctumTest : FunSpec({

    val StarlitSanctum = card("Starlit Sanctum") {
        typeLine = "Land"

        activatedAbility {
            cost = AbilityCost.Tap
            effect = AddColorlessManaEffect(1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{W}"),
                Costs.Tap,
                Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Cleric"))
            )
            effect = GainLifeEffect(
                amount = DynamicAmount.SacrificedPermanentToughness,
                target = EffectTarget.Controller
            )
        }

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{B}"),
                Costs.Tap,
                Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Cleric"))
            )
            target = TargetPlayer()
            effect = LoseLifeEffect(
                amount = DynamicAmount.SacrificedPermanentPower,
                target = EffectTarget.ContextTarget(0)
            )
        }
    }

    val gainLifeAbilityId = StarlitSanctum.activatedAbilities[1].id
    val loseLifeAbilityId = StarlitSanctum.activatedAbilities[2].id

    // Non-Cleric creature for negative tests
    val GoblinScout = CardDefinition.creature(
        name = "Goblin Scout",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 1,
        toughness = 1
    )

    // A bigger Cleric to test dynamic amounts (3/4)
    val BigCleric = CardDefinition.creature(
        name = "Big Cleric",
        manaCost = ManaCost.parse("{3}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Cleric")),
        power = 3,
        toughness = 4
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(StarlitSanctum, GoblinScout, BigCleric))
        return driver
    }

    test("white ability - sacrifice a Cleric to gain life equal to its toughness") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sanctum = driver.putPermanentOnBattlefield(activePlayer, "Starlit Sanctum")
        val cleric = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric") // 2/2

        driver.giveMana(activePlayer, Color.WHITE, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = sanctum,
                abilityId = gainLifeAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(cleric))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Cleric should be sacrificed
        driver.findPermanent(activePlayer, "Test Cleric") shouldBe null

        // Should gain life equal to toughness (2)
        driver.getLifeTotal(activePlayer) shouldBe 22
    }

    test("white ability - gains life equal to toughness of bigger Cleric") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sanctum = driver.putPermanentOnBattlefield(activePlayer, "Starlit Sanctum")
        val cleric = driver.putCreatureOnBattlefield(activePlayer, "Big Cleric") // 3/4

        driver.giveMana(activePlayer, Color.WHITE, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = sanctum,
                abilityId = gainLifeAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(cleric))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Should gain life equal to toughness (4)
        driver.getLifeTotal(activePlayer) shouldBe 24
    }

    test("black ability - target player loses life equal to sacrificed Cleric power") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sanctum = driver.putPermanentOnBattlefield(activePlayer, "Starlit Sanctum")
        val cleric = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric") // 2/2

        driver.giveMana(activePlayer, Color.BLACK, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = sanctum,
                abilityId = loseLifeAbilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(cleric))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Cleric should be sacrificed
        driver.findPermanent(activePlayer, "Test Cleric") shouldBe null

        // Opponent should lose life equal to power (2)
        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("black ability - loses life equal to power of bigger Cleric") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sanctum = driver.putPermanentOnBattlefield(activePlayer, "Starlit Sanctum")
        val cleric = driver.putCreatureOnBattlefield(activePlayer, "Big Cleric") // 3/4

        driver.giveMana(activePlayer, Color.BLACK, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = sanctum,
                abilityId = loseLifeAbilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(cleric))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Opponent should lose life equal to power (3)
        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("cannot sacrifice a non-Cleric creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sanctum = driver.putPermanentOnBattlefield(activePlayer, "Starlit Sanctum")
        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Scout")

        driver.giveMana(activePlayer, Color.WHITE, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = sanctum,
                abilityId = gainLifeAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(goblin))
            )
        )
        result.isSuccess shouldBe false

        // Life should not change
        driver.getLifeTotal(activePlayer) shouldBe 20
    }

    test("black ability can target yourself") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sanctum = driver.putPermanentOnBattlefield(activePlayer, "Starlit Sanctum")
        val cleric = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric") // 2/2

        driver.giveMana(activePlayer, Color.BLACK, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = sanctum,
                abilityId = loseLifeAbilityId,
                targets = listOf(ChosenTarget.Player(activePlayer)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(cleric))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Active player should lose life equal to power (2)
        driver.getLifeTotal(activePlayer) shouldBe 18
    }
})
