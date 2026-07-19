package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.AffectsFilter
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectData
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.Modification
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blc.cards.BelloBardOfTheBrambles
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * CR 613.6 — a single continuous effect that applies in several layers/sublayers affects the
 * *same* locked-in set of objects in each layer, and keeps applying "even if the ability generating
 * the effect is removed during this process".
 *
 * The engine models such an effect as a group of [ContinuousEffectData] sharing a
 * [ContinuousEffectData.groupId] (one group per multi-layer static ability). This suite pins the
 * two behaviours the group machinery must guarantee, both at the raw projection level (direct
 * effect injection) and end-to-end through Bello, Bard of the Brambles (a
 * [com.wingedsheep.sdk.scripting.CompositeStaticAbility]).
 *
 * Regression coverage for issue #1317.
 */
class MultiLayerStaticAbilityTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(vararg extraCards: CardDefinition): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extraCards.toList())
        return driver
    }

    fun GameTestDriver.init() {
        initMirrorMatch(
            deck = com.wingedsheep.sdk.model.Deck.of("Plains" to 20, "Forest" to 20),
            skipMulligans = true
        )
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.addContinuousEffects(entityId: EntityId, effects: List<ContinuousEffectData>) {
        replaceState(state.updateEntity(entityId) { container ->
            val existing = container.get<ContinuousEffectSourceComponent>()
            val merged = if (existing != null) existing.effects + effects else effects
            container.with(ContinuousEffectSourceComponent(merged))
        })
    }

    // A blank enchantment used only as a carrier for injected continuous effects.
    fun blankEnchantment(name: String) =
        CardDefinition.enchantment(name, ManaCost.parse("{2}"), script = CardScript())

    // A vanilla artifact with a chosen mana value (no P/T, no abilities of its own).
    fun blankArtifact(name: String, cost: String) =
        CardDefinition.artifact(name, ManaCost.parse(cost))

    // "Noncreature artifact you control." Controller-dependent, so Layer 4/7 re-resolve it; and its
    // membership FLIPS once the object becomes a creature — the exact shape CR 613.6 must lock in.
    val noncreatureArtifactYouControl = AffectsFilter.Generic(
        GroupFilter(baseFilter = GameObjectFilter.Artifact.notCreature().youControl())
    )

    // =========================================================================
    // Raw projection: the group machinery in isolation.
    // =========================================================================

    context("CR 613.6 group locking (direct effect injection)") {

        test("same set of objects: a grouped animate keeps setting P/T on the now-creature it made") {
            // "Each noncreature artifact you control is a 2/2 creature" — Layer 4 makes it a
            // creature, Layer 7b sets its P/T to those SAME objects (CR 613.6's noncreature-
            // artifact example). Because the two parts share a groupId, the affected set is locked
            // in at Layer 4 and reused in Layer 7b even though the artifact is no longer a
            // "noncreature artifact" by then.
            val driver = createDriver(blankEnchantment("Animus"), blankArtifact("Idol", "{4}"))
            driver.init()
            val p = driver.activePlayer!!

            val source = driver.putPermanentOnBattlefield(p, "Animus")
            val idol = driver.putPermanentOnBattlefield(p, "Idol")
            driver.addContinuousEffects(source, listOf(
                ContinuousEffectData(Modification.AddType("CREATURE"), noncreatureArtifactYouControl, groupId = "g0"),
                ContinuousEffectData(Modification.SetPowerToughness(2, 2), noncreatureArtifactYouControl, groupId = "g0"),
            ))

            val projected = projector.project(driver.state)

            projected.isCreature(idol) shouldBe true
            projected.getPower(idol) shouldBe 2
            projected.getToughness(idol) shouldBe 2
        }

        test("ungrouped contrast: the Layer 7b part is lost because the filter re-resolves per layer") {
            // The SAME two effects WITHOUT a shared groupId: each layer re-resolves the filter
            // independently, so by Layer 7b the artifact is a creature and drops out of
            // "noncreature artifact" — its P/T is never set. This documents the bug that grouping
            // (CompositeStaticAbility / groupId) fixes.
            val driver = createDriver(blankEnchantment("Animus"), blankArtifact("Idol", "{4}"))
            driver.init()
            val p = driver.activePlayer!!

            val source = driver.putPermanentOnBattlefield(p, "Animus")
            val idol = driver.putPermanentOnBattlefield(p, "Idol")
            driver.addContinuousEffects(source, listOf(
                ContinuousEffectData(Modification.AddType("CREATURE"), noncreatureArtifactYouControl),
                ContinuousEffectData(Modification.SetPowerToughness(2, 2), noncreatureArtifactYouControl),
            ))

            val projected = projector.project(driver.state)

            projected.isCreature(idol) shouldBe true
            // Layer 7b dropped it — no P/T set.
            projected.getPower(idol) shouldBe null
            projected.getToughness(idol) shouldBe null
        }

        test("removed mid-sequence: a grouped effect that started before Layer 6 still applies Layer 7") {
            // The animate source loses all abilities (Layer 6). Per CR 613.6, because the animate
            // effect already started applying in Layer 4, it keeps applying its Layer 7b P/T set to
            // the locked-in objects even though the ability generating it was removed.
            val driver = createDriver(
                blankEnchantment("Animus"), blankEnchantment("Silencer"), blankArtifact("Idol", "{4}")
            )
            driver.init()
            val p = driver.activePlayer!!

            val source = driver.putPermanentOnBattlefield(p, "Animus")
            val idol = driver.putPermanentOnBattlefield(p, "Idol")
            val silencer = driver.putPermanentOnBattlefield(p, "Silencer")
            driver.addContinuousEffects(source, listOf(
                ContinuousEffectData(Modification.AddType("CREATURE"), noncreatureArtifactYouControl, groupId = "g0"),
                ContinuousEffectData(Modification.SetPowerToughness(2, 2), noncreatureArtifactYouControl, groupId = "g0"),
            ))
            // Silencer removes all abilities from the animate source (the ability generating the effect).
            driver.addContinuousEffects(silencer, listOf(
                ContinuousEffectData(Modification.RemoveAllAbilities, AffectsFilter.SpecificEntities(setOf(source))),
            ))

            val projected = projector.project(driver.state)

            projected.hasLostAllAbilities(source) shouldBe true
            projected.isCreature(idol) shouldBe true
            projected.getPower(idol) shouldBe 2
            projected.getToughness(idol) shouldBe 2
        }

        test("ungrouped contrast: losing the ability suppresses the standalone Layer 7 P/T set") {
            // Without grouping, the Layer 7 part is a standalone effect from a source that lost all
            // abilities, so it IS suppressed — the artifact becomes a 0/0-ish creature with no P/T.
            val driver = createDriver(
                blankEnchantment("Animus"), blankEnchantment("Silencer"), blankArtifact("Idol", "{4}")
            )
            driver.init()
            val p = driver.activePlayer!!

            val source = driver.putPermanentOnBattlefield(p, "Animus")
            val idol = driver.putPermanentOnBattlefield(p, "Idol")
            val silencer = driver.putPermanentOnBattlefield(p, "Silencer")
            driver.addContinuousEffects(source, listOf(
                ContinuousEffectData(Modification.AddType("CREATURE"), noncreatureArtifactYouControl),
                ContinuousEffectData(Modification.SetPowerToughness(2, 2), noncreatureArtifactYouControl),
            ))
            driver.addContinuousEffects(silencer, listOf(
                ContinuousEffectData(Modification.RemoveAllAbilities, AffectsFilter.SpecificEntities(setOf(source))),
            ))

            val projected = projector.project(driver.state)

            projected.getPower(idol) shouldBe null
            projected.getToughness(idol) shouldBe null
        }
    }

    // =========================================================================
    // End-to-end: Bello, Bard of the Brambles (CompositeStaticAbility).
    // =========================================================================

    context("Bello, Bard of the Brambles") {

        test("animates a qualifying artifact across layers 4/6/7 on your turn") {
            val driver = createDriver(
                BelloBardOfTheBrambles, blankArtifact("Big Idol", "{4}")
            )
            driver.init()
            val p = driver.activePlayer!!

            driver.putPermanentOnBattlefield(p, "Bello, Bard of the Brambles")
            val idol = driver.putPermanentOnBattlefield(p, "Big Idol")

            val projected = projector.project(driver.state)

            // Layer 4: a 4/4 Elemental creature in addition to its other types (still an artifact).
            projected.isCreature(idol) shouldBe true
            projected.hasSubtype(idol, "Elemental") shouldBe true
            projected.getTypes(idol).contains("ARTIFACT") shouldBe true
            // Layer 7b: base 4/4.
            projected.getPower(idol) shouldBe 4
            projected.getToughness(idol) shouldBe 4
            // Layer 6: indestructible and haste.
            projected.hasKeyword(idol, Keyword.INDESTRUCTIBLE) shouldBe true
            projected.hasKeyword(idol, Keyword.HASTE) shouldBe true
        }

        test("does not animate an artifact with mana value below 4, nor Equipment") {
            val driver = createDriver(
                BelloBardOfTheBrambles,
                blankArtifact("Small Idol", "{3}"),
                CardDefinition.equipment("Big Boots", ManaCost.parse("{4}"), equipCost = ManaCost.parse("{2}")),
            )
            driver.init()
            val p = driver.activePlayer!!

            driver.putPermanentOnBattlefield(p, "Bello, Bard of the Brambles")
            val smallIdol = driver.putPermanentOnBattlefield(p, "Small Idol")
            val boots = driver.putPermanentOnBattlefield(p, "Big Boots")

            val projected = projector.project(driver.state)

            projected.isCreature(smallIdol) shouldBe false
            projected.isCreature(boots) shouldBe false
        }

        test("keeps the animation when Bello loses all abilities (CR 613.6)") {
            // Bello's animate ability is removed in Layer 6, but it already started applying in
            // Layer 4, so the whole effect — including the Layer 7b 4/4 — keeps applying.
            val driver = createDriver(
                BelloBardOfTheBrambles, blankEnchantment("Silencer"), blankArtifact("Big Idol", "{4}")
            )
            driver.init()
            val p = driver.activePlayer!!

            val bello = driver.putPermanentOnBattlefield(p, "Bello, Bard of the Brambles")
            val idol = driver.putPermanentOnBattlefield(p, "Big Idol")
            val silencer = driver.putPermanentOnBattlefield(p, "Silencer")
            driver.addContinuousEffects(silencer, listOf(
                ContinuousEffectData(Modification.RemoveAllAbilities, AffectsFilter.SpecificEntities(setOf(bello))),
            ))

            val projected = projector.project(driver.state)

            projected.hasLostAllAbilities(bello) shouldBe true
            // The animated artifact still projects as a 4/4 creature.
            projected.isCreature(idol) shouldBe true
            projected.getPower(idol) shouldBe 4
            projected.getToughness(idol) shouldBe 4
        }
    }
})
