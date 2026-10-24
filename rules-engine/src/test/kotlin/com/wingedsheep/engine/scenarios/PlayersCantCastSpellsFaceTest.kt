package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A filtered [PlayersCantCastSpells] static ("you can't cast permanent spells" — Codie, Vociferous
 * Codex) is judged against the spell as proposed, after its face or alternative cost is chosen
 * (CR 601.2e), exactly like a resolved `CantCastSpellsComponent` ban: an Adventure's instant half,
 * a modal back face and a face-down cast are each their own spell. Enumeration and the cast
 * validator must agree, so every case checks both.
 */
class PlayersCantCastSpellsFaceTest : FunSpec({
    fun warden(name: String, filter: GameObjectFilter, affected: Player = Player.You) = card(name) {
        manaCost = "{0}"; typeLine = "Artifact"
        staticAbility { ability = PlayersCantCastSpells(affected = affected, spellFilter = filter) }
    }
    val permanentWarden = warden("Permanent Cast Warden", GameObjectFilter.Permanent)
    val noncreatureWarden = warden("Noncreature Cast Warden", GameObjectFilter.Noncreature)
    val opponentsPermanentWarden = warden("Opposing Permanent Warden", GameObjectFilter.Permanent, Player.EachOpponent)
    val manaValueWarden = warden("Mana Value Two Warden", GameObjectFilter.Any.manaValue(2))
    val blanketWarden = warden("Blanket Cast Warden", GameObjectFilter.Any)

    val creature = card("Face Ban Creature") {
        manaCost = "{0}"; typeLine = "Artifact Creature — Golem"; power = 2; toughness = 2
    }
    val instant = card("Face Ban Instant") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }
    val adventure = creature.copy(
        name = "Face Ban Adventure", layout = CardLayout.ADVENTURE,
        cardFaces = listOf(CardFace("Face Ban Adventure Spell", ManaCost.ZERO, TypeLine.parse("Instant"),
            script = instant.script))
    )
    val morph = card("Face Ban Morph") {
        manaCost = "{0}"; typeLine = "Enchantment"; morph = "{0}"
    }
    val modal = instant.copy(name = "Face Ban Modal", layout = CardLayout.MODAL_DFC,
        backFace = creature.copy(name = "Face Ban Modal Back"))
    val xInstant = card("Face Ban X Instant") {
        manaCost = "{X}"; typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }

    fun setup(vararg wardens: CardDefinition): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(permanentWarden, noncreatureWarden, opponentsPermanentWarden,
            manaValueWarden, blanketWarden, creature, instant, adventure, morph, modal, xInstant))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        wardens.forEach { putPermanentOnBattlefield(activePlayer!!, it.name) }
    }
    fun offers(d: GameTestDriver, id: EntityId, player: EntityId = d.activePlayer!!): List<CastSpell> =
        d.legalActions(player).mapNotNull { it.action as? CastSpell }.filter { it.cardId == id }

    test("can't cast permanent spells: the creature is withheld and rejected, an instant is not") {
        val d = setup(permanentWarden); val me = d.activePlayer!!
        val body = d.putCardInHand(me, creature.name)
        val spell = d.putCardInHand(me, instant.name)
        offers(d, body).isEmpty() shouldBe true
        d.submitExpectFailure(CastSpell(me, body))
        offers(d, spell).isNotEmpty() shouldBe true
        d.submitSuccess(CastSpell(me, spell))
    }
    test("an Adventure's instant half stays castable while its creature half is prohibited") {
        val d = setup(permanentWarden); val me = d.activePlayer!!
        val id = d.putCardInHand(me, adventure.name)
        offers(d, id).any { it.faceIndex == null } shouldBe false
        val action = offers(d, id).first { it.faceIndex == 0 }
        d.submitExpectFailure(CastSpell(me, id))
        d.submitSuccess(action)
    }
    test("a noncreature prohibition withholds the Adventure but not the creature half") {
        val d = setup(noncreatureWarden); val me = d.activePlayer!!
        val id = d.putCardInHand(me, adventure.name)
        offers(d, id).any { it.faceIndex == 0 } shouldBe false
        d.submitExpectFailure(CastSpell(me, id, faceIndex = 0))
        d.submitSuccess(offers(d, id).first { it.faceIndex == null })
    }
    test("a modal card's instant front stays castable while its creature back is prohibited") {
        val d = setup(permanentWarden); val me = d.activePlayer!!
        val id = d.putCardInHand(me, modal.name)
        offers(d, id).any { it.alternativeCostType == AlternativeCostType.MODAL_BACK_FACE } shouldBe false
        d.submitExpectFailure(CastSpell(me, id, useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.MODAL_BACK_FACE))
        d.submitSuccess(offers(d, id).first { !it.useAlternativeCost })
    }
    test("a noncreature card cast face down is a creature spell, so a noncreature prohibition allows it") {
        val d = setup(noncreatureWarden); val me = d.activePlayer!!
        repeat(3) { d.putLandOnBattlefield(me, "Plains") }
        val id = d.putCardInHand(me, morph.name)
        offers(d, id).any { !it.castFaceDown } shouldBe false
        val action = offers(d, id).first { it.castFaceDown }
        d.submitExpectFailure(CastSpell(me, id))
        d.submitSuccess(action.copy(paymentStrategy = PaymentStrategy.AutoPay))
    }
    test("the prohibition binds only the affected players") {
        val d = setup(opponentsPermanentWarden); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val mine = d.putCardInHand(me, creature.name)
        offers(d, mine).isNotEmpty() shouldBe true
        // The opponent is bound: their creature never surfaces, but their Adventure half does.
        val theirs = d.putCardInHand(opp, adventure.name)
        val theirInstant = d.putCardInHand(opp, instant.name)
        d.passPriority(me)
        d.state.priorityPlayerId shouldBe opp
        offers(d, theirs, opp).any { it.faceIndex == null } shouldBe false
        offers(d, theirs, opp).any { it.faceIndex == 0 } shouldBe true
        d.submitExpectFailure(CastSpell(opp, theirs))
        d.submitSuccess(CastSpell(opp, theirInstant))
    }
    test("a mana-value prohibition reads the chosen X: the offer stays up and only the prohibited X is rejected") {
        val d = setup(manaValueWarden); val me = d.activePlayer!!
        repeat(2) { d.putLandOnBattlefield(me, "Plains") }
        val spell = d.putCardInHand(me, xInstant.name)
        offers(d, spell).isNotEmpty() shouldBe true
        d.submitExpectFailure(CastSpell(me, spell, xValue = 2))
        d.submitSuccess(CastSpell(me, spell, xValue = 1))
    }
    test("a blanket prohibition withholds every face") {
        val d = setup(blanketWarden); val me = d.activePlayer!!
        val id = d.putCardInHand(me, adventure.name)
        offers(d, id).isEmpty() shouldBe true
        d.submitExpectFailure(CastSpell(me, id, faceIndex = 0))
        d.submitExpectFailure(CastSpell(me, id))
    }
})
