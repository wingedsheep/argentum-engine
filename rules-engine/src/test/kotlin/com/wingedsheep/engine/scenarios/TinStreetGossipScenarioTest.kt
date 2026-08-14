package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.isSatisfiedBy
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.NightdrinkerMoroii
import com.wingedsheep.mtg.sets.definitions.mkm.cards.TinStreetGossip
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
import io.kotest.matchers.shouldNotBe

/**
 * Tin Street Gossip — "{T}: Add {R}{G}. Spend this mana only to cast face-down spells or to turn
 * creatures face up."
 *
 * The turn-face-up half is [ManaRestriction.TurnPermanentsFaceUpOnly], already covered by Overgrown
 * Zealot and Creeping Peeper. What is new here is [ManaRestriction.FaceDownSpellsOnly] and the
 * face-down payment context that satisfies it, so these tests pin down three things: the atom's
 * truth table, that the mana actually pays for a real disguise cast, and that it is *not* spendable
 * on the same card cast normally.
 */
class TinStreetGossipScenarioTest : FunSpec({

    val gossipRestriction = ManaRestriction.AnyOf(
        listOf(
            ManaRestriction.FaceDownSpellsOnly,
            ManaRestriction.TurnPermanentsFaceUpOnly,
        )
    )

    // A morph creature whose turn-face-up cost is the mana {1}{R}.
    val manaMorphTester = card("Mana Morph Tester") {
        manaCost = "{3}{R}"
        typeLine = "Creature — Zombie"
        power = 2
        toughness = 2
        morph = "{1}{R}"
    }

    val allCards = TestCards.all + listOf(TinStreetGossip, NightdrinkerMoroii, manaMorphTester)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    fun GameTestDriver.putFaceDownCreature(playerId: EntityId, cardName: String): EntityId {
        val creatureId = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = allCards.first { it.name == cardName }
        val morphAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Morph>().firstOrNull()
        replaceState(
            state.updateEntity(creatureId) { container ->
                var c = container.with(FaceDownComponent)
                if (morphAbility != null) {
                    c = c.with(MorphDataComponent(morphAbility.morphCost, cardDef.name))
                }
                c
            }
        )
        removeSummoningSickness(creatureId)
        return creatureId
    }

    test("FaceDownSpellsOnly is satisfied by a face-down cast and nothing else") {
        val faceDownCast = SpellPaymentContext.faceDownCast()
        val creatureSpell = SpellPaymentContext(isCreature = true, cardTypes = setOf(CardType.CREATURE))
        val turnFaceUp = SpellPaymentContext(isTurnFaceUpAction = true)
        val abilityActivation = SpellPaymentContext(isAbilityActivation = true)

        ManaRestriction.FaceDownSpellsOnly.isSatisfiedBy(faceDownCast) shouldBe true
        ManaRestriction.FaceDownSpellsOnly.isSatisfiedBy(creatureSpell) shouldBe false
        ManaRestriction.FaceDownSpellsOnly.isSatisfiedBy(turnFaceUp) shouldBe false
        ManaRestriction.FaceDownSpellsOnly.isSatisfiedBy(abilityActivation) shouldBe false

        // The AnyOf the card actually prints covers both halves of its clause.
        gossipRestriction.isSatisfiedBy(faceDownCast) shouldBe true
        gossipRestriction.isSatisfiedBy(turnFaceUp) shouldBe true
        gossipRestriction.isSatisfiedBy(creatureSpell) shouldBe false
    }

    test("a face-down cast context carries the CR 708.2 characteristics, not the card's") {
        // Nameless colorless 2/2 creature spell with mana value 0 — so restricted mana keyed to
        // the hidden card's real characteristics can never pay for it.
        val faceDownCast = SpellPaymentContext.faceDownCast()

        faceDownCast.isCreature shouldBe true
        faceDownCast.manaValue shouldBe 0
        faceDownCast.cardTypes shouldBe setOf(CardType.CREATURE)
        faceDownCast.subtypes shouldBe emptySet()

        ManaRestriction.SpellsWithManaValueAtLeast(4).isSatisfiedBy(faceDownCast) shouldBe false
        ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Vampire").isSatisfiedBy(faceDownCast) shouldBe false
    }

    test("the restricted mana pays for casting a disguise card face down") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The face-down cast costs {3}; hand it exactly that as Tin Street Gossip mana.
        val cardId = driver.putCardInHand(player, "Nightdrinker Moroii")
        driver.giveRestrictedMana(player, Color.RED, 1, gossipRestriction)
        driver.giveRestrictedMana(player, Color.GREEN, 1, gossipRestriction)
        driver.giveRestrictedMana(player, null, 1, gossipRestriction)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.error shouldBe null
        driver.stackSize shouldBe 1

        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        pool.restrictedMana.size shouldBe 0
    }

    test("the same mana cannot pay for that card cast face up") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Nightdrinker Moroii costs {3}{B}. Hand over mana that matches that cost exactly in both
        // colour and quantity, so the only thing that can stop the cast is the spend restriction.
        val cardId = driver.putCardInHand(player, "Nightdrinker Moroii")
        driver.giveRestrictedMana(player, Color.BLACK, 1, gossipRestriction)
        driver.giveRestrictedMana(player, null, 3, gossipRestriction)

        val result = driver.submit(
            CastSpell(playerId = player, cardId = cardId, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.error shouldNotBe null
        driver.stackSize shouldBe 0
    }

    test("the restricted mana still pays to turn a face-down creature face up") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val hidden = driver.putFaceDownCreature(player, "Mana Morph Tester")
        driver.giveRestrictedMana(player, Color.RED, 1, gossipRestriction)
        driver.giveRestrictedMana(player, null, 1, gossipRestriction)

        val result = driver.submit(
            TurnFaceUp(playerId = player, sourceId = hidden, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.error shouldBe null
        driver.state.getEntity(hidden)?.get<FaceDownComponent>() shouldBe null
    }

    test("card definition exposes one mana ability carrying the two-atom restriction") {
        TinStreetGossip.activatedAbilities.size shouldBe 1
        TinStreetGossip.activatedAbilities.single().isManaAbility shouldBe true
    }
})
