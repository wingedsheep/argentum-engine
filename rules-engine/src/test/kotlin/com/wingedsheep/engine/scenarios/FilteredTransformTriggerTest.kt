package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A transform trigger with a filtered subject (CR 701.27): "whenever a [filter] you control
 * transforms" — Norn's Inquisitor's "a permanent you control transforms into a Phyrexian". The
 * filter is matched against the face that's up *after* the transform, and its "you control"
 * against the trigger's controller. "Enters transformed" (Corruption of Towashi) is the enter
 * trigger over a `transformed()` filter — a permanent entering back face up (CR 701.27g).
 *
 * Uses the shared "Test DFC Front" (Human) // "Test DFC Back" (Werewolf) pair.
 */
class FilteredTransformTriggerTest : ScenarioTestBase() {

    private val werewolfWatcher = card("Test Werewolf Transform Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Permanent.youControl().withSubtype("Werewolf")).transforms()
            effect = Effects.GainLife(1)
        }
    }

    private val enteredTransformedWatcher = card("Test Entered Transformed Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Permanent.youControl().transformed()).enters()
            effect = Effects.GainLife(1)
        }
    }

    private val otherTransformWatcher = card("Test Other Transform Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.another(GameObjectFilter.Permanent.youControl()).transforms()
            effect = Effects.GainLife(1)
        }
    }

    private val returnTransformed = card("Test Return Transformed") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ExileAndReturnTransformed(t)
        }
    }

    init {
        cardRegistry.register(werewolfWatcher)
        cardRegistry.register(enteredTransformedWatcher)
        cardRegistry.register(otherTransformWatcher)
        cardRegistry.register(returnTransformed)

        fun board(watcher: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, watcher)
            .withCardInHand(1, "Transform Target Creature")
            .withCardInHand(1, "Transform Target Creature")
            .withCardInHand(1, "Test Return Transformed")
            .withLandsOnBattlefield(1, "Island", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("fires when a permanent you control transforms into a face that matches the filter") {
            val game = board(werewolfWatcher.name).withCardOnBattlefield(1, "Test DFC Front").build()
            val dfc = game.findPermanent("Test DFC Front")!!

            game.castSpell(1, "Transform Target Creature", targetId = dfc)
            game.resolveStack()

            game.state.getEntity(dfc)?.get<CardComponent>()?.name shouldBe "Test DFC Back"
            game.getLifeTotal(1) shouldBe 21
        }

        test("the filter reads the new face — transforming back into a Human doesn't fire it") {
            val game = board(werewolfWatcher.name).withCardOnBattlefield(1, "Test DFC Front").build()
            val dfc = game.findPermanent("Test DFC Front")!!

            game.castSpell(1, "Transform Target Creature", targetId = dfc)
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 21

            game.castSpell(1, "Transform Target Creature", targetId = dfc)
            game.resolveStack()
            withClue("now a Human again — no trigger") {
                game.state.getEntity(dfc)?.get<CardComponent>()?.name shouldBe "Test DFC Front"
                game.getLifeTotal(1) shouldBe 21
            }
        }

        test("an opponent's permanent transforming doesn't fire a 'you control' trigger") {
            val game = board(werewolfWatcher.name).withCardOnBattlefield(2, "Test DFC Front").build()
            val dfc = game.findPermanent("Test DFC Front")!!

            game.castSpell(1, "Transform Target Creature", targetId = dfc)
            game.resolveStack()

            game.state.getEntity(dfc)?.get<CardComponent>()?.name shouldBe "Test DFC Back"
            game.getLifeTotal(1) shouldBe 20
        }

        test("'another' fires for other permanents") {
            val game = board(otherTransformWatcher.name).withCardOnBattlefield(1, "Test DFC Front").build()
            val dfc = game.findPermanent("Test DFC Front")!!

            game.castSpell(1, "Transform Target Creature", targetId = dfc)
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 21
        }

        test("a permanent returned to the battlefield transformed fires the entered-transformed trigger") {
            val game = board(enteredTransformedWatcher.name).withCardOnBattlefield(1, "Test DFC Front").build()
            val dfc = game.findPermanent("Test DFC Front")!!

            game.castSpell(1, "Test Return Transformed", targetId = dfc)
            game.resolveStack()

            withClue("entered back face up") { (game.findPermanent("Test DFC Back") != null) shouldBe true }
            game.getLifeTotal(1) shouldBe 21
        }

        test("a permanent entering front face up doesn't count as entering transformed") {
            val game = board(enteredTransformedWatcher.name).withCardInHand(1, "Test DFC Front")
                .withLandsOnBattlefield(1, "Forest", 3).build()

            game.castSpell(1, "Test DFC Front")
            game.resolveStack()

            (game.findPermanent("Test DFC Front") != null) shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
