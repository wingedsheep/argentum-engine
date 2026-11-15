package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.isSatisfiedBy
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.OvergrownZealot
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Overgrown Zealot's second mana ability — "Add two mana of any one color. Spend this
 * mana only to turn permanents face up." ([ManaRestriction.TurnPermanentsFaceUpOnly]).
 */
class OvergrownZealotScenarioTest : FunSpec({

    // A morph creature whose turn-face-up cost is the mana {2}{B}.
    val manaMorphTester = card("Mana Morph Tester") {
        manaCost = "{3}{B}"
        typeLine = "Creature — Zombie"
        power = 2
        toughness = 2
        morph = "{2}{B}"
    }

    val allCards = TestCards.all + listOf(OvergrownZealot, manaMorphTester)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    fun GameTestDriver.putFaceDownCreature(playerId: EntityId, cardName: String): EntityId {
        val creatureId = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = allCards.first { it.name == cardName }
        val morphAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Morph>().firstOrNull()
        replaceState(state.updateEntity(creatureId) { container ->
            var c = container.with(FaceDownComponent)
            if (morphAbility != null) {
                c = c.with(MorphDataComponent(morphAbility.morphCost, cardDef.name))
            }
            c
        })
        removeSummoningSickness(creatureId)
        return creatureId
    }

    test("TurnPermanentsFaceUpOnly mana satisfies only the turn-face-up context") {
        val faceUp = SpellPaymentContext(isTurnFaceUpAction = true)
        val unlock = SpellPaymentContext(isUnlockDoorAction = true)
        val creatureSpell = SpellPaymentContext(isCreature = true, cardTypes = setOf(CardType.CREATURE))

        ManaRestriction.TurnPermanentsFaceUpOnly().isSatisfiedBy(faceUp) shouldBe true
        ManaRestriction.TurnPermanentsFaceUpOnly().isSatisfiedBy(unlock) shouldBe false
        ManaRestriction.TurnPermanentsFaceUpOnly().isSatisfiedBy(creatureSpell) shouldBe false
    }

    test("restricted face-up mana in the pool pays a face-down creature's morph cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Morph cost is {2}{B}. Give exactly that as turn-face-up-only mana.
        val cutthroat = driver.putFaceDownCreature(player, "Mana Morph Tester")
        driver.giveRestrictedMana(player, Color.BLACK, 1, ManaRestriction.TurnPermanentsFaceUpOnly())
        driver.giveRestrictedMana(player, null, 2, ManaRestriction.TurnPermanentsFaceUpOnly())

        val result = driver.submit(
            TurnFaceUp(playerId = player, sourceId = cutthroat, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.error shouldBe null

        // Flipped face up, and all the restricted mana was consumed.
        driver.state.getEntity(cutthroat)?.get<FaceDownComponent>() shouldBe null
        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        pool.restrictedMana.size shouldBe 0
    }

    test("card definition exposes two mana abilities, the second restricted to turn-face-up") {
        OvergrownZealot.activatedAbilities.size shouldBe 2
        OvergrownZealot.activatedAbilities.all { it.isManaAbility } shouldBe true
    }
})
