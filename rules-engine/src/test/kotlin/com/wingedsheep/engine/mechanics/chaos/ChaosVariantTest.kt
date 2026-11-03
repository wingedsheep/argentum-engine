package com.wingedsheep.engine.mechanics.chaos

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.GameRestartRequest
import com.wingedsheep.engine.core.GameRestarter
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ChosenModesEverComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.EmblemSourceComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.chaos.ChaosRules
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The **Chaos** house variant ([ChaosVariant], [ChaosRules]): every player has a Chaos emblem, and
 * at the beginning of each of their upkeeps it rolls a random chaos rule.
 *
 * The roll is a "choose one at random" modal trigger, so the engine — not the player — draws the
 * rule as the trigger goes on the stack, leaves out rules that couldn't legally happen, and never
 * draws a rule its emblem has drawn before. The rolled rule's "you" is the player whose upkeep it
 * is, and the emblem, a colorless object (CR 114.3), is the source of anything it does.
 */
class ChaosVariantTest : FunSpec({

    val forest = basicLand("Forest") {}
    val bears = card("Test Chaos Bears") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val deck = Deck.of("Forest" to 20, "Test Chaos Bears" to 20)

    fun indexOf(name: String): Int = ChaosRules.all.indexOfFirst { it.name == name }.also { check(it >= 0) { name } }

    /** Every rule index except [names] — what an emblem must already have rolled to leave only them. */
    fun allBut(vararg names: String): Set<Int> = ChaosRules.all.indices.toSet() - names.map(::indexOf).toSet()

    fun withMemory(state: GameState, emblem: EntityId, spent: Set<Int>): GameState =
        state.updateEntity(emblem) { it.with(ChosenModesEverComponent(spent)) }

    /**
     * A two-player Chaos game advanced to player 2's upkeep, where player 2's emblem rolls from the
     * rules [player2Pool] names. Player 1's emblem has spent every rule, so it never interferes.
     */
    fun atPlayer2Upkeep(vararg player2Pool: String, seed: Long = 1L): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(listOf(forest, bears))
        driver.initMirrorMatch(deck)
        var state = ChaosVariant.install(driver.state, listOf(driver.player1, driver.player2))
        state = withMemory(state, ChaosVariant.emblemOf(state, driver.player1)!!, ChaosRules.all.indices.toSet())
        state = withMemory(state, ChaosVariant.emblemOf(state, driver.player2)!!, allBut(*player2Pool))
        driver.replaceState(state.copy(rng = GameRng.seeded(seed)))
        driver.passPriorityUntil(Step.UPKEEP, driver.player2)
        return driver
    }

    fun GameTestDriver.chaosTriggers(): List<TriggeredAbilityOnStackComponent> =
        state.stack.mapNotNull { state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>() }
            .filter { it.sourceId in ChaosVariant.emblems(state).values }

    test("a Chaos game gives every player a Chaos emblem of their own") {
        val registry = com.wingedsheep.engine.registry.CardRegistry().apply { register(listOf(forest, bears)) }
        val result = GameInitializer(registry).initializeGame(
            GameConfig(
                players = listOf(PlayerConfig("Ann", deck), PlayerConfig("Bo", deck)),
                skipMulligans = true,
                seed = 7L,
                chaosRules = true,
            )
        )
        val state = result.state
        state.chaosRules shouldBe true
        val emblems = ChaosVariant.emblems(state)
        emblems.keys shouldBe result.playerIds.toSet()
        for ((player, emblem) in emblems) {
            val container = state.getEntity(emblem)!!
            container.get<ControllerComponent>()?.playerId shouldBe player
            container.get<EmblemSourceComponent>()?.sourceName shouldBe ChaosRules.SOURCE_NAME
            withClue("an emblem has no types, colors or mana cost (CR 114.3)") {
                val card = container.get<CardComponent>()!!
                card.typeLine.cardTypes.isEmpty() shouldBe true
                card.colors.isEmpty() shouldBe true
            }
            withClue("an emblem is in no zone") {
                state.zones.values.any { emblem in it } shouldBe false
            }
        }
    }

    test("without the variant nobody has an emblem and the same seed deals the same game") {
        val registry = com.wingedsheep.engine.registry.CardRegistry().apply { register(listOf(forest, bears)) }
        fun init(chaos: Boolean) = GameInitializer(registry).initializeGame(
            GameConfig(
                players = listOf(PlayerConfig("Ann", deck), PlayerConfig("Bo", deck)),
                skipMulligans = true,
                seed = 11L,
                chaosRules = chaos,
            )
        ).state
        val plain = init(false)
        val chaos = init(true)
        plain.chaosRules shouldBe false
        ChaosVariant.emblems(plain) shouldBe emptyMap()
        withClue("minting the emblems draws no randomness") {
            chaos.turnOrder shouldBe plain.turnOrder
            chaos.getHand(chaos.turnOrder.first()) shouldBe plain.getHand(plain.turnOrder.first())
        }
    }

    test("at the beginning of your upkeep your emblem rolls a rule, without asking anyone") {
        val pool = arrayOf("Blessed Wine", "Collective Unconscious", "Reset")
        val driver = atPlayer2Upkeep(*pool)
        driver.state.pendingDecision shouldBe null
        val rolled = driver.chaosTriggers().single()
        rolled.controllerId shouldBe driver.player2
        rolled.sourceId shouldBe ChaosVariant.emblemOf(driver.state, driver.player2)
        rolled.chosenModes.size shouldBe 1
        withClue("the rule comes from this emblem's pool") {
            (ChaosRules.all[rolled.chosenModes.single()].name in pool) shouldBe true
        }
    }

    test("every rule in the pool can come up, and a seed always rolls the same rule") {
        val pool = arrayOf("Blessed Wine", "Collective Unconscious", "Reset")
        val seen = (1L..30L).map { seed -> atPlayer2Upkeep(*pool, seed = seed).chaosTriggers().single().chosenModes.single() }.toSet()
        seen shouldBe pool.map(::indexOf).toSet()
        atPlayer2Upkeep(*pool, seed = 5L).chaosTriggers().single().chosenModes shouldBe
            atPlayer2Upkeep(*pool, seed = 5L).chaosTriggers().single().chosenModes
    }

    test("the rolled rule happens to the player who rolled it") {
        val driver = atPlayer2Upkeep("Blessed Wine")
        val life = driver.getLifeTotal(driver.player2)
        val hand = driver.getHand(driver.player2).size
        driver.bothPass()
        driver.chaosTriggers() shouldBe emptyList()
        driver.getLifeTotal(driver.player2) shouldBe life + 1
        driver.getHand(driver.player2).size shouldBe hand + 1
        driver.getLifeTotal(driver.player1) shouldBe 20
    }

    test("a rule that couldn't happen is left out of the roll") {
        // Shatter needs an artifact to target; there is none, so only Blessed Wine can be rolled.
        for (seed in 1L..12L) {
            val rolled = atPlayer2Upkeep("Shatter", "Blessed Wine", seed = seed).chaosTriggers().single()
            withClue("seed $seed") { rolled.chosenModes shouldBe listOf(indexOf("Blessed Wine")) }
        }
    }

    test("a player never rolls the same rule twice") {
        val driver = atPlayer2Upkeep("Blessed Wine")
        val emblem = ChaosVariant.emblemOf(driver.state, driver.player2)!!
        driver.bothPass()
        driver.state.getEntity(emblem)?.get<ChosenModesEverComponent>()?.modeIndices shouldBe ChaosRules.all.indices.toSet()
        // Player 2's next upkeep: every rule is spent, so the emblem has nothing left to roll.
        driver.passPriorityUntil(Step.DRAW, driver.player2)
        driver.passPriorityUntil(Step.UPKEEP, driver.player2)
        driver.chaosTriggers() shouldBe emptyList()
    }

    test("damage a rule deals comes from the Chaos emblem") {
        // rekeeS mrotS: the roller is dealt damage equal to the number of cards in their hand.
        val driver = atPlayer2Upkeep("rekeeS mrotS")
        val emblem = ChaosVariant.emblemOf(driver.state, driver.player2)!!
        val hand = driver.getHand(driver.player2).size
        driver.bothPass()
        val damage = driver.events.filterIsInstance<DamageDealtEvent>().last()
        damage.sourceId shouldBe emblem
        damage.targetId shouldBe driver.player2
        damage.amount shouldBe hand
        driver.getLifeTotal(driver.player2) shouldBe 20 - hand
    }

    test("a restarted game keeps the variant and starts every emblem fresh") {
        val driver = atPlayer2Upkeep("Blessed Wine")
        val before = ChaosVariant.emblems(driver.state)
        val restarted = GameRestarter(driver.cardRegistry)
            .restart(driver.state, GameRestartRequest(startingPlayerId = driver.player1)).state
        restarted.chaosRules shouldBe true
        val after = ChaosVariant.emblems(restarted)
        after.keys shouldBe before.keys
        for ((player, emblem) in after) {
            emblem shouldNotBe before[player]
            restarted.getEntity(emblem)?.get<ChosenModesEverComponent>() shouldBe null
        }
    }
})
