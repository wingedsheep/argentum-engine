package com.wingedsheep.engine

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalAttackTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * An emblem's (or any global granted) "whenever a [filtered] creature attacks" trigger fans out per
 * matching attacker (CR 603.2c), binding each one as the triggering entity — exactly as the
 * battlefield and graveyard scans do. Garruk, Apex Predator's −8 emblem is the card that needs it
 * (covered in its scenario test); a synthetic green sorcery keeps the primitive off any one card.
 */
class EmblemAttackTriggerPerAttackerTest : FunSpec({

    val emblemText = "Whenever a green creature you control attacks, it gets +1/+1 until end of turn."

    val emblemSorcery: CardDefinition = card("Test Attack Emblem") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "You get an emblem with \"$emblemText\""
        spell {
            effect = Effects.CreateGlobalTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.a(GameObjectFilter.Creature.withColor(Color.GREEN).youControl()).attacks()
                    effect = Effects.ModifyStats(1, 1, EffectTarget.TriggeringEntity)
                    description = emblemText
                },
                descriptionOverride = emblemText,
                emblem = true
            )
        }
    }

    val attackDoubler: CardDefinition = card("Test Attack Doubler") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "If a creature attacking causes a triggered ability of a permanent you control to trigger, that ability triggers an additional time."
        staticAbility { ability = AdditionalAttackTriggers(attackerFilter = GameObjectFilter.Any) }
    }

    fun newGame(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + emblemSorcery + attackDoubler)
        initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castEmblem() {
        giveMana(player1, Color.GREEN, 1)
        castSpell(player1, putCardInHand(player1, "Test Attack Emblem")).error shouldBe null
        var passes = 0
        while (stackSize > 0 && passes++ < 20) bothPass()
        state.globalGrantedTriggeredAbilities.size shouldBe 1
    }

    test("an attack-trigger doubler leaves the emblem's trigger alone: it isn't a permanent's ability") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player1, "Test Attack Doubler")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(courser)
        d.castEmblem()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(courser), d.player2).error shouldBe null
        d.stackSize shouldBe 1
    }

    test("each matching attacker triggers the emblem once and gets the pump; a non-matching one doesn't") {
        val d = newGame()
        val courserA = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val courserB = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        listOf(courserA, courserB, lions).forEach { d.removeSummoningSickness(it) }

        d.castEmblem()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(courserA, courserB, lions), d.player2).error shouldBe null
        d.stackSize shouldBe 2
        var passes = 0
        while (d.stackSize > 0 && passes++ < 20) d.bothPass()

        val projected = d.state.projectedState
        projected.getPower(courserA) shouldBe 4
        projected.getToughness(courserA) shouldBe 4
        projected.getPower(courserB) shouldBe 4
        projected.getToughness(courserB) shouldBe 4
        projected.getPower(lions) shouldBe 1
        projected.getToughness(lions) shouldBe 1
    }
})
