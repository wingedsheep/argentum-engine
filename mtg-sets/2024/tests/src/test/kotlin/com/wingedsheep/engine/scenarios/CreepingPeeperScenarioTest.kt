package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.isSatisfiedBy
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.CreepingPeeper
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
 * Tests for Creeping Peeper's mana ability — "Add {U}. Spend this mana only to cast an enchantment
 * spell, unlock a door, or turn a permanent face up." Modeled as a [ManaRestriction.AnyOf] of three
 * atomic restrictions.
 */
class CreepingPeeperScenarioTest : FunSpec({

    val peeperRestriction = ManaRestriction.AnyOf(
        listOf(
            ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                cardType = CardType.ENCHANTMENT,
                allowSpells = true,
                allowAbilities = false,
            ),
            ManaRestriction.UnlockDoorOnly,
            ManaRestriction.TurnPermanentsFaceUpOnly(),
        ),
    )

    // A morph creature whose turn-face-up cost is the mana {2}{B}.
    val manaMorphTester = card("Mana Morph Tester") {
        manaCost = "{3}{B}"
        typeLine = "Creature — Zombie"
        power = 2
        toughness = 2
        morph = "{2}{B}"
    }

    val allCards = TestCards.all + listOf(CreepingPeeper, manaMorphTester)

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

    test("the AnyOf restriction satisfies enchantment spells, unlock, and turn-face-up — but not creatures") {
        val enchantmentSpell = SpellPaymentContext(cardTypes = setOf(CardType.ENCHANTMENT))
        val unlock = SpellPaymentContext(isUnlockDoorAction = true)
        val faceUp = SpellPaymentContext(isTurnFaceUpAction = true)
        val creatureSpell = SpellPaymentContext(isCreature = true, cardTypes = setOf(CardType.CREATURE))
        val artifactSpell = SpellPaymentContext(cardTypes = setOf(CardType.ARTIFACT))

        peeperRestriction.isSatisfiedBy(enchantmentSpell) shouldBe true
        peeperRestriction.isSatisfiedBy(unlock) shouldBe true
        peeperRestriction.isSatisfiedBy(faceUp) shouldBe true
        peeperRestriction.isSatisfiedBy(creatureSpell) shouldBe false
        peeperRestriction.isSatisfiedBy(artifactSpell) shouldBe false
    }

    test("Creeping Peeper's restricted {U} can pay toward turning a permanent face up") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Morph cost is {2}{B}. Give 1 blue (Peeper) + the rest as the same multi-option
        // restriction, all spendable on the turn-face-up action.
        val cutthroat = driver.putFaceDownCreature(player, "Mana Morph Tester")
        driver.giveRestrictedMana(player, Color.BLUE, 1, peeperRestriction)
        driver.giveRestrictedMana(player, Color.BLACK, 1, peeperRestriction)
        driver.giveRestrictedMana(player, null, 1, peeperRestriction)

        val result = driver.submit(
            TurnFaceUp(playerId = player, sourceId = cutthroat, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.error shouldBe null
        driver.state.getEntity(cutthroat)?.get<FaceDownComponent>() shouldBe null
        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        pool.restrictedMana.size shouldBe 0
    }

    test("card definition exposes one mana ability") {
        CreepingPeeper.activatedAbilities.size shouldBe 1
        CreepingPeeper.activatedAbilities.single().isManaAbility shouldBe true
    }
})
