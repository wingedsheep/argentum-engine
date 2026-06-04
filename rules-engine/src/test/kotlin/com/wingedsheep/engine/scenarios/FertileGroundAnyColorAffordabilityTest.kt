package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.LegalActionEnricher
import com.wingedsheep.mtg.sets.definitions.inv.cards.ChaoticStrike
import com.wingedsheep.mtg.sets.definitions.inv.cards.FertileGround
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Regression: Fertile Ground's "add an additional one mana of any color" lets the enchanted land
 * pay a colored pip no other source can produce. The auto-tap solver paid colored pips before
 * tapping any source, so the any-color bonus — which only lands in the bonus pool once its source
 * is tapped — was invisible to the colored pass. A player whose only red came from a Fertile Ground
 * forest was told they could not afford `{1}{R}`, so the engine never paused to offer the trick.
 *
 * Reported via: attacking with a 2/1 (Goblin Guide) into a 1/1 (Savannah Lions) block, holding
 * Chaotic Strike ({1}{R}) with only forests in play — one enchanted with Fertile Ground. The engine
 * skipped the declare-blockers priority window because it judged Chaotic Strike unaffordable.
 */
class FertileGroundAnyColorAffordabilityTest : FunSpec({

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FertileGround, ChaoticStrike))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

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

    test("two plain forests cannot pay {1}{R} (no red source)") {
        val (driver, you) = newGame()
        repeat(2) { driver.putLandOnBattlefield(you, "Forest") }

        ManaSolver(driver.cardRegistry)
            .canPay(driver.state, you, ManaCost.parse("{1}{R}")) shouldBe false
    }

    test("a Fertile Ground forest supplies the {R}, making {1}{R} payable") {
        val (driver, you) = newGame()
        driver.putLandOnBattlefield(you, "Forest")
        val enchantedForest = driver.putLandOnBattlefield(you, "Forest")
        driver.attachAura(you, FertileGround, enchantedForest)

        ManaSolver(driver.cardRegistry)
            .canPay(driver.state, you, ManaCost.parse("{1}{R}")) shouldBe true
    }

    test("a single Fertile Ground forest pays {1}{R} on its own (printed {G} + any-color bonus)") {
        val (driver, you) = newGame()
        val enchantedForest = driver.putLandOnBattlefield(you, "Forest")
        driver.attachAura(you, FertileGround, enchantedForest)

        val solver = ManaSolver(driver.cardRegistry)
        // {G} pays the generic {1}; the any-color bonus pays the {R}.
        solver.canPay(driver.state, you, ManaCost.parse("{1}{R}")) shouldBe true
        // And the bonus alone covers a lone colored pip.
        solver.canPay(driver.state, you, ManaCost.parse("{R}")) shouldBe true
    }

    test("Chaotic Strike is an affordable legal action in the declare-blockers window via Fertile Ground") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FertileGround, ChaoticStrike))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attacker = driver.activePlayer!!
        val defender = if (attacker == driver.player1) driver.player2 else driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val goblin = driver.putCreatureOnBattlefield(attacker, "Goblin Guide")     // 2/1
        val lions = driver.putCreatureOnBattlefield(defender, "Savannah Lions")    // 1/1
        driver.removeSummoningSickness(goblin)

        // Attacker holds Chaotic Strike and only forests — one enchanted with Fertile Ground.
        driver.putCardInHand(attacker, "Chaotic Strike")
        driver.putLandOnBattlefield(attacker, "Forest")
        val enchantedForest = driver.putLandOnBattlefield(attacker, "Forest")
        driver.attachAura(attacker, FertileGround, enchantedForest)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(goblin), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(lions to listOf(goblin)))
        driver.submit(PassPriority(defender))

        // Active player now holds the pre-damage priority window (where Chaotic Strike is legal).
        driver.state.priorityPlayerId shouldBe attacker
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        val services = EngineServices(driver.cardRegistry)
        val enumerator = LegalActionEnumerator(
            driver.cardRegistry, services.manaSolver, services.costCalculator,
            services.predicateEvaluator, services.conditionEvaluator, services.turnManager
        )
        val enricher = LegalActionEnricher(services.manaSolver, driver.cardRegistry)
        val legalActions = enricher.enrich(enumerator.enumerate(driver.state, attacker), driver.state, attacker)

        val chaoticStrike = legalActions.filter {
            it.actionType == "CastSpell" && it.description.contains("Chaotic Strike", ignoreCase = true)
        }
        chaoticStrike.shouldNotBeEmpty()
        chaoticStrike.first().isAffordable shouldBe true
    }
})
