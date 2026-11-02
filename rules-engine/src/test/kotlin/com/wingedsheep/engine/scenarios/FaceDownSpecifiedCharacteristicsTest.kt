package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A face-down battlefield entry whose effect specifies the face-down characteristics instead of
 * the plain 2/2 (CR 708.2a, "unless otherwise specified by the effect that put it onto the
 * battlefield face down") — `entersWith` on a face-down move. Tezzeret, Cruel Machinist's −7:
 * "They're 5/5 artifact creatures."
 */
class FaceDownSpecifiedCharacteristicsTest : FunSpec({
    val putFaceDown = card("Face-Down Artifact Maker") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val chosen = chooseAnyNumber(from = hand, prompt = "Choose cards")
                move(
                    chosen,
                    CardDestination.ToZone(Zone.BATTLEFIELD, Player.You),
                    faceDown = FaceDownMode.HIDDEN,
                    entersWith = EntersWithCharacteristics(
                        addedCardTypes = setOf(CardType.ARTIFACT), power = 5, toughness = 5,
                    ),
                )
            }
        }
    }
    val morphling = card("Specified Morphling") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Elf"
        power = 1; toughness = 3
        morph = "{G}"
    }
    val plain = card("Specified Plain Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2; toughness = 2
    }
    val copier = card("Specified Copier") {
        manaCost = "{U}"; typeLine = "Creature — Shapeshifter"; power = 0; toughness = 0
        replacementEffect(EntersAsCopy())
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(putFaceDown, morphling, plain, copier))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun putDown(d: GameTestDriver, vararg names: String): List<EntityId> {
        val cards = names.map { d.putCardInHand(d.player1, it) }
        val spell = d.putCardInHand(d.player1, putFaceDown.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, spell).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitCardSelection(d.player1, cards).error shouldBe null
        return cards
    }

    test("the face-down permanents are 5/5 colorless artifact creatures with no name or subtypes") {
        val d = driver()
        val (bear) = putDown(d, plain.name)
        d.state.getBattlefield().contains(bear) shouldBe true
        d.state.getEntity(bear)!!.has<FaceDownComponent>() shouldBe true
        val projected = d.state.projectedState
        projected.getPower(bear) shouldBe 5
        projected.getToughness(bear) shouldBe 5
        projected.hasType(bear, "ARTIFACT") shouldBe true
        projected.isCreature(bear) shouldBe true
        projected.getSubtypes(bear) shouldBe emptySet()
        projected.getColors(bear) shouldBe emptySet()
        // No turn-up procedure: the mode offers none and the card prints neither morph nor disguise.
        d.state.getEntity(bear)!!.has<MorphDataComponent>() shouldBe false
    }

    test("both players see a face-down 5/5 artifact creature") {
        val d = driver()
        val (bear) = putDown(d, plain.name)
        val transformer = ClientStateTransformer(
            cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = d.cardRegistry))
        val view = transformer.transform(d.state, d.player2).cards[bear]!!
        view.isFaceDown shouldBe true
        view.typeLine shouldBe "Artifact Creature"
        view.cardTypes shouldBe setOf("ARTIFACT", "CREATURE")
        view.power shouldBe 5
        view.basePower shouldBe 5
        view.baseToughness shouldBe 5

        // Its controller sees the card, measured against the face-down 5/5 artifact creature it
        // is — not as a 2/2 Bear pumped +3/+3 and granted the artifact type.
        val own = transformer.transform(d.state, d.player1).cards[bear]!!
        own.basePower shouldBe 5
        own.baseToughness shouldBe 5
        own.grantedCardTypes shouldBe emptySet()
    }

    test("a copy of one is a face-up 5/5 artifact creature with no other characteristics") {
        val d = driver()
        val (bear) = putDown(d, plain.name)
        val copy = d.putCardInHand(d.player1, copier.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, copy).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitCardSelection(d.player1, listOf(bear)).error shouldBe null
        val c = d.state.getEntity(copy)!!.get<CardComponent>()!!
        c.name shouldBe ""
        c.typeLine shouldBe TypeLine.parse("Artifact Creature")
        c.baseStats!!.basePower shouldBe 5
        d.state.getEntity(copy)!!.has<FaceDownComponent>() shouldBe false
        d.state.projectedState.getPower(copy) shouldBe 5
        d.state.projectedState.hasType(copy, "ARTIFACT") shouldBe true
    }

    test("a card with morph can still be turned face up, ending the 5/5 artifact effect") {
        val d = driver()
        val (elf) = putDown(d, morphling.name)
        d.state.getEntity(elf)!!.get<MorphDataComponent>()!!.hasMorphProcedure shouldBe true
        d.giveMana(d.player1, Color.GREEN, 1)
        d.submit(TurnFaceUp(d.player1, elf)).error shouldBe null
        d.state.getEntity(elf)!!.has<FaceDownComponent>() shouldBe false
        d.state.getEntity(elf)!!.has<FaceDownModeComponent>() shouldBe false
        val projected = d.state.projectedState
        projected.getPower(elf) shouldBe 1
        projected.getToughness(elf) shouldBe 3
        projected.hasType(elf, "ARTIFACT") shouldBe false
        projected.getSubtypes(elf) shouldBe setOf("Elf")
    }
})

