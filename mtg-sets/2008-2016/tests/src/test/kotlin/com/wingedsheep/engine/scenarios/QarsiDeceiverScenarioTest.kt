package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.isSatisfiedBy
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.QarsiDeceiver
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Qarsi Deceiver — "{T}: Add {C}. Spend this mana only to cast a face-down creature spell, pay a
 * mana cost to turn a manifested creature face up, or pay a morph cost."
 *
 * Pins each route the clause names (face-down cast, manifest turn-up, morph turn-up — including a
 * manifested card that prints morph, CR 701.40c) and the routes it doesn't (a cloaked or disguised
 * permanent's turn-up, a spell cast face up).
 */
class QarsiDeceiverScenarioTest : FunSpec({

    val restriction = (QarsiDeceiver.activatedAbilities.single().effect as AddColorlessManaEffect).restriction!!

    // Each tester turns up for exactly {1}, so only the spend restriction decides the outcome.
    val morphTester = card("Qarsi Morph Tester") {
        manaCost = "{3}{U}"
        typeLine = "Creature — Snake"
        power = 3
        toughness = 3
        morph = "{1}"
    }
    val cheapCreature = card("Qarsi Cheap Creature") {
        manaCost = "{1}"
        typeLine = "Creature — Snake"
        power = 1
        toughness = 1
    }
    val disguiseTester = card("Qarsi Disguise Tester") {
        manaCost = "{3}{U}"
        typeLine = "Creature — Snake"
        power = 3
        toughness = 3
        disguise = "{1}"
    }

    val allCards = TestCards.all + listOf(QarsiDeceiver, morphTester, cheapCreature, disguiseTester)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.putFaceDown(playerId: EntityId, cardName: String, mode: FaceDownMode): EntityId {
        val id = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = allCards.first { it.name == cardName }
        replaceState(
            state.updateEntity(id) { c ->
                c.with(FaceDownComponent)
                    .with(FaceDownModeComponent(mode))
                    .with(FaceDownTurnUp.dataFor(cardDef, cardDef.name, mode)!!)
            }
        )
        removeSummoningSickness(id)
        return id
    }

    fun GameTestDriver.tapQarsi(playerId: EntityId) {
        val qarsi = putCreatureOnBattlefield(playerId, "Qarsi Deceiver")
        removeSummoningSickness(qarsi)
        val abilityId = QarsiDeceiver.activatedAbilities.single().id
        submit(ActivateAbility(playerId, qarsi, abilityId)).error shouldBe null
    }

    fun GameTestDriver.restrictedLeft(playerId: EntityId): Int =
        (state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()).restrictedMana.size

    test("the clause admits each printed route and nothing else") {
        val face = SpellPaymentContext::turnFaceUp
        restriction.isSatisfiedBy(SpellPaymentContext.faceDownCast()) shouldBe true
        restriction.isSatisfiedBy(face(FaceDownMode.MORPH, FaceDownMode.MORPH)) shouldBe true
        restriction.isSatisfiedBy(face(FaceDownMode.MANIFEST, FaceDownMode.MANIFEST)) shouldBe true
        withClue("a manifested card that prints morph, turned up for its morph cost") {
            restriction.isSatisfiedBy(face(FaceDownMode.MORPH, FaceDownMode.MANIFEST)) shouldBe true
        }
        withClue("a cloaked creature is not a manifested one") {
            restriction.isSatisfiedBy(face(FaceDownMode.CLOAK, FaceDownMode.CLOAK)) shouldBe false
        }
        withClue("a disguise cost is not a morph cost") {
            restriction.isSatisfiedBy(face(FaceDownMode.DISGUISE, FaceDownMode.DISGUISE)) shouldBe false
        }
        restriction.isSatisfiedBy(SpellPaymentContext(isCreature = true, cardTypes = setOf(CardType.CREATURE))) shouldBe false
        restriction.isSatisfiedBy(SpellPaymentContext(isAbilityActivation = true)) shouldBe false
    }

    test("tapping Qarsi Deceiver pays a face-down creature's morph cost") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val hidden = driver.putFaceDown(player, "Qarsi Morph Tester", FaceDownMode.MORPH)
        driver.tapQarsi(player)
        driver.restrictedLeft(player) shouldBe 1

        driver.submit(TurnFaceUp(player, hidden, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        driver.state.getEntity(hidden)?.get<FaceDownComponent>() shouldBe null
        driver.restrictedLeft(player) shouldBe 0
    }

    test("the mana pays the mana cost to turn a manifested creature face up") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val hidden = driver.putFaceDown(player, "Qarsi Cheap Creature", FaceDownMode.MANIFEST)
        driver.giveRestrictedMana(player, null, 1, restriction)

        driver.legalActions(player).any { (it.action as? TurnFaceUp)?.sourceId == hidden } shouldBe true
        driver.submit(TurnFaceUp(player, hidden, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        driver.state.getEntity(hidden)?.get<FaceDownComponent>() shouldBe null
    }

    test("the mana can't turn a cloaked creature face up") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val hidden = driver.putFaceDown(player, "Qarsi Cheap Creature", FaceDownMode.CLOAK)
        driver.giveRestrictedMana(player, null, 1, restriction)

        driver.legalActions(player).any { (it.action as? TurnFaceUp)?.sourceId == hidden } shouldBe false
        driver.submit(TurnFaceUp(player, hidden, paymentStrategy = PaymentStrategy.FromPool)).error shouldNotBe null
        driver.state.getEntity(hidden)?.get<FaceDownComponent>() shouldNotBe null
        driver.restrictedLeft(player) shouldBe 1
    }

    test("the mana can't pay a disguise cost") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val hidden = driver.putFaceDown(player, "Qarsi Disguise Tester", FaceDownMode.DISGUISE)
        driver.giveRestrictedMana(player, null, 1, restriction)

        driver.legalActions(player).any { (it.action as? TurnFaceUp)?.sourceId == hidden } shouldBe false
        driver.submit(TurnFaceUp(player, hidden, paymentStrategy = PaymentStrategy.FromPool)).error shouldNotBe null
        driver.state.getEntity(hidden)?.get<FaceDownComponent>() shouldNotBe null
    }

    test("the mana casts a morph card face down but not a creature spell face up") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val morphCard = driver.putCardInHand(player, "Qarsi Morph Tester")
        val plainCard = driver.putCardInHand(player, "Qarsi Cheap Creature")
        driver.giveRestrictedMana(player, null, 3, restriction)

        withClue("{1} creature spell cast face up") {
            driver.submit(CastSpell(player, plainCard, paymentStrategy = PaymentStrategy.FromPool)).error shouldNotBe null
        }
        driver.submit(
            CastSpell(player, morphCard, castFaceDown = true, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.stackSize shouldBe 1
        driver.restrictedLeft(player) shouldBe 0
    }
})
