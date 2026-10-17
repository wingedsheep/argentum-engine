package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Danitha, Benalia's Hope (DMU #15) — "When Danitha enters, you may put an Aura or Equipment card
 * from your hand or graveyard onto the battlefield attached to Danitha."
 *
 * The card splits along its two rulings, both through `attachTo`: an Aura enters attached (or stays in
 * its previous zone when it can't legally enchant Danitha), while an Equipment enters attached — or
 * unattached when Danitha is gone.
 */
class DanithaBenaliasHopeScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Danitha, Benalia's Hope")
        .withCardInHand(1, "Holy Strength")
        .withCardInGraveyard(1, "Bonesplitter")
        .withCardInGraveyard(1, "Erosion")
        .withLandsOnBattlefield(1, "Plains", 5)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Danitha, Benalia's Hope") {

            test("offers Auras and Equipment from hand and graveyard; an Equipment enters attached") {
                val game = board().build()
                val holy = game.findCardsInHand(1, "Holy Strength").single()
                val bonesplitter = game.findCardsInGraveyard(1, "Bonesplitter").single()
                val erosion = game.findCardsInGraveyard(1, "Erosion").single()

                game.castSpell(1, "Danitha, Benalia's Hope").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.options shouldContainExactlyInAnyOrder listOf(holy, bonesplitter, erosion)
                game.selectCards(listOf(bonesplitter)).error shouldBe null
                game.resolveStack()

                val danitha = game.findPermanent("Danitha, Benalia's Hope")!!
                val equipment = game.findPermanent("Bonesplitter")!!
                withClue("Bonesplitter is attached to Danitha") {
                    game.state.getEntity(equipment)!!.get<AttachedToComponent>()!!.targetId shouldBe danitha
                    game.state.projectedState.getPower(danitha) shouldBe 6
                }
            }

            test("an Aura from hand enters attached to Danitha") {
                val game = board().build()
                val holy = game.findCardsInHand(1, "Holy Strength").single()

                game.castSpell(1, "Danitha, Benalia's Hope").error shouldBe null
                game.resolveStack()
                game.selectCards(listOf(holy)).error shouldBe null
                game.resolveStack()

                val danitha = game.findPermanent("Danitha, Benalia's Hope")!!
                val aura = game.findPermanent("Holy Strength")!!
                game.state.getEntity(aura)!!.get<AttachedToComponent>()!!.targetId shouldBe danitha
                game.state.projectedState.getPower(danitha) shouldBe 5
                game.state.projectedState.getToughness(danitha) shouldBe 6
            }

            test("an Aura that can't enchant Danitha stays in its previous zone") {
                val game = board().build()
                val erosion = game.findCardsInGraveyard(1, "Erosion").single()

                game.castSpell(1, "Danitha, Benalia's Hope").error shouldBe null
                game.resolveStack()
                game.selectCards(listOf(erosion)).error shouldBe null
                game.resolveStack()

                withClue("Erosion (enchant land) never enters") {
                    game.isOnBattlefield("Erosion") shouldBe false
                    game.isInGraveyard(1, "Erosion") shouldBe true
                }
            }

            test("declining puts nothing onto the battlefield") {
                val game = board().build()
                game.castSpell(1, "Danitha, Benalia's Hope").error shouldBe null
                game.resolveStack()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Bonesplitter") shouldBe false
                game.isOnBattlefield("Holy Strength") shouldBe false
                game.isInHand(1, "Holy Strength") shouldBe true
            }

            test("with Danitha gone, an Equipment still enters, unattached") {
                val game = board().build()
                val bonesplitter = game.findCardsInGraveyard(1, "Bonesplitter").single()

                game.castSpell(1, "Danitha, Benalia's Hope").error shouldBe null
                // Resolve the creature spell only; its enters trigger goes on the stack.
                game.passPriority()
                game.passPriority()
                val danitha = game.findPermanent("Danitha, Benalia's Hope")!!
                withClue("the enters trigger is waiting on the stack") { game.state.stack.size shouldBe 1 }

                game.state = zones.moveToZone(game.state, danitha, Zone.GRAVEYARD).state
                game.resolveStack()
                game.selectCards(listOf(bonesplitter)).error shouldBe null
                game.resolveStack()

                val equipment = game.findPermanent("Bonesplitter")
                withClue("Bonesplitter entered the battlefield unattached") {
                    (equipment != null) shouldBe true
                    game.state.getEntity(equipment!!)!!.get<AttachedToComponent>() shouldBe null
                }
            }
        }
    }
}
