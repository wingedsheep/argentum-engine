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

    test("each matching attacker triggers the emblem once and gets the pump; a non-matching one doesn't") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + emblemSorcery)
            initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val courserA = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val courserB = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        listOf(courserA, courserB, lions).forEach { d.removeSummoningSickness(it) }

        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpell(d.player1, d.putCardInHand(d.player1, "Test Attack Emblem")).error shouldBe null
        var passes = 0
        while (d.stackSize > 0 && passes++ < 20) d.bothPass()
        d.state.globalGrantedTriggeredAbilities.size shouldBe 1

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(courserA, courserB, lions), d.player2).error shouldBe null
        d.stackSize shouldBe 2
        passes = 0
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
