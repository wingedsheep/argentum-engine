package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.ArchwayOfInnovation
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Archway of Innovation (MH3) — "{U}, {T}: The next spell you cast this turn has improvise."
 * The rider is spent by the next spell, so the spell after it gets no improvise.
 */
class ArchwayOfInnovationScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(ArchwayOfInnovation)
        cardRegistry.register(card("Archway Trinket") {
            manaCost = "{1}"
            typeLine = "Artifact"
            oracleText = ""
        })
        cardRegistry.register(card("Archway Lesson") {
            manaCost = "{3}{U}"
            colorIdentity = "U"
            typeLine = "Sorcery"
            oracleText = "You gain 4 life."
            spell { effect = Effects.GainLife(4) }
        })

        fun board() = scenario()
            .withPlayers("P1", "P2")
            .withCardInHand(1, "Archway Lesson")
            .withCardInHand(1, "Archway Lesson")
            .withCardOnBattlefield(1, "Archway of Innovation")
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardOnBattlefield(1, "Archway Trinket")
            .withCardOnBattlefield(1, "Archway Trinket")
            .withCardOnBattlefield(1, "Archway Trinket")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.lessonAction() = getLegalActions(1).firstOrNull {
            it.actionType == "CastSpell" && it.action is CastSpell && it.description.contains("Archway Lesson")
        }

        test("{U}, {T}: the next spell has improvise — artifacts pay its generic") {
            val game = board()
            val archway = game.findPermanent("Archway of Innovation")!!
            withClue("before the Archway's ability, two Islands can't pay {3}{U} and nothing improvises") {
                game.lessonAction()?.hasTapForGeneric shouldNotBe true
            }

            val improviseAbility = ArchwayOfInnovation.activatedAbilities[1].id
            game.execute(ActivateAbility(game.player1Id, archway, improviseAbility)).error shouldBe null
            game.resolveStack()

            val action = game.lessonAction()!!
            action.isAffordable shouldBe true
            action.hasTapForGeneric shouldBe true

            val trinkets = game.findAllPermanents("Archway Trinket")
            val before = game.getLifeTotal(1)
            game.execute(
                (action.action as CastSpell).copy(
                    alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = trinkets.toSet())
                )
            ).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe before + 4
            trinkets.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
            withClue("the Lesson was the next spell, so it spent the rider") {
                game.state.pendingNextSpellKeywords.shouldBeEmpty()
            }

            withClue("the second Lesson is not 'the next spell' — no improvise, and no mana left for it") {
                game.lessonAction()?.hasTapForGeneric shouldNotBe true
            }
        }
    }
}
