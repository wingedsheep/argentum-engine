package com.wingedsheep.engine.handlers.effects.permanent.attachments

import com.wingedsheep.engine.core.PermanentAttachedEvent
import com.wingedsheep.engine.core.PermanentUnattachedEvent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * [AttachmentMover] — the shared "attach an Aura or Equipment on the battlefield to a permanent"
 * operation (CR 701.3a) behind AttachEquipment, AttachTargetEquipmentToCreature and AttachToChosenHost.
 *
 * Legality (CR 701.3a): an Aura needs a host its enchant ability allows and without protection from
 * it (CR 702.16c) and can't enchant itself (CR 303.4d); an Equipment needs a creature (CR 301.5) that
 * isn't protected from it (CR 702.16d); anything else can't be attached. Moving (CR 701.3b/d): a move
 * to a new host unattaches first and reports both events; re-attaching to the same host does nothing.
 */
class AttachmentMoverTest : ScenarioTestBase() {

    private fun legal(game: TestGame, attachment: String, host: String): Boolean =
        AttachmentMover.canAttach(
            game.state, services.predicateEvaluator, cardRegistry,
            game.findPermanent(attachment)!!, game.findPermanent(host)!!
        )

    init {
        context("canAttach") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardAttachedTo(1, "Breath of Fury", "Craw Wurm")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Black Knight")
                .withCardOnBattlefield(1, "Leonin Scimitar")
                .withCardOnBattlefield(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("an Aura may move to any creature its enchant ability allows") {
                legal(game, "Holy Strength", "Hill Giant") shouldBe true
                legal(game, "Holy Strength", "Craw Wurm") shouldBe true
            }
            test("not to a permanent its enchant ability excludes") {
                legal(game, "Holy Strength", "Forest") shouldBe false
                withClue("Breath of Fury enchants only creatures its controller controls") {
                    legal(game, "Breath of Fury", "Hill Giant") shouldBe false
                    legal(game, "Breath of Fury", "Grizzly Bears") shouldBe true
                }
            }
            test("not to a creature with protection from its color (CR 702.16c)") {
                legal(game, "Holy Strength", "Black Knight") shouldBe false
            }
            test("an Aura can't enchant itself (CR 303.4d)") {
                legal(game, "Holy Strength", "Holy Strength") shouldBe false
            }
            test("an Equipment needs a creature; a non-attachment can't be attached") {
                legal(game, "Leonin Scimitar", "Hill Giant") shouldBe true
                legal(game, "Leonin Scimitar", "Forest") shouldBe false
                legal(game, "Grizzly Bears", "Craw Wurm") shouldBe false
            }
        }

        context("attach") {
            test("moving to a new host unattaches, attaches, and reports both events") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val aura = game.findPermanent("Holy Strength")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val (state, events) = AttachmentMover.attach(game.state, aura, giant, game.player1Id)

                state.getEntity(aura)!!.get<AttachedToComponent>()!!.targetId shouldBe giant
                state.getEntity(giant)!!.get<AttachmentsComponent>()!!.attachedIds shouldBe listOf(aura)
                state.getEntity(bears)!!.get<AttachmentsComponent>() shouldBe null
                events.filterIsInstance<PermanentUnattachedEvent>().single().attachedToId shouldBe bears
                events.filterIsInstance<PermanentAttachedEvent>().single().attachedToId shouldBe giant
            }

            test("re-attaching to the current host does nothing (CR 701.3b)") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val aura = game.findPermanent("Holy Strength")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val (state, events) = AttachmentMover.attach(game.state, aura, bears, game.player1Id)

                state shouldBe game.state
                events.shouldBeEmpty()
            }
        }
    }
}
