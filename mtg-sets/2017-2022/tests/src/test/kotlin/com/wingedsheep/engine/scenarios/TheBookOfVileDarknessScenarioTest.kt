package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.afr.cards.TheBookOfVileDarkness
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * The Book of Vile Darkness (AFR #91) — {B}{B}{B} Legendary Artifact — Book.
 *
 *   At the beginning of your end step, if you lost 2 or more life this turn, create a 2/2 black
 *   Zombie creature token.
 *   {T}, Exile The Book of Vile Darkness and artifacts you control named Eye of Vecna and Hand of
 *   Vecna: Create Vecna, a legendary 8/8 black Zombie God creature token with indestructible and
 *   all triggered abilities of the exiled cards.
 *
 * Hand of Vecna isn't in the corpus yet, so the test registers a stand-in of that name whose only
 * ability is a marker trigger.
 */
class TheBookOfVileDarknessScenarioTest : ScenarioTestBase() {

    private val handStandIn = card("Hand of Vecna") {
        manaCost = "{3}"
        typeLine = "Legendary Artifact — Equipment"
        oracleText = "At the beginning of combat on your turn, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
            effect = Effects.GainLife(1)
        }
    }

    private fun ScenarioTestBase.TestGame.activateBook() = execute(
        ActivateAbility(player1Id, findPermanent("The Book of Vile Darkness")!!, TheBookOfVileDarkness.activatedAbilities.single().id)
    )

    init {
        cardRegistry.register(listOf(handStandIn))

        fun board(vararg artifacts: String) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "The Book of Vile Darkness")
            .apply { artifacts.forEach { withCardOnBattlefield(1, it) } }
            .apply { repeat(5) { withCardInLibrary(1, "Swamp") } }
            .apply { repeat(5) { withCardInLibrary(2, "Swamp") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("end step makes a Zombie only after losing 2 or more life this turn") {
            val game = board().withCardInHand(1, "Eye of Vecna").withLandsOnBattlefield(1, "Swamp", 2).build()
            game.castSpell(1, "Eye of Vecna").error shouldBe null
            game.resolveStack() // Eye's enters trigger: draw, lose 2
            game.getLifeTotal(1) shouldBe 18

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.findPermanents("Zombie Token") shouldHaveSize 1
        }

        test("no Zombie when less than 2 life was lost") {
            val game = board().build()
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.findPermanents("Zombie Token") shouldHaveSize 0
        }

        test("Vecna is created with every exiled card's triggered abilities, and Eye's enters ability triggers on it") {
            val game = board("Eye of Vecna", "Hand of Vecna").build()
            val handBefore = game.handSize(1)
            game.activateBook().error shouldBe null
            game.resolveStack()

            val vecna = game.findPermanent("Vecna")!!
            val card = game.state.getEntity(vecna)!!.get<CardComponent>()!!
            card.typeLine.isLegendary shouldBe true
            game.state.projectedState.getPower(vecna) shouldBe 8
            game.state.projectedState.getToughness(vecna) shouldBe 8
            game.state.projectedState.hasKeyword(vecna, Keyword.INDESTRUCTIBLE) shouldBe true
            // Book (1) + Eye (2) + Hand (1).
            card.copyTriggeredAbilities shouldHaveSize 4
            game.isInExile(1, "The Book of Vile Darkness") shouldBe true
            game.isInExile(1, "Eye of Vecna") shouldBe true
            game.isInExile(1, "Hand of Vecna") shouldBe true

            // Eye of Vecna's "when this enters, draw a card and lose 2 life" fired for Vecna.
            game.handSize(1) shouldBe handBefore + 1
            game.getLifeTotal(1) shouldBe 18

            // The Book's own end-step ability now lives on Vecna: 2 life was lost, so a Zombie.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.findPermanents("Zombie Token") shouldHaveSize 1
        }

        test("can't be activated without both Eye of Vecna and Hand of Vecna") {
            val game = board("Eye of Vecna").build()
            (game.activateBook().error != null) shouldBe true
            game.isInExile(1, "Eye of Vecna") shouldBe false
            game.findPermanent("Vecna") shouldBe null
        }
    }
}
