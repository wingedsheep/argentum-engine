package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.afr.cards.DancingSword
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dancing Sword (AFR #8) — {1}{W} Artifact — Equipment.
 *
 *   Equipped creature gets +2/+1.
 *   When equipped creature dies, you may have this Equipment become a 2/1 Construct artifact
 *   creature with flying and ward {1}. If you do, it isn't an Equipment.
 *   Equip {1}
 */
class DancingSwordScenarioTest : ScenarioTestBase() {

    private val equip = DancingSword.activatedAbilities.single { it.isEquipAbility }

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
            .withCardAttachedTo(1, "Dancing Sword", "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withLandsOnBattlefield(1, "Plains", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun ScenarioTestBase.TestGame.killBearsAndAnswer(animate: Boolean) {
            state.projectedState.getToughness(findPermanent("Grizzly Bears")!!) shouldBe 3
            castSpell(1, "Lightning Bolt", targetId = findPermanent("Grizzly Bears")!!).error shouldBe null
            resolveStack()
            answerYesNo(animate)
            resolveStack()
        }

        test("animating makes a permanent 2/1 flying ward Construct that is no longer an Equipment") {
            val game = board()
            game.killBearsAndAnswer(animate = true)
            val sword = game.findPermanent("Dancing Sword")!!

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

            val projected = game.state.projectedState
            withClue("still animated on the next turn") {
                projected.isCreature(sword) shouldBe true
                projected.getPower(sword) shouldBe 2
                projected.getToughness(sword) shouldBe 1
            }
            projected.hasType(sword, "ARTIFACT") shouldBe true
            projected.hasSubtype(sword, "Construct") shouldBe true
            withClue("it isn't an Equipment") { projected.hasSubtype(sword, "Equipment") shouldBe false }
            projected.hasKeyword(sword, Keyword.FLYING) shouldBe true
            projected.hasKeyword(sword, Keyword.WARD) shouldBe true
        }

        test("the animated Sword can no longer equip") {
            val game = board()
            game.killBearsAndAnswer(animate = true)
            val sword = game.findPermanent("Dancing Sword")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.execute(
                ActivateAbility(game.player1Id, sword, equip.id, targets = listOf(ChosenTarget.Permanent(giant)))
            )
            game.resolveStack()

            game.state.getEntity(sword)?.get<AttachedToComponent>() shouldBe null
            game.state.projectedState.getPower(giant) shouldBe 3
        }

        test("declining leaves an unattached Equipment that can equip again") {
            val game = board()
            game.killBearsAndAnswer(animate = false)
            val sword = game.findPermanent("Dancing Sword")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.state.projectedState.isCreature(sword) shouldBe false
            game.state.projectedState.hasSubtype(sword, "Equipment") shouldBe true

            game.execute(
                ActivateAbility(game.player1Id, sword, equip.id, targets = listOf(ChosenTarget.Permanent(giant)))
            ).error shouldBe null
            game.resolveStack()

            game.state.getEntity(sword)?.get<AttachedToComponent>()?.targetId shouldBe giant
            game.state.projectedState.getPower(giant) shouldBe 5
        }
    }
}
