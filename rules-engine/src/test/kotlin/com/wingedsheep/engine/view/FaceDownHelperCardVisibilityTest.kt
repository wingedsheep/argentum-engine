package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.MANIFEST_HELPER_CARD_IMAGE_URI
import com.wingedsheep.sdk.scripting.effects.MORPH_HELPER_CARD_IMAGE_URI
import com.wingedsheep.sdk.scripting.effects.MYSTERIOUS_CREATURE_HELPER_CARD_IMAGE_URI
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Which helper card a face-down object is *drawn* as.
 *
 * Paper Magic prints one per mechanic — the helmeted Morph token, the Manifest token, and
 * "A Mysterious Creature" for both disguise (CR 702.168) and cloak (CR 701.58) — and the client
 * shows that card's art in place of the hidden one. The mode travels to the client on
 * [ClientCard.faceDownMode], and the masked view (what an opponent or spectator gets, where the
 * real art must never be sent) carries the helper card's own image in [ClientCard.imageUri].
 *
 * The interesting case is a spell **still on the stack**: `FaceDownModeComponent` is stamped when
 * the permanent enters the battlefield, so while the spell is being cast the mode has to be derived
 * from the keyword it was cast under. Without that, a creature cast with disguise was drawn as a
 * *morph* for as long as it sat on the stack, and only became "A Mysterious Creature" on resolution.
 */
class FaceDownHelperCardVisibilityTest : FunSpec({

    val disguiseBear = card("Disguised Bear") {
        manaCost = "{2}{B}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        disguise = "{2}{B}"
    }

    val morphBear = card("Morphing Bear") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        morph = "{2}{G}"
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(disguiseBear, morphBear))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry)

    /** Cast [cardName] face down for {3} and leave the spell on the stack. */
    fun GameTestDriver.castFaceDown(playerId: EntityId, cardName: String): EntityId {
        val card = putCardInHand(playerId, cardName)
        giveMana(playerId, Color.BLACK, 3)
        submit(
            CastSpell(
                playerId = playerId,
                cardId = card,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).isSuccess shouldBe true
        return card
    }

    /** Put [cardName] on the battlefield face down under [mode], as a real face-down entry does. */
    fun GameTestDriver.putFaceDown(playerId: EntityId, cardName: String, mode: FaceDownMode): EntityId {
        val id = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = cardRegistry.requireCard(cardName)
        replaceState(
            state.updateEntity(id) { container ->
                var c = container.with(FaceDownComponent).with(FaceDownModeComponent(mode))
                FaceDownTurnUp.dataFor(cardDef, cardName, mode)?.let { c = c.with(it) }
                c
            }
        )
        return id
    }

    context("a creature cast with disguise") {

        test("is A Mysterious Creature to its opponent while the spell is on the stack") {
            val d = driver()
            val caster = d.activePlayer!!
            val opponent = if (caster == d.player1) d.player2 else d.player1
            val spell = d.castFaceDown(caster, "Disguised Bear")

            val view = transformer(d).transform(d.state, viewingPlayerId = opponent)
            val card = view.cards[spell].shouldNotBeNull()
            card.isFaceDown shouldBe true
            card.faceDownMode shouldBe FaceDownMode.DISGUISE.name
            withClue("the opponent must get the helper card's art, never the hidden card's") {
                card.imageUri shouldBe MYSTERIOUS_CREATURE_HELPER_CARD_IMAGE_URI
            }
            card.name shouldBe "Face-down creature"
        }

        test("is A Mysterious Creature to its caster while the spell is on the stack") {
            val d = driver()
            val caster = d.activePlayer!!
            val spell = d.castFaceDown(caster, "Disguised Bear")

            val view = transformer(d).transform(d.state, viewingPlayerId = caster)
            val card = view.cards[spell].shouldNotBeNull()
            card.isFaceDown shouldBe true
            // The caster's own view keeps the real card's name and art (they may look at it); the
            // mode is what tells the client to draw the helper card instead.
            card.faceDownMode shouldBe FaceDownMode.DISGUISE.name
            card.name shouldBe "Disguised Bear"
        }

        test("is still A Mysterious Creature once it has resolved") {
            val d = driver()
            val caster = d.activePlayer!!
            val opponent = if (caster == d.player1) d.player2 else d.player1
            val spell = d.castFaceDown(caster, "Disguised Bear")
            d.bothPass()

            val view = transformer(d).transform(d.state, viewingPlayerId = opponent)
            val card = view.cards[spell].shouldNotBeNull()
            card.faceDownMode shouldBe FaceDownMode.DISGUISE.name
            card.imageUri shouldBe MYSTERIOUS_CREATURE_HELPER_CARD_IMAGE_URI
        }
    }

    context("the other face-down mechanics keep their own helper card") {

        test("a morph spell on the stack is the Morph token, not A Mysterious Creature") {
            val d = driver()
            val caster = d.activePlayer!!
            val opponent = if (caster == d.player1) d.player2 else d.player1
            val spell = d.castFaceDown(caster, "Morphing Bear")

            val view = transformer(d).transform(d.state, viewingPlayerId = opponent)
            val card = view.cards[spell].shouldNotBeNull()
            card.faceDownMode shouldBe FaceDownMode.MORPH.name
            card.imageUri shouldBe MORPH_HELPER_CARD_IMAGE_URI
        }

        test("a cloaked permanent is A Mysterious Creature — the card covers both mechanics") {
            val d = driver()
            val player = d.activePlayer!!
            val opponent = if (player == d.player1) d.player2 else d.player1
            val permanent = d.putFaceDown(player, "Grizzly Bears", FaceDownMode.CLOAK)

            val view = transformer(d).transform(d.state, viewingPlayerId = opponent)
            val card = view.cards[permanent].shouldNotBeNull()
            card.faceDownMode shouldBe FaceDownMode.CLOAK.name
            card.imageUri shouldBe MYSTERIOUS_CREATURE_HELPER_CARD_IMAGE_URI
        }

        test("a manifested permanent is the Manifest token") {
            val d = driver()
            val player = d.activePlayer!!
            val opponent = if (player == d.player1) d.player2 else d.player1
            val permanent = d.putFaceDown(player, "Grizzly Bears", FaceDownMode.MANIFEST)

            val view = transformer(d).transform(d.state, viewingPlayerId = opponent)
            val card = view.cards[permanent].shouldNotBeNull()
            card.faceDownMode shouldBe FaceDownMode.MANIFEST.name
            card.imageUri shouldBe MANIFEST_HELPER_CARD_IMAGE_URI
        }

        test("a face-down permanent with no mode recorded falls back to the Morph token") {
            val d = driver()
            val player = d.activePlayer!!
            val opponent = if (player == d.player1) d.player2 else d.player1
            val permanent = d.putCreatureOnBattlefield(player, "Grizzly Bears")
            d.replaceState(d.state.updateEntity(permanent) { it.with(FaceDownComponent) })

            val view = transformer(d).transform(d.state, viewingPlayerId = opponent)
            val card = view.cards[permanent].shouldNotBeNull()
            card.faceDownMode shouldBe null
            card.imageUri shouldBe MORPH_HELPER_CARD_IMAGE_URI
        }
    }
})
