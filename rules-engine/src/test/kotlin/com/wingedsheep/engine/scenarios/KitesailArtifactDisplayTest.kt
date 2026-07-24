package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Client DTO rendering of a Layer-4 card-type change — Kitesail Larcenist turning a creature into a
 * Treasure artifact. The end-to-end transform + projection (types become ARTIFACT, `isCreature`
 * false, subtypes Treasure) is proven in [KitesailLarcenistScenarioTest]; this test pins how
 * [com.wingedsheep.engine.view.ClientStateTransformer] surfaces that projected state to the client:
 *
 *  - **CR 208.3** — a noncreature permanent has no power or toughness, so the transformed
 *    permanent's `ClientCard` reports null P/T and the client hides its stat box (it no longer reads
 *    as a creature).
 *  - a **`card_type_changed`** `activeEffects` badge announces the new type line ("Artifact —
 *    Treasure"), because the printed art still shows the original creature.
 *
 * The transform is applied here via the same Layer-4 floating effects Kitesail's
 * `BecomeArtifactEffect` creates ([SerializableModification.SetCardTypes] +
 * [SerializableModification.SetAllSubtypes]); the executor path itself is covered by the scenario
 * test, so this stays a focused view-layer test.
 */
class KitesailArtifactDisplayTest : ScenarioTestBase() {

    /** Apply Kitesail's type replacement (creature -> Treasure artifact) to [target]. */
    private fun TestGame.becomeTreasure(source: EntityId, target: EntityId) {
        val ctx = EffectContext(sourceId = source, controllerId = player1Id)
        val duration = Duration.WhileSourceOnBattlefield("Kitesail Larcenist")
        state = state
            .addFloatingEffect(
                layer = Layer.TYPE,
                modification = SerializableModification.SetCardTypes(setOf("ARTIFACT")),
                affectedEntities = setOf(target),
                duration = duration,
                context = ctx
            )
            .addFloatingEffect(
                layer = Layer.TYPE,
                modification = SerializableModification.SetAllSubtypes(setOf("Treasure")),
                affectedEntities = setOf(target),
                duration = duration,
                context = ctx
            )
    }

    init {
        context("Kitesail Larcenist — client display of the Treasure transform") {

            test("transformed creature reports as an Artifact with no P/T and a type-change badge") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kitesail Larcenist")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kitesail = game.findPermanent("Kitesail Larcenist")!!
                val courser = game.findPermanent("Centaur Courser")!!

                // Baseline: the untransformed Centaur Courser renders as a 3/3 creature with no badge.
                val before = game.getClientState(1).cards.getValue(courser)
                before.power.shouldNotBeNull()
                before.toughness.shouldNotBeNull()
                before.cardTypes shouldContain "CREATURE"
                before.activeEffects.none { it.effectId == "card_type_changed" } shouldBe true

                game.becomeTreasure(kitesail, courser)

                val after = game.getClientState(1).cards.getValue(courser)
                // CR 208.3: a noncreature permanent has no power or toughness.
                after.power.shouldBeNull()
                after.toughness.shouldBeNull()
                // It is now an Artifact — Treasure, no longer a creature.
                after.cardTypes shouldContain "ARTIFACT"
                after.cardTypes shouldNotContain "CREATURE"
                after.subtypes shouldContain "Treasure"
                // The type change is surfaced as a badge (the art still shows the original creature).
                val badge = after.activeEffects.firstOrNull { it.effectId == "card_type_changed" }
                badge.shouldNotBeNull()
                badge.name shouldBe "Artifact — Treasure"
                badge.description shouldBe "Card type is now Artifact — Treasure"
            }

            test("an untouched creature still shows its P/T and carries no type-change badge") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                val card = game.getClientState(1).cards.getValue(courser)
                card.power shouldBe 3
                card.toughness shouldBe 3
                card.cardTypes shouldContain "CREATURE"
                card.activeEffects.none { it.effectId == "card_type_changed" } shouldBe true
            }
        }
    }
}
