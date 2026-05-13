package com.wingedsheep.engine

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.effects.ConniveEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

/**
 * BDD: Connive — draw a card, discard a card; if the discarded card is a nonland,
 * put a +1/+1 counter on the conniving creature.
 */
class ConniveTest : FunSpec({

    val conniveAbilityId = AbilityId(UUID.randomUUID().toString())

    val ConniveCreature = CardDefinition(
        name = "Connive Creature",
        manaCost = ManaCost.parse("{2}{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Human"))),
        oracleText = "{T}: Connive. (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)",
        creatureStats = CreatureStats(2, 2),
        script = CardScript.permanent(
            ActivatedAbility(
                id = conniveAbilityId,
                cost = AbilityCost.Tap,
                effect = ConniveEffect(target = EffectTarget.Self)
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ConniveCreature))
        return driver
    }

    /**
     * GIVEN a player controls a creature targeted to connive
     * AND the library's top card is a known nonland card (draw is deterministic)
     * AND the player's hand also contains a nonland card to discard
     * WHEN the engine resolves a ConniveEffect targeting that creature
     *   with the player choosing to discard the nonland card from hand
     * THEN the player has drawn exactly one card
     * AND the chosen nonland card has moved from hand to graveyard
     * AND the targeted creature has exactly one additional +1/+1 counter
     */
    test("connive draws one card, discard of nonland gives targeted creature a +1/+1 counter") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Forest" to 30),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!

        // GIVEN: a known nonland card on top of the library (deterministic draw)
        driver.putCardOnTopOfLibrary(player, "Grizzly Bears")

        // GIVEN: a nonland card in hand that the player will choose to discard
        val nonlandToDiscard = driver.putCardInHand(player, "Grizzly Bears")
        val handSizeBefore = driver.getHandSize(player)

        // GIVEN: the connive creature on the battlefield
        val creature = driver.putCreatureOnBattlefield(player, "Connive Creature")
        driver.removeSummoningSickness(creature)
        val countersBefore = driver.state.getEntity(creature)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        // WHEN: activate the connive ability — it draws first, then pauses for discard selection
        val activateResult = driver.submit(
            ActivateAbility(playerId = player, sourceId = creature, abilityId = conniveAbilityId)
        )
        activateResult.isSuccess shouldBe true
        driver.bothPass()

        // The engine drew one card before pausing — hand grew by 1
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.getHandSize(player) shouldBe handSizeBefore + 1

        // WHEN: player chooses to discard the nonland card
        val decision = driver.pendingDecision as SelectCardsDecision
        driver.submitDecision(
            player,
            CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(nonlandToDiscard))
        )

        driver.isPaused shouldBe false

        // THEN: the chosen nonland card is now in the graveyard
        driver.state.getGraveyard(player).contains(nonlandToDiscard) shouldBe true

        // THEN: hand size returned to what it was (drew 1, discarded 1)
        driver.getHandSize(player) shouldBe handSizeBefore

        // THEN: the creature gained exactly one +1/+1 counter
        val countersAfter = driver.state.getEntity(creature)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        countersAfter shouldBe countersBefore + 1
    }
})
