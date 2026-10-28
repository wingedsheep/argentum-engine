package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.state.components.battlefield.EncodedOnComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cipher (CR 702.99). As a cipher spell represented by a card resolves, just after its other
 * effects, its controller may exile it encoded on a creature they control (702.99a); for as long as
 * it stays encoded — the card stays exiled and the creature stays on the battlefield (702.99c) —
 * the creature has "Whenever this creature deals combat damage to a player, you may copy the
 * encoded card and you may cast the copy without paying its mana cost."
 *
 * The test spell gains its caster 2 life, so the original resolution and every copy show on the
 * life total.
 */
class CipherMechanicTest : FunSpec({

    val cipherSpell = card("Test Cipher Insight") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        keywords(Keyword.CIPHER)
        spell { effect = Effects.GainLife(2) }
    }
    val trinket = card("Test Cipher Trinket") { typeLine = "Artifact" }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(cipherSpell, trinket))
        initMirrorMatch(deck = Deck.of("Island" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.creature(owner: EntityId): EntityId =
        putCreatureOnBattlefield(owner, "Centaur Courser").also { removeSummoningSickness(it) }

    /** Cast the cipher spell and resolve it up to the encode choice. */
    fun GameTestDriver.castCipher(player: EntityId): EntityId {
        giveMana(player, Color.BLUE, 1)
        val spell = putCardInHand(player, cipherSpell.name)
        castSpell(player, spell)
        bothPass()
        return spell
    }

    /** Run [effect] as resolved by a third permanent of the active player's (a lose-all or a steal). */
    fun GameTestDriver.resolveFromTrinket(effect: Effect, target: EntityId, controller: EntityId = activePlayer!!) {
        val source = putPermanentOnBattlefield(controller, trinket.name)
        val result = services.effectExecutorRegistry.execute(
            state, effect,
            EffectContext(sourceId = source, controllerId = controller, targets = listOf(ChosenTarget.Permanent(target)))
        )
        result.error shouldBe null
        replaceState(result.state)
    }

    fun GameTestDriver.encodedOn(card: EntityId): EntityId? =
        state.getEntity(card)?.get<EncodedOnComponent>()?.creatureId

    /** Attack [defender] with [attacker], no blocks, and stop at the first decision (or postcombat main). */
    fun GameTestDriver.attackUnblocked(attacker: EntityId, attackerController: EntityId, defender: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(attackerController, listOf(attacker), defender)
        declareNoBlockers(defender)
        var guard = 0
        while (guard++ < 40 && state.step != Step.POSTCOMBAT_MAIN && pendingDecision == null) bothPass()
    }

    test("resolving, the controller may encode the card on a creature: it goes from the stack to exile") {
        val d = driver(); val me = d.activePlayer!!
        val bear = d.creature(me)
        val life = d.getLifeTotal(me)

        val spell = d.castCipher(me)
        // The spell's own effect has already happened when the choice comes (the ruling).
        d.getLifeTotal(me) shouldBe life + 2
        val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        choice.options shouldBe listOf(bear)
        d.submitCardSelection(me, listOf(bear))

        d.getExile(me).contains(spell).shouldBeTrue()
        d.getGraveyard(me).contains(spell).shouldBeFalse()
        d.encodedOn(spell) shouldBe bear
    }

    test("declining the encode puts the card into its owner's graveyard as usual") {
        val d = driver(); val me = d.activePlayer!!
        d.creature(me)
        val spell = d.castCipher(me)
        d.submitCardSelection(me, emptyList())

        d.getGraveyard(me).contains(spell).shouldBeTrue()
        d.getExile(me).contains(spell).shouldBeFalse()
    }

    test("with no creature to encode on, nothing is asked and the card goes to the graveyard") {
        val d = driver(); val me = d.activePlayer!!
        d.creature(d.getOpponent(me)) // only an opponent's creature — cipher is "a creature you control"
        val spell = d.castCipher(me)

        d.pendingDecision shouldBe null
        d.getGraveyard(me).contains(spell).shouldBeTrue()
    }

    test("combat damage to a player lets the controller cast a free copy; the original stays encoded") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val bear = d.creature(me)
        val spell = d.castCipher(me)
        d.submitCardSelection(me, listOf(bear))
        val life = d.getLifeTotal(me)

        d.attackUnblocked(bear, me, opp)
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(me, true)
        d.bothPass() // resolve the copy

        d.getLifeTotal(me) shouldBe life + 2
        // A copy of a cipher spell isn't represented by a card, so it never asks to encode.
        d.pendingDecision shouldBe null
        d.encodedOn(spell) shouldBe bear
        d.getExile(me).count { d.getCardName(it) == cipherSpell.name } shouldBe 1
        d.getGraveyard(me).any { d.getCardName(it) == cipherSpell.name }.shouldBeFalse()
    }

    test("declining the copy casts nothing and leaves no copy behind") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val bear = d.creature(me)
        d.castCipher(me)
        d.submitCardSelection(me, listOf(bear))
        val life = d.getLifeTotal(me)

        d.attackUnblocked(bear, me, opp)
        d.submitYesNo(me, false)
        d.bothPass()

        d.getLifeTotal(me) shouldBe life
        d.getExile(me).count { d.getCardName(it) == cipherSpell.name } shouldBe 1
    }

    test("when the creature leaves the battlefield the card stays exiled but is no longer encoded") {
        val d = driver(); val me = d.activePlayer!!
        val bear = d.creature(me)
        val spell = d.castCipher(me)
        d.submitCardSelection(me, listOf(bear))

        d.resolveFromTrinket(Effects.ReturnToHand(EffectTarget.ContextTarget(0)), bear)
        d.getExile(me).contains(spell).shouldBeTrue()
        d.state.grantedTriggeredAbilities.none {
            it.entityId == bear && GrantDurationGate.triggeredGrantLive(d.state, it)
        }.shouldBeTrue()
    }

    test("when the card leaves exile it is no longer encoded and the creature's trigger is gone") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val bear = d.creature(me)
        val spell = d.castCipher(me)
        d.submitCardSelection(me, listOf(bear))

        d.resolveFromTrinket(Effects.ReturnToHand(EffectTarget.SpecificEntity(spell)), bear)
        d.getHand(me).contains(spell).shouldBeTrue()
        d.state.grantedTriggeredAbilities.none {
            it.entityId == bear && GrantDurationGate.triggeredGrantLive(d.state, it)
        }.shouldBeTrue()

        d.attackUnblocked(bear, me, opp)
        d.pendingDecision shouldBe null
    }

    test("a card that leaves exile and is exiled again is a new object, not encoded") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val bear = d.creature(me)
        val spell = d.castCipher(me)
        d.submitCardSelection(me, listOf(bear))

        d.resolveFromTrinket(Effects.ReturnToHand(EffectTarget.SpecificEntity(spell)), bear)
        d.resolveFromTrinket(Effects.Move(EffectTarget.SpecificEntity(spell), Zone.EXILE), bear)
        d.getExile(me).contains(spell).shouldBeTrue()

        d.attackUnblocked(bear, me, opp)
        d.pendingDecision shouldBe null
    }

    test("a creature that changes control keeps the encoding, and its new controller gets the copy") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val bear = d.creature(me)
        val spell = d.castCipher(me)
        d.submitCardSelection(me, listOf(bear))

        // The opponent takes the creature for good and attacks with it on their turn.
        d.resolveFromTrinket(Effects.GainControl(EffectTarget.ContextTarget(0)), bear, controller = opp)
        d.encodedOn(spell) shouldBe bear
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        d.removeSummoningSickness(bear)
        val oppLife = d.getLifeTotal(opp)

        d.attackUnblocked(bear, opp, me)
        val ask = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        ask.playerId shouldBe opp
        d.submitYesNo(opp, true)
        d.bothPass()

        d.getLifeTotal(opp) shouldBe oppLife + 2
    }

    test("a later 'loses all abilities' stops the trigger; the card stays encoded") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val bear = d.creature(me)
        val spell = d.castCipher(me)
        d.submitCardSelection(me, listOf(bear))

        d.resolveFromTrinket(Effects.RemoveAllAbilities(EffectTarget.ContextTarget(0)), bear)
        d.attackUnblocked(bear, me, opp)

        d.pendingDecision shouldBe null
        d.encodedOn(spell) shouldBe bear
    }

    test("an earlier 'loses all abilities' doesn't remove the ability the encoding grants afterwards") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val bear = d.creature(me)
        d.resolveFromTrinket(Effects.RemoveAllAbilities(EffectTarget.ContextTarget(0)), bear)

        d.castCipher(me)
        d.submitCardSelection(me, listOf(bear))
        d.attackUnblocked(bear, me, opp)

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
    }

    test("a creature with two encoded cards triggers once per card") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val bear = d.creature(me)
        val first = d.castCipher(me)
        d.submitCardSelection(me, listOf(bear))
        val second = d.castCipher(me)
        d.submitCardSelection(me, listOf(bear))
        first shouldNotBe second
        val life = d.getLifeTotal(me)

        d.attackUnblocked(bear, me, opp)
        var copies = 0
        var guard = 0
        while (guard++ < 20 && d.state.step != Step.POSTCOMBAT_MAIN) {
            if (d.pendingDecision is YesNoDecision) { d.submitYesNo(me, true); copies++ } else d.bothPass()
        }

        copies shouldBe 2
        d.getLifeTotal(me) shouldBe life + 4
    }
})
