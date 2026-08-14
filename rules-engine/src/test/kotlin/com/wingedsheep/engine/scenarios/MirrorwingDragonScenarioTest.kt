package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Mirrorwing Dragon — "Whenever a player casts an instant or sorcery spell that targets only this
 * creature, that player copies that spell for each other creature they control that the spell could
 * target. Each copy targets a different one of those creatures."
 *
 * Exercises `SpellCastPredicate.TargetsOnlySource` and `CopySpellForEachOtherPossibleTargetEffect`
 * (CR 707.10d). The tests are the card's 2016-07-13 rulings, one apiece.
 */
class MirrorwingDragonScenarioTest : ScenarioTestBase() {

    init {
        test("copies the spell for each other creature its caster controls") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Mirrorwing Dragon")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dragon = game.findPermanent("Mirrorwing Dragon")!!
            game.castSpell(1, "Giant Growth", dragon).error shouldBe null
            game.resolveStack()

            // The original pumps the Dragon; one copy each pumps the two other creatures.
            val projected = game.state.projectedState
            projected.getPower(dragon) shouldBe 7
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 5
            projected.getPower(game.findPermanent("Hill Giant")!!) shouldBe 6
        }

        test("copies go to the caster's creatures, not the Dragon controller's") {
            // The key ruling: "the affected creatures may be controlled by a different player than
            // the controller of Mirrorwing Dragon."
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Mirrorwing Dragon")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dragon = game.findPermanent("Mirrorwing Dragon")!!
            game.castSpell(1, "Giant Growth", dragon).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            // Player 1 cast it, so player 1's Bears got the copy.
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 5
            // Player 2's other creature is untouched even though player 2 controls the Dragon.
            projected.getPower(game.findPermanent("Hill Giant")!!) shouldBe 3
            // The original still resolved on the Dragon.
            projected.getPower(dragon) shouldBe 7
        }

        test("does not trigger on a spell that targets a creature other than the Dragon") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Mirrorwing Dragon")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 5
            projected.getPower(game.findPermanent("Hill Giant")!!) shouldBe 3
            projected.getPower(game.findPermanent("Mirrorwing Dragon")!!) shouldBe 4
        }

        test("does not trigger on a spell that targets a player") {
            // "targets only Mirrorwing Dragon and no other object or player" — a player target is
            // not the Dragon, so the trigger never fires.
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Mirrorwing Dragon")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            // No copy was made, so the Bears took no damage and are still a 2/2.
            game.state.projectedState.getToughness(game.findPermanent("Grizzly Bears")!!) shouldBe 2
        }

        test("skips a creature the spell could not target") {
            // "Any creature the player controls that couldn't be targeted by the original spell (due
            // to shroud, protection abilities, targeting restrictions, or any other reason) is just
            // ignored." Kodama of the North Tree has shroud.
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Mirrorwing Dragon")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Kodama of the North Tree")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dragon = game.findPermanent("Mirrorwing Dragon")!!
            game.castSpell(1, "Giant Growth", dragon).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 5
            // Shrouded, so no copy targeted it — still a printed 6/4.
            projected.getPower(game.findPermanent("Kodama of the North Tree")!!) shouldBe 6
            projected.getPower(dragon) shouldBe 7
        }

        test("makes no copies and still resolves when the caster controls nothing else") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Mirrorwing Dragon")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dragon = game.findPermanent("Mirrorwing Dragon")!!
            game.castSpell(1, "Giant Growth", dragon).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(dragon) shouldBe 7
            game.state.stack.isEmpty() shouldBe true
        }

        test("the copies are not cast, so the ability does not retrigger") {
            // "The copies … are created on the stack, so they're not cast. Abilities that trigger
            // when a player casts a spell (like Mirrorwing Dragon's ability itself) won't trigger."
            // Two other creatures must yield exactly two copies, never a cascade.
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Mirrorwing Dragon")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dragon = game.findPermanent("Mirrorwing Dragon")!!
            game.castSpell(1, "Giant Growth", dragon).error shouldBe null

            // Resolve only the trigger, then count the Giant Growths waiting on the stack:
            // the original plus exactly two copies.
            game.passPriority()
            game.passPriority()
            val growthsOnStack = game.state.stack.count { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Giant Growth"
            }
            growthsOnStack shouldBe 3
        }

        test("copies of a modal spell keep the same mode and target a different creature each") {
            // "If the spell that's copied is modal … the copies will have the same mode. A different
            // mode cannot be chosen." Sarkhan's Resolve mode 0 is "target creature gets +3/+3".
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Mirrorwing Dragon")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Sarkhan's Resolve")
                .withLandsOnBattlefield(1, "Forest", 2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dragon = game.findPermanent("Mirrorwing Dragon")!!
            game.castSpellWithMode(1, "Sarkhan's Resolve", 0, dragon).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(dragon) shouldBe 7
            // The copy kept mode 0 and was retargeted at the Bears — it did not destroy anything.
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 5
        }
    }
}
