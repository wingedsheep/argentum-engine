package com.wingedsheep.engine

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

/**
 * BDD: connive whose +1/+1 counter lands on a *reflexively chosen* target — the Teo, Spirited
 * Glider shape ("...When you discard a nonland card this way, put a +1/+1 counter on target
 * creature you control"). The recipient must be chosen only AFTER a nonland card is discarded,
 * never up front, and not at all when the discard is a land. Regression for the bug where the
 * target was declared as a cast-time `target(...)`, forcing the choice before the discard.
 */
class ConniveTargetingTest : FunSpec({

    val abilityId = AbilityId(UUID.randomUUID().toString())

    // {T}: draw a card, then discard a card. When you discard a nonland card this way, put a
    // +1/+1 counter on target creature you control.
    val Conniver = CardDefinition(
        name = "Targeting Conniver",
        manaCost = ManaCost.parse("{2}{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Human"))),
        oracleText = "{T}: Connive onto target creature you control.",
        creatureStats = CreatureStats(2, 2),
        script = CardScript.permanent(
            ActivatedAbility(
                id = abilityId,
                cost = AbilityCost.Tap,
                effect = Effects.ConniveTargeting(Targets.CreatureYouControl)
            )
        )
    )

    // (driver, conniver, other creature, card-to-discard) — paused on the discard selection.
    data class Setup(
        val driver: GameTestDriver,
        val conniver: EntityId,
        val other: EntityId,
        val toDiscard: EntityId,
    )

    fun setup(handCardName: String): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Conniver))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Forest" to 30),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        driver.putCardOnTopOfLibrary(player, "Grizzly Bears")
        val handCard = driver.putCardInHand(player, handCardName)
        val conniver = driver.putCreatureOnBattlefield(player, "Targeting Conniver")
        // A second creature you control, so the counter target is a genuine choice (not auto-select).
        val other = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.removeSummoningSickness(conniver)

        driver.submit(
            ActivateAbility(playerId = player, sourceId = conniver, abilityId = abilityId)
        ).isSuccess shouldBe true
        driver.bothPass()

        // First pause is the DISCARD selection — the counter target has NOT been chosen yet.
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        return Setup(driver, conniver, other, handCard)
    }

    fun GameTestDriver.counters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("nonland discard: target is chosen AFTER the discard, then gets the counter") {
        val (driver, conniver, other, nonland) = setup("Grizzly Bears")
        val player = driver.activePlayer!!

        // Discard the nonland.
        val discard = driver.pendingDecision as SelectCardsDecision
        driver.submitDecision(
            player,
            CardsSelectedResponse(decisionId = discard.id, selectedCards = listOf(nonland))
        )

        // Only NOW — after a nonland was discarded — are we asked to choose the counter recipient.
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        // Put the counter on the OTHER creature, not the conniver — proving it's a free target.
        driver.submitTargetSelection(player, listOf(other))
        driver.isPaused shouldBe false

        driver.counters(other) shouldBe 1
        driver.counters(conniver) shouldBe 0
    }

    test("land discard: no target is ever requested and no counter is placed") {
        val (driver, conniver, other, land) = setup("Forest")
        val player = driver.activePlayer!!

        val discard = driver.pendingDecision as SelectCardsDecision
        driver.submitDecision(
            player,
            CardsSelectedResponse(decisionId = discard.id, selectedCards = listOf(land))
        )

        // Land discard → reflexive trigger doesn't fire → no target prompt, resolution completes.
        driver.isPaused shouldBe false

        driver.counters(other) shouldBe 0
        driver.counters(conniver) shouldBe 0
    }
})
