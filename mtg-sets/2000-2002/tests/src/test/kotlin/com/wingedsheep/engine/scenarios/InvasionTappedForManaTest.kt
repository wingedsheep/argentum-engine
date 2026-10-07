package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.inv.cards.Overabundance
import com.wingedsheep.mtg.sets.definitions.inv.cards.PulseOfLlanowar
import com.wingedsheep.mtg.sets.definitions.usg.cards.FertileGround
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome

/**
 * Invasion engine gap #3 — "whenever a player taps a land for mana" mana effects.
 *
 * Covers the three cards built on the tapped-for-mana primitives:
 *  - **Fertile Ground** (aura): the enchanted land's controller gets an additional one mana of
 *    any color — a per-tap color choice resolved off-stack (pauses for a color decision).
 *  - **Pulse of Llanowar**: a basic you control produces one mana of a color of your choice instead
 *    of its normal mana (base mana effect swapped for "add one mana of any color").
 *  - **Overabundance**: tapping a land mirrors the produced mana and deals 1 damage to the tapper
 *    (the inline non-mana rider on the source-tap static).
 *
 * Mana abilities are activated manually via the land's intrinsic mana ability so the rider / color
 * choice on the [com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler] path is
 * exercised directly.
 */
class InvasionTappedForManaTest : FunSpec({

    val TestForest = basicLand("Forest") {}
    val TestPlains = basicLand("Plains") {}

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestForest, TestPlains, FertileGround, Overabundance, PulseOfLlanowar))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun GameTestDriver.pool(playerId: EntityId): ManaPoolComponent =
        state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    /** Put a non-creature aura on the battlefield already attached to [target]. */
    fun GameTestDriver.attachAura(playerId: EntityId, cardDef: CardDefinition, target: EntityId): EntityId {
        val auraId = EntityId.generate()
        val cardComponent = CardComponent(
            cardDefinitionId = cardDef.name,
            name = cardDef.name,
            manaCost = cardDef.manaCost,
            typeLine = cardDef.typeLine,
            oracleText = cardDef.oracleText,
            baseStats = cardDef.creatureStats,
            baseKeywords = cardDef.keywords,
            baseFlags = cardDef.flags,
            colors = cardDef.colors,
            ownerId = playerId,
            spellEffect = cardDef.spellEffect
        )
        val container = ComponentContainer.of(
            cardComponent,
            OwnerComponent(playerId),
            ControllerComponent(playerId),
            AttachedToComponent(target)
        )
        var newState = state.withEntity(auraId, container)
        newState = newState.addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), auraId)
        val existing = newState.getEntity(target)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        newState = newState.updateEntity(target) { it.with(AttachmentsComponent(existing + auraId)) }
        replaceState(newState)
        return auraId
    }

    test("Fertile Ground adds an additional one mana of any color (chosen) when the land is tapped") {
        val (driver, you) = newGame()
        val forest = driver.putLandOnBattlefield(you, "Forest")
        driver.attachAura(you, FertileGround, forest)

        // Tapping the Forest produces {G}; the any-color bonus then pauses for a color choice.
        val tapResult = driver.submit(
            ActivateAbility(playerId = you, sourceId = forest, abilityId = AbilityId.intrinsicMana('G'))
        )
        (tapResult.outcome is Outcome.Paused).shouldBeTrue()

        // Base {G} is already in the pool; the bonus is still pending.
        driver.pool(you).green shouldBe 1
        driver.pool(you).blue shouldBe 0

        driver.submitDecision(you, ColorChosenResponse(tapResult.pendingDecision!!.id, Color.BLUE))

        driver.pool(you).green shouldBe 1
        driver.pool(you).blue shouldBe 1
    }

    test("Pulse of Llanowar: a basic you control produces a color of your choice instead") {
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Pulse of Llanowar")
        val forest = driver.putLandOnBattlefield(you, "Forest")

        // Tap the Forest choosing blue — Pulse replaces its green with the chosen color.
        driver.submitSuccess(
            ActivateAbility(playerId = you, sourceId = forest, abilityId = AbilityId.intrinsicMana('G'), manaColorChoice = Color.BLUE)
        )
        driver.pool(you).blue shouldBe 1
        driver.pool(you).green shouldBe 0
    }

    test("Without Pulse, a Forest's mana-color choice is ignored (still produces green)") {
        val (driver, you) = newGame()
        val forest = driver.putLandOnBattlefield(you, "Forest")

        driver.submitSuccess(
            ActivateAbility(playerId = you, sourceId = forest, abilityId = AbilityId.intrinsicMana('G'), manaColorChoice = Color.BLUE)
        )
        driver.pool(you).green shouldBe 1
        driver.pool(you).blue shouldBe 0
    }

    test("Overabundance mirrors the produced mana and deals 1 damage to the tapping player") {
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Overabundance")
        val forest = driver.putLandOnBattlefield(you, "Forest")

        driver.submitSuccess(
            ActivateAbility(playerId = you, sourceId = forest, abilityId = AbilityId.intrinsicMana('G'))
        )

        // Base {G} + mirrored {G} bonus = two green; 1 damage to the tapper.
        driver.pool(you).green shouldBe 2
        driver.getLifeTotal(you) shouldBe 19
    }
})
