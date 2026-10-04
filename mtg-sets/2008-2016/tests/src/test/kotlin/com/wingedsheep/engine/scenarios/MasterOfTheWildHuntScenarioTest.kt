package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Master of the Wild Hunt — {T}: Tap all untapped Wolf creatures you control. Each Wolf tapped this
 * way deals damage equal to its power to target creature. That creature deals damage equal to its
 * power divided as its controller chooses among any number of those Wolves.
 */
class MasterOfTheWildHuntScenarioTest : ScenarioTestBase() {

    private fun TestGame.damageOn(id: EntityId): Int = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    private fun TestGame.activateHunt(target: EntityId) {
        val master = findPermanent("Master of the Wild Hunt")!!
        val ability = cardRegistry.getCard("Master of the Wild Hunt")!!.script.activatedAbilities.single()
        execute(
            ActivateAbility(player1Id, master, ability.id, targets = listOf(ChosenTarget.Permanent(target)))
        ).error shouldBe null
        resolveStack()
    }

    init {
        test("wolves tapped this way hit the target, and its controller divides the damage back among them") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Master of the Wild Hunt")
                .withCardOnBattlefield(1, "Lone Wolf")      // 2/2
                .withCardOnBattlefield(1, "Brazen Wolves")  // 2/3
                .withCardOnBattlefield(1, "Watchwolf", tapped = true) // already tapped: not "tapped this way"
                .withCardOnBattlefield(2, "Craw Wurm")      // 6/4
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val loneWolf = game.findPermanent("Lone Wolf")!!
            val brazen = game.findPermanent("Brazen Wolves")!!
            val watchwolf = game.findPermanent("Watchwolf")!!
            val wurm = game.findPermanent("Craw Wurm")!!

            game.activateHunt(wurm)

            withClue("the untapped Wolves are tapped by the effect") {
                game.state.getEntity(loneWolf)?.has<TappedComponent>() shouldBe true
                game.state.getEntity(brazen)?.has<TappedComponent>() shouldBe true
            }
            withClue("each Wolf tapped this way dealt damage equal to its power to the target") {
                game.damageOn(wurm) shouldBe 4
            }

            val decision = game.getPendingDecision().shouldNotBeNull().shouldBeInstanceOf<DistributeDecision>()
            withClue("the target's controller divides, among only the Wolves tapped this way") {
                decision.playerId shouldBe game.player2Id
                decision.totalAmount shouldBe 6
                decision.targets shouldContainExactlyInAnyOrder listOf(loneWolf, brazen)
            }

            game.submitDistribution(mapOf(loneWolf to 2, brazen to 4)).error shouldBe null

            game.findPermanent("Lone Wolf") shouldBe null
            game.findPermanent("Brazen Wolves") shouldBe null
            game.isInGraveyard(1, "Lone Wolf") shouldBe true
            game.isInGraveyard(1, "Brazen Wolves") shouldBe true
            withClue("the Wolf that wasn't tapped this way takes nothing") {
                game.damageOn(watchwolf) shouldBe 0
            }
            withClue("the Wolves' 4 damage is lethal to the 6/4") {
                game.findPermanent("Craw Wurm") shouldBe null
            }
        }

        test("the controller may pile all the damage on one Wolf and leave the other out") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Master of the Wild Hunt")
                .withCardOnBattlefield(1, "Lone Wolf")
                .withCardOnBattlefield(1, "Brazen Wolves")
                .withCardOnBattlefield(2, "Ironroot Treefolk") // 3/5
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val loneWolf = game.findPermanent("Lone Wolf")!!
            val brazen = game.findPermanent("Brazen Wolves")!!
            val treefolk = game.findPermanent("Ironroot Treefolk")!!

            game.activateHunt(treefolk)
            game.getPendingDecision().shouldBeInstanceOf<DistributeDecision>()
            game.submitDistribution(mapOf(loneWolf to 0, brazen to 3)).error shouldBe null

            game.findPermanent("Lone Wolf") shouldBe loneWolf
            game.damageOn(loneWolf) shouldBe 0
            game.findPermanent("Brazen Wolves") shouldBe null
            withClue("4 damage from the Wolves doesn't kill a 3/5") {
                game.damageOn(treefolk) shouldBe 4
            }
        }

        test("a lone Wolf takes all of the target's damage with no division to make") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Master of the Wild Hunt")
                .withCardOnBattlefield(1, "Brazen Wolves")  // 2/3
                .withCardOnBattlefield(2, "Hill Giant")     // 3/3
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.activateHunt(giant)

            game.hasPendingDecision() shouldBe false
            game.findPermanent("Brazen Wolves") shouldBe null
            game.damageOn(giant) shouldBe 2
        }

        test("a target dealt lethal damage by the Wolves still deals its damage back") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Master of the Wild Hunt")
                .withCardOnBattlefield(1, "Lone Wolf")
                .withCardOnBattlefield(1, "Brazen Wolves")
                .withCardOnBattlefield(2, "Hill Giant")  // 3/3, takes 4
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val loneWolf = game.findPermanent("Lone Wolf")!!
            val brazen = game.findPermanent("Brazen Wolves")!!
            game.activateHunt(game.findPermanent("Hill Giant")!!)

            withClue("state-based actions wait until the ability finishes resolving") {
                game.getPendingDecision().shouldBeInstanceOf<DistributeDecision>().totalAmount shouldBe 3
            }
            game.submitDistribution(mapOf(loneWolf to 2, brazen to 1)).error shouldBe null

            game.findPermanent("Hill Giant") shouldBe null
            game.findPermanent("Lone Wolf") shouldBe null
            game.damageOn(brazen) shouldBe 1
        }

        test("with no Wolves nothing is dealt") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Master of the Wild Hunt")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.activateHunt(giant)

            game.hasPendingDecision() shouldBe false
            game.damageOn(giant) shouldBe 0
            game.damageOn(game.findPermanent("Master of the Wild Hunt")!!) shouldBe 0
        }

        test("upkeep creates a 2/2 green Wolf") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Master of the Wild Hunt")
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UNTAP)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            val wolves = game.findPermanents("Wolf Token")
            wolves.size shouldBe 1
        }
    }
}
