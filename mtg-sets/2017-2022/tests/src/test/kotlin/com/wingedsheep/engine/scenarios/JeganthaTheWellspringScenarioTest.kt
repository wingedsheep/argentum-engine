package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.iko.cards.JeganthaTheWellspring
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CompanionEligibility
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jegantha, the Wellspring (IKO #222).
 *
 * - Its companion condition admits a deck whose cards never repeat a mana symbol, and rejects
 *   one card with a repeat (Serra Angel's `{3}{W}{W}`).
 * - `{T}`: adds one mana of each color, all of it barred from generic costs — it pays Watchwolf's
 *   `{G}{W}` alone but not Grizzly Bears' `{1}{G}`.
 */
class JeganthaTheWellspringScenarioTest : ScenarioTestBase() {

    private val manaAbility = JeganthaTheWellspring.activatedAbilities.single().id

    private fun card(name: String): CardDefinition = cardRegistry.getCard(name)!!

    init {
        context("Jegantha, the Wellspring") {

            test("companion: a deck without repeated mana symbols qualifies; one {W}{W} card breaks it") {
                val clean = listOf("Forest", "Watchwolf", "Grizzly Bears", "Lightning Bolt").map(::card)
                CompanionEligibility.conditionError(JeganthaTheWellspring, clean, 40).shouldBeNull()
                CompanionEligibility.conditionError(JeganthaTheWellspring, clean + card("Serra Angel"), 40)
                    .shouldNotBeNull()
            }

            test("tapping it adds {W}{U}{B}{R}{G}, each barred from generic costs") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jegantha, the Wellspring")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val jegantha = game.findPermanent("Jegantha, the Wellspring")!!

                game.execute(ActivateAbility(game.player1Id, jegantha, manaAbility)).error shouldBe null

                val pool = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.restrictedMana
                pool.map { it.color } shouldContainExactlyInAnyOrder
                    listOf(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN)
                pool.all { it.restriction == ManaRestriction.CannotPayGenericCosts } shouldBe true
            }

            test("its mana pays a fully colored cost but never the generic part") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jegantha, the Wellspring")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Watchwolf")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldNotBe null
                game.castSpell(1, "Watchwolf").error shouldBe null
                game.resolveStack()
                game.findPermanent("Watchwolf") shouldNotBe null
            }
        }
    }
}
