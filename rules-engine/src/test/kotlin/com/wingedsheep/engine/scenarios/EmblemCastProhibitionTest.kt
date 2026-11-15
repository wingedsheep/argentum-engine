package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A [PlayersCantCastSpells] held by an emblem ("Your opponents can't cast noncreature spells" —
 * Narset Transcendent's −9). The emblem sits in no zone, so the cast-legality chokepoint must read
 * it beside the battlefield, with the emblem's controller as "you". Enumeration and the cast
 * validator must agree, so each case checks both.
 */
class EmblemCastProhibitionTest : FunSpec({
    val banEmblem = card("Cast Ban Emblem Instant") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            effect = Effects.CreatePermanentEmblem(
                ownedStaticAbilities = listOf(
                    PlayersCantCastSpells(affected = Player.EachOpponent, spellFilter = GameObjectFilter.Noncreature)
                ),
                emblemDescription = "Your opponents can't cast noncreature spells."
            )
        }
    }
    val cantrip = card("Emblem Ban Cantrip") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }
    val golem = card("Emblem Ban Golem") {
        manaCost = "{0}"; typeLine = "Artifact Creature — Golem"; power = 2; toughness = 2
    }
    val trinket = card("Emblem Ban Trinket") {
        manaCost = "{0}"; typeLine = "Artifact"
    }

    fun GameTestDriver.offers(player: EntityId, cardId: EntityId): List<CastSpell> =
        legalActions(player).mapNotNull { it.action as? CastSpell }.filter { it.cardId == cardId }

    fun GameTestDriver.resolveAll() {
        repeat(6) { if (state.stack.isNotEmpty()) bothPass() }
        state.stack.isEmpty() shouldBe true
    }

    /**
     * The non-active player ([holder]) gets the emblem at instant speed during [active]'s main
     * phase, so [active] is left at sorcery speed under an opposing emblem.
     */
    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(banEmblem, cantrip, golem, trinket))
        d.initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = d.activePlayer!!
        val holder = d.getOpponent(active)
        d.submitSuccess(CastSpell(active, d.putCardInHand(active, cantrip.name)))
        d.passPriority(active)
        d.submitSuccess(CastSpell(holder, d.putCardInHand(holder, banEmblem.name)))
        d.resolveAll()
        d.activePlayer shouldBe active
        return Triple(d, active, holder)
    }

    test("the emblem's opponent can't cast a noncreature spell, from enumeration or the validator") {
        val (d, active, _) = setup()
        val artifact = d.putCardInHand(active, trinket.name)
        val instant = d.putCardInHand(active, cantrip.name)

        d.offers(active, artifact).isEmpty() shouldBe true
        d.offers(active, instant).isEmpty() shouldBe true
        d.submitExpectFailure(CastSpell(active, artifact))
        d.submitExpectFailure(CastSpell(active, instant))
    }

    test("the emblem's opponent can still cast a creature spell") {
        val (d, active, _) = setup()
        val creature = d.putCardInHand(active, golem.name)

        d.offers(active, creature).isNotEmpty() shouldBe true
        d.submitSuccess(CastSpell(active, creature))
    }

    test("the emblem's controller is not bound by it") {
        val (d, active, holder) = setup()
        d.submitSuccess(CastSpell(active, d.putCardInHand(active, golem.name)))
        d.passPriority(active)

        val instant = d.putCardInHand(holder, cantrip.name)
        d.offers(holder, instant).isNotEmpty() shouldBe true
        d.submitSuccess(CastSpell(holder, instant))
    }
})
