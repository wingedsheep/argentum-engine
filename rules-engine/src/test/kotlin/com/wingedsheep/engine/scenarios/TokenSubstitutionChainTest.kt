package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MultiplyTokenCreation
import com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithToken
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Chained [ReplaceTokenCreationWithToken] substitutions.
 *
 * - CR 616.2: a replacement can become applicable to an event as the result of another replacement
 *   that modified it — a Fish replaced by a Shark is then checked against "if you would create a
 *   Shark token" (Fisher's Talent levels 2 and 3).
 * - CR 614.5: a replacement gets only one opportunity to affect an event and the events that replace
 *   it — so Fish → Shark → Fish stops at the Fish instead of looping.
 * - The substitutes skip a second count pass: a doubler scales the original event once.
 */
class TokenSubstitutionChainTest : FunSpec({

    fun creature(power: Int, type: String): Effect =
        Effects.CreateToken(power = power, toughness = power, colors = setOf(Color.BLUE), creatureTypes = setOf(type))

    fun substitution(name: String, from: String, toPower: Int, to: String) = card(name) {
        typeLine = "Enchantment"
        replacementEffect(
            ReplaceTokenCreationWithToken(
                tokens = listOf(creature(toPower, to)),
                appliesTo = EventPattern.TokenCreationEvent(
                    controller = Player.You,
                    tokenFilter = GameObjectFilter.Any.withSubtype(from)
                )
            )
        )
    }

    val fishToShark = substitution("Test Fish To Shark", "Fish", 3, "Shark")
    val sharkToOctopus = substitution("Test Shark To Octopus", "Shark", 8, "Octopus")
    val sharkToFish = substitution("Test Shark To Fish", "Shark", 1, "Fish")
    val treasureToFish = card("Test Treasure To Fish") {
        typeLine = "Enchantment"
        replacementEffect(
            ReplaceTokenCreationWithToken(
                tokens = listOf(creature(1, "Fish")),
                appliesTo = EventPattern.TokenCreationEvent(Player.You, GameObjectFilter.Artifact)
            )
        )
    }
    val doubler = card("Test Token Doubler") {
        typeLine = "Enchantment"
        replacementEffect(MultiplyTokenCreation(appliesTo = EventPattern.TokenCreationEvent(controller = Player.You)))
    }

    fun driver(vararg permanents: String): GameTestDriver = GameTestDriver().also { d ->
        d.registerCards(TestCards.all + listOf(fishToShark, sharkToOctopus, sharkToFish, treasureToFish, doubler, PredefinedTokens.Treasure))
        d.initMirrorMatch(Deck.of("Island" to 40))
        permanents.forEach { d.putPermanentOnBattlefield(d.player1, it) }
    }

    fun GameTestDriver.create(effect: Effect): GameState =
        services.effectExecutorRegistry.execute(state, effect, EffectContext(sourceId = null, controllerId = player1)).state

    fun GameState.tokensOf(player: EntityId): List<EntityId> = getBattlefield().filter { id ->
        val c = getEntity(id) ?: return@filter false
        c.has<TokenComponent>() && c.get<ControllerComponent>()?.playerId == player
    }

    fun GameState.subtypesOfTokens(player: EntityId): List<String> =
        tokensOf(player).map { id -> listOf("Fish", "Shark", "Octopus").single { projectedState.hasSubtype(id, it) } }

    test("one substitution applies on its own") {
        val d = driver(fishToShark.name)
        val state = d.create(creature(1, "Fish"))
        state.subtypesOfTokens(d.player1) shouldBe listOf("Shark")
        state.projectedState.getPower(state.tokensOf(d.player1).single()) shouldBe 3
    }

    test("a substitute token is checked again: Fish → Shark → Octopus (CR 616.2)") {
        val d = driver(fishToShark.name, sharkToOctopus.name)
        val state = d.create(creature(1, "Fish"))
        state.subtypesOfTokens(d.player1) shouldBe listOf("Octopus")
        state.projectedState.getPower(state.tokensOf(d.player1).single()) shouldBe 8
    }

    test("a Shark created directly skips the Fish step and becomes an Octopus") {
        val d = driver(fishToShark.name, sharkToOctopus.name)
        d.create(creature(3, "Shark")).subtypesOfTokens(d.player1) shouldBe listOf("Octopus")
    }

    test("each substitution applies once per event: Fish → Shark → Fish stops (CR 614.5)") {
        val d = driver(fishToShark.name, sharkToFish.name)
        val state = d.create(creature(1, "Fish"))
        withClue("Fish → Shark (first) → Fish (second); the first can't apply again") {
            state.subtypesOfTokens(d.player1) shouldBe listOf("Fish")
        }
    }

    test("the chain runs from a predefined token: Treasure → Fish → Shark") {
        val d = driver(treasureToFish.name, fishToShark.name)
        d.create(Effects.CreateTreasure()).subtypesOfTokens(d.player1) shouldBe listOf("Shark")
    }

    test("a doubler scales the original event once; the chain keeps the doubled count") {
        val d = driver(doubler.name, fishToShark.name, sharkToOctopus.name)
        d.create(creature(1, "Fish")).subtypesOfTokens(d.player1) shouldBe listOf("Octopus", "Octopus")
    }

    test("an opponent's substitutions don't touch your tokens") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, fishToShark.name)
        d.create(creature(1, "Fish")).subtypesOfTokens(d.player1) shouldBe listOf("Fish")
    }
})
