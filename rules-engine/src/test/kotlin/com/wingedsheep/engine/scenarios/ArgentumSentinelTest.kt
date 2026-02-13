package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.serialization.CardLoader
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * End-to-end test: load a fictional creature card from pure JSON (no code definition)
 * and verify it behaves correctly in the game engine.
 *
 * Argentum Sentinel: {2}{W}
 * Creature — Human Soldier
 * 3/2
 * First strike
 * When Argentum Sentinel enters the battlefield, you gain 2 life.
 * {T}, Pay 1 life: Argentum Sentinel deals 1 damage to target creature.
 */
class ArgentumSentinelTest : FunSpec({

    // The card defined entirely as JSON — no Kotlin card definition exists
    val cardJson = """
    {
        "name": "Argentum Sentinel",
        "manaCost": "{2}{W}",
        "typeLine": "Creature — Human Soldier",
        "oracleText": "First strike\nWhen Argentum Sentinel enters the battlefield, you gain 2 life.\n{T}, Pay 1 life: Argentum Sentinel deals 1 damage to target creature.",
        "creatureStats": {
            "power": { "type": "FixedValue", "value": 3 },
            "toughness": { "type": "FixedValue", "value": 2 }
        },
        "keywords": ["FIRST_STRIKE"],
        "script": {
            "triggeredAbilities": [
                {
                    "trigger": { "type": "EntersBattlefield" },
                    "effect": {
                        "type": "GainLife",
                        "amount": { "type": "Fixed", "amount": 2 }
                    }
                }
            ],
            "activatedAbilities": [
                {
                    "cost": {
                        "type": "CostComposite",
                        "costs": [
                            { "type": "CostTap" },
                            { "type": "CostPayLife", "amount": 1 }
                        ]
                    },
                    "effect": {
                        "type": "DealDamage",
                        "amount": { "type": "Fixed", "amount": 1 },
                        "target": { "type": "ContextTarget", "index": 0 }
                    },
                    "targetRequirement": { "type": "TargetCreature" }
                }
            ]
        }
    }
    """.trimIndent()

    // Load from JSON — this is all the engine needs
    val argentumSentinel = CardLoader.fromJson(cardJson)
    val sentinelAbilityId = argentumSentinel.script.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(argentumSentinel))
        return driver
    }

    test("JSON loads correctly into a CardDefinition") {
        argentumSentinel.name shouldBe "Argentum Sentinel"
        argentumSentinel.manaCost shouldBe ManaCost.parse("{2}{W}")
        argentumSentinel.creatureStats!!.basePower shouldBe 3
        argentumSentinel.creatureStats!!.baseToughness shouldBe 2
        argentumSentinel.keywords shouldBe setOf(Keyword.FIRST_STRIKE)
        argentumSentinel.script.triggeredAbilities.size shouldBe 1
        argentumSentinel.script.triggeredAbilities[0].trigger.shouldBeInstanceOf<OnEnterBattlefield>()
        argentumSentinel.script.triggeredAbilities[0].effect.shouldBeInstanceOf<GainLifeEffect>()
        argentumSentinel.script.activatedAbilities.size shouldBe 1
        argentumSentinel.script.activatedAbilities[0].cost.shouldBeInstanceOf<AbilityCost.Composite>()
    }

    test("ETB trigger grants 2 life when cast") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.getLifeTotal(activePlayer) shouldBe 20

        val sentinel = driver.putCardInHand(activePlayer, "Argentum Sentinel")
        driver.giveMana(activePlayer, Color.WHITE, 3)

        val castResult = driver.castSpell(activePlayer, sentinel)
        castResult.isSuccess shouldBe true

        // Resolve creature spell
        driver.bothPass()
        driver.findPermanent(activePlayer, "Argentum Sentinel") shouldNotBe null

        // Resolve ETB trigger
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        driver.getLifeTotal(activePlayer) shouldBe 22
    }

    test("activated ability deals 1 damage and costs 1 life") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sentinelPermanent = driver.putCreatureOnBattlefield(activePlayer, "Argentum Sentinel")
        driver.removeSummoningSickness(sentinelPermanent)

        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears") // 2/2
        val bears = driver.findPermanent(opponent, "Grizzly Bears")!!

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = sentinelPermanent,
                abilityId = sentinelAbilityId,
                targets = listOf(ChosenTarget.Permanent(bears))
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        driver.isTapped(sentinelPermanent) shouldBe true
        driver.getLifeTotal(activePlayer) shouldBe 19
        // 2/2 survives 1 damage
        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null
    }

    test("activated ability kills a 1-toughness creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sentinelPermanent = driver.putCreatureOnBattlefield(activePlayer, "Argentum Sentinel")
        driver.removeSummoningSickness(sentinelPermanent)

        driver.putCreatureOnBattlefield(opponent, "Goblin Guide") // 2/1
        val goblin = driver.findPermanent(opponent, "Goblin Guide")!!

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = sentinelPermanent,
                abilityId = sentinelAbilityId,
                targets = listOf(ChosenTarget.Permanent(goblin))
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(opponent, "Goblin Guide") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Goblin Guide"
    }

    test("full gameplay: cast from JSON, ETB fires, then activate ability to kill creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Goblin Guide")

        // Cast from hand
        val sentinel = driver.putCardInHand(activePlayer, "Argentum Sentinel")
        driver.giveMana(activePlayer, Color.WHITE, 3)
        driver.castSpell(activePlayer, sentinel)
        driver.bothPass()

        val sentinelPermanent = driver.findPermanent(activePlayer, "Argentum Sentinel")!!

        // Resolve ETB (gain 2 life)
        if (driver.stackSize > 0) {
            driver.bothPass()
        }
        driver.getLifeTotal(activePlayer) shouldBe 22

        // Next turn simulation — remove summoning sickness
        driver.removeSummoningSickness(sentinelPermanent)

        // Activate ability to kill Goblin Guide
        val goblin = driver.findPermanent(opponent, "Goblin Guide")!!
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = sentinelPermanent,
                abilityId = sentinelAbilityId,
                targets = listOf(ChosenTarget.Permanent(goblin))
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // 22 - 1 (ability cost) = 21
        driver.getLifeTotal(activePlayer) shouldBe 21
        driver.findPermanent(opponent, "Goblin Guide") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Goblin Guide"
    }
})
