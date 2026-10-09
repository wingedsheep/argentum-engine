package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.player.FlashGrantsThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class LinkedExileFlashTimingTest : FunSpec({
    val keeper = card("Flash Timing Exile Keeper") {
        manaCost = "{0}"
        typeLine = "Artifact"
        staticAbility {
            ability = GrantMayCastFromLinkedExile(
                filter = GameObjectFilter.Creature,
                duringYourTurnOnly = true,
                withoutPayingManaCost = true,
            )
        }
    }
    val granter = card("Flash Timing Granter") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        staticAbility {
            ability = GrantFlashToSpellType(GameObjectFilter.Creature, controllerOnly = true)
        }
    }
    val enabler = card("Flash Timing Enabler") {
        manaCost = "{0}"
        typeLine = "Artifact"
    }
    val conditionalCreature = card("Flash Timing Conditional Creature") {
        manaCost = "{2}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        conditionalFlash = Conditions.YouControl(GameObjectFilter.Permanent.named(enabler.name))
    }

    for (grantKind in listOf("static", "turn", "conditional")) {
        for (grantToCaster in listOf(false, true)) {
            test("linked exile reads $grantKind flash from the caster when only the ${if (grantToCaster) "caster" else "owner"} qualifies") {
                val d = GameTestDriver().apply {
                    registerCards(TestCards.all + listOf(keeper, granter, enabler, conditionalCreature))
                    initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
                    passPriorityUntil(Step.BEGIN_COMBAT)
                }
                val caster = d.activePlayer!!
                val owner = d.getOpponent(caster)
                val source = d.putPermanentOnBattlefield(caster, keeper.name)
                val cardName = if (grantKind == "conditional") conditionalCreature.name else "Grizzly Bears"
                val exiled = d.putCardInExile(owner, cardName)
                d.replaceState(d.state.updateEntity(source) { it.with(LinkedExileComponent(listOf(exiled))) })

                val recipient = if (grantToCaster) caster else owner
                when (grantKind) {
                    "static" -> d.putPermanentOnBattlefield(recipient, granter.name)
                    "turn" -> d.replaceState(d.state.updateEntity(recipient) {
                        it.with(FlashGrantsThisTurnComponent(listOf(GameObjectFilter.Creature)))
                    })
                    "conditional" -> d.putPermanentOnBattlefield(recipient, enabler.name)
                }

                d.legalActions(caster).any {
                    it.affordable && (it.action as? CastSpell)?.cardId == exiled
                } shouldBe grantToCaster
                val result = d.castSpell(caster, exiled)
                if (grantToCaster) {
                    result.outcome shouldBe Outcome.Done
                    d.bothPass()
                    d.assertPermanentExists(caster, cardName)
                } else {
                    result.outcome shouldNotBe Outcome.Done
                }
            }
        }
    }
})
