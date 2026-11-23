package com.wingedsheep.engine.handlers.effects.copy

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.permanent.types.EachPermanentBecomesCopyOfTargetExecutor
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopySourceRead
import com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [CopySourceRead] — which object a "becomes a copy of …" source reference names.
 *
 * The triggering object of a zone change into a graveyard is both the permanent that left and the
 * card that arrived (CR 400.7). "A copy of that creature" ([CopySourceRead.PERMANENT]) reads the
 * permanent as it last existed on the battlefield, copy effects included; "a copy of that card"
 * ([CopySourceRead.CARD]) reads the card in the graveyard — its own printed values — and keeps
 * reading it after it has left the graveyard (Lazav, Dimir Mastermind's 2013-01-24 ruling).
 */
class CopySourceReadTest : FunSpec({

    /** Printed 1/3; becomes a copy of Savannah Lions (1/1) on the battlefield. */
    val mimic = card("Test Copy Mimic") {
        manaCost = "{U}"
        typeLine = "Creature — Shapeshifter"
        power = 1
        toughness = 3
    }

    fun copier(name: String, read: CopySourceRead) = card(name) {
        manaCost = "{U}"
        typeLine = "Creature — Shapeshifter"
        power = 0
        toughness = 4
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.nontoken().ownedByOpponent())
                .changesZone(to = Zone.GRAVEYARD)
            effect = Effects.EachPermanentBecomesCopyOfTarget(
                target = EffectTarget.TriggeringEntity,
                affected = EffectTarget.Self,
                sourceRead = read,
            )
        }
    }
    val millOne = card("Test Mill One") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            val player = target(Targets.Player)
            effect = Patterns.Library.mill(1, player)
        }
    }
    val cardCopier = copier("Test Card Copier", CopySourceRead.CARD)
    val permanentCopier = copier("Test Permanent Copier", CopySourceRead.PERMANENT)

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(mimic, millOne, cardCopier, permanentCopier))
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun nameOf(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)!!.get<CardComponent>()!!.name

    /** The opponent's mimic, made a copy of Savannah Lions, then Bolted: its card goes to the graveyard. */
    fun killCopiedMimic(d: GameTestDriver): EntityId {
        val lions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        val mimicId = d.putCreatureOnBattlefield(d.player2, mimic.name)
        val result = EachPermanentBecomesCopyOfTargetExecutor(PredicateEvaluator(cardRegistry = d.cardRegistry), d.cardRegistry)
            .execute(
                d.state,
                EachPermanentBecomesCopyOfTargetEffect(target = EffectTarget.ContextTarget(0), affected = EffectTarget.Self),
                EffectContext(sourceId = mimicId, controllerId = d.player2, targets = listOf(ChosenTarget.Permanent(lions))),
            )
        d.replaceState(result.state)
        nameOf(d, mimicId) shouldBe "Savannah Lions"

        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, targets = listOf(mimicId)).error shouldBe null
        d.bothPass() // Bolt resolves; the copied mimic dies and its card is in the graveyard
        d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD)).contains(mimicId) shouldBe true
        return mimicId
    }

    test("CARD copies the card in the graveyard, not the copy effect it had on the battlefield") {
        val d = driver()
        val copierId = d.putCreatureOnBattlefield(d.player1, cardCopier.name)
        killCopiedMimic(d)
        d.bothPass() // the copier's trigger resolves

        withClue("the graveyard card is the printed 1/3 mimic") {
            nameOf(d, copierId) shouldBe mimic.name
            d.state.projectedState.getPower(copierId) shouldBe 1
            d.state.projectedState.getToughness(copierId) shouldBe 3
        }
    }

    test("PERMANENT copies the creature as it last existed on the battlefield, copy effect included") {
        val d = driver()
        val copierId = d.putCreatureOnBattlefield(d.player1, permanentCopier.name)
        killCopiedMimic(d)
        d.bothPass()

        withClue("the dead permanent was a Savannah Lions copy") {
            nameOf(d, copierId) shouldBe "Savannah Lions"
        }
    }

    test("CARD still copies a card that left the graveyard before resolution, as it last existed there") {
        val d = driver()
        val copierId = d.putCreatureOnBattlefield(d.player1, cardCopier.name)
        val mimicId = killCopiedMimic(d)

        // With the trigger on the stack, the card moves on from the graveyard to exile.
        d.replaceState(
            d.state.removeFromZone(ZoneKey(d.player2, Zone.GRAVEYARD), mimicId)
                .addToZone(ZoneKey(d.player2, Zone.EXILE), mimicId)
        )
        d.bothPass()

        withClue("the card is copied as it last existed in the graveyard") {
            nameOf(d, copierId) shouldBe mimic.name
            d.state.projectedState.getToughness(copierId) shouldBe 3
        }
    }

    test("CARD copies a card milled straight from the library — there is no permanent to read") {
        val d = driver()
        val copierId = d.putCreatureOnBattlefield(d.player1, cardCopier.name)
        val milled = d.putCardOnTopOfLibrary(d.player2, mimic.name)
        val tome = d.putCardInHand(d.player1, millOne.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, tome, targets = listOf(d.player2)).error shouldBe null
        d.bothPass() // mills the mimic
        d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD)).contains(milled) shouldBe true
        d.bothPass()

        nameOf(d, copierId) shouldBe mimic.name
    }
})
