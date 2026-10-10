package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.effects.Effect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

/**
 * A single card put back on top of a library has no order to choose. The reorder prompt survives
 * for one card only where it is the player's *look* ("look at the top card of your library"); after
 * scry 1 / surveil 1 the selection already showed the card, so a second prompt is pure friction.
 */
class SingleCardPutBackPromptTest : FunSpec({

    fun looker(name: String, effect: Effect): Pair<CardDefinition, AbilityId> {
        val abilityId = AbilityId(UUID.randomUUID().toString())
        return CardDefinition(
            name = name,
            manaCost = ManaCost.parse("{U}"),
            typeLine = TypeLine.creature(setOf(Subtype("Human"), Subtype("Wizard"))),
            oracleText = "{U}: …",
            creatureStats = CreatureStats(1, 1),
            script = CardScript.permanent(
                ActivatedAbility(id = abilityId, cost = Costs.Mana(ManaCost.parse("{U}")), effect = effect)
            )
        ) to abilityId
    }

    val (scryer, scryAbility) = looker("Scry Adept", Patterns.Library.scry(1))
    val (surveiler, surveilAbility) = looker("Surveil Adept", Patterns.Library.surveil(1))
    val (peeker, peekAbility) = looker("Peek Adept", Patterns.Library.lookAtTopAndReorder(1))

    /** Activates [name]'s ability with a known top card and resolves it; returns (driver, player, top card). */
    fun activate(name: String, abilityId: AbilityId): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(scryer, surveiler, peeker))
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Mountain" to 30), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val top = driver.putCardOnTopOfLibrary(player, "Grizzly Bears")
        val source = driver.putCreatureOnBattlefield(player, name)
        driver.removeSummoningSickness(source)
        driver.giveMana(player, Color.BLUE, 1)
        driver.submit(ActivateAbility(playerId = player, sourceId = source, abilityId = abilityId)).outcome shouldBe Outcome.Done
        driver.bothPass()
        return Triple(driver, player, top)
    }

    test("scry 1 keeping the card on top asks only the scry question") {
        val (driver, player, top) = activate("Scry Adept", scryAbility)
        val select = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        select.options shouldBe listOf(top)
        driver.submitDecision(player, CardsSelectedResponse(select.id, emptyList()))

        driver.isPaused shouldBe false
        driver.state.getZone(ZoneKey(player, Zone.LIBRARY)).first() shouldBe top
    }

    test("surveil 1 keeping the card on top asks only the surveil question") {
        val (driver, player, top) = activate("Surveil Adept", surveilAbility)
        val select = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(player, CardsSelectedResponse(select.id, emptyList()))

        driver.isPaused shouldBe false
        driver.state.getZone(ZoneKey(player, Zone.LIBRARY)).first() shouldBe top
    }

    test("looking at the top card still shows it — the prompt is the look") {
        val (driver, player, top) = activate("Peek Adept", peekAbility)
        val look = driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        look.cards shouldBe listOf(top)
        driver.submitOrderedResponse(player, look.cards)

        driver.isPaused shouldBe false
        driver.state.getZone(ZoneKey(player, Zone.LIBRARY)).first() shouldBe top
    }
})
