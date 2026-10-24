package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.afr.cards.HandOfVecna
import com.wingedsheep.mtg.sets.definitions.afr.cards.TheBookOfVileDarkness
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hand of Vecna (AFR #246) — {3} Legendary Artifact — Equipment.
 *
 *   At the beginning of combat on your turn, equipped creature or a creature you control named Vecna
 *   gets +X/+X until end of turn, where X is the number of cards in your hand.
 *   Equip—Pay 1 life for each card in your hand.
 *   Equip {2}
 *
 * The life equip is priced from the hand as it is paid; the combat bonus lets the controller choose
 * between the equipped creature and their Vecna (2021-07-23 ruling).
 */
class HandOfVecnaScenarioTest : ScenarioTestBase() {

    private val lifeEquip = HandOfVecna.activatedAbilities.first { it.isEquipAbility && it.cost is AbilityCost.Atom }

    private fun ScenarioTestBase.TestGame.equipWithLife(target: EntityId) = execute(
        ActivateAbility(player1Id, findPermanent("Hand of Vecna")!!, lifeEquip.id, targets = listOf(ChosenTarget.Permanent(target)))
    )

    /** No printed card is named Vecna; the ruling's creature, as a plain 1/1. */
    private val vecnaStandIn = card("Vecna") {
        manaCost = "{B}"
        typeLine = "Legendary Creature — Zombie God"
        power = 1
        toughness = 1
    }

    init {
        cardRegistry.register(listOf(vecnaStandIn))

        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Hand of Vecna")
            .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
            .withCardsInHand(1, "Swamp", 3)
            .apply { repeat(5) { withCardInLibrary(1, "Swamp") } }
            .apply { repeat(5) { withCardInLibrary(2, "Swamp") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("equip by paying 1 life per card in hand, then the combat trigger pumps by hand size") {
            val game = board().build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val hand = game.findPermanent("Hand of Vecna")!!

            game.equipWithLife(bears).error shouldBe null
            withClue("three cards in hand cost three life") { game.getLifeTotal(1) shouldBe 17 }
            game.resolveStack()
            game.state.getEntity(hand)!!.get<AttachedToComponent>()?.targetId shouldBe bears

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            if (game.getPendingDecision() is SelectCardsDecision) game.selectCards(listOf(bears))
            game.resolveStack()
            if (game.getPendingDecision() is SelectCardsDecision) game.selectCards(listOf(bears))
            withClue("+3/+3 for three cards in hand") {
                game.state.projectedState.getPower(bears) shouldBe 5
                game.state.projectedState.getToughness(bears) shouldBe 5
            }
        }

        test("the life equip can't be activated with less life than cards in hand") {
            val game = board().withLifeTotal(1, 2).build()
            game.equipWithLife(game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
            game.getLifeTotal(1) shouldBe 2
        }

        test("the Vecna token from The Book of Vile Darkness carries the trigger and pumps itself") {
            val game = board()
                .withCardOnBattlefield(1, "The Book of Vile Darkness")
                .withCardOnBattlefield(1, "Eye of Vecna")
                .build()
            game.execute(
                ActivateAbility(game.player1Id, game.findPermanent("The Book of Vile Darkness")!!,
                    TheBookOfVileDarkness.activatedAbilities.single().id)
            ).error shouldBe null
            game.resolveStack()
            val vecna = game.findPermanent("Vecna")!!
            val cards = game.handSize(1)

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            if (game.getPendingDecision() is SelectCardsDecision) game.selectCards(listOf(vecna))
            game.resolveStack()
            if (game.getPendingDecision() is SelectCardsDecision) game.selectCards(listOf(vecna))
            withClue("Vecna, attached to nothing, is the only creature the copied trigger can pick") {
                game.state.projectedState.getPower(vecna) shouldBe 8 + cards
                game.state.projectedState.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
            }
        }

        test("with Vecna and an equipped creature, the controller chooses which one gets the bonus") {
            val game = board().withCardOnBattlefield(1, vecnaStandIn.name).build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val vecna = game.findPermanent("Vecna")!!
            game.equipWithLife(bears).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()
            val decision = game.getPendingDecision()
            withClue("both the equipped Bears and Vecna are offered") {
                (decision as SelectCardsDecision).options shouldContainExactlyInAnyOrder listOf(bears, vecna)
            }
            game.selectCards(listOf(vecna)).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getPower(vecna) shouldBe 4
            game.state.projectedState.getPower(bears) shouldBe 2
        }
    }
}
