package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.*
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Classic "hard" layer scenarios from MTG judge exams and community resources.
 *
 * These are the canonical brain-teasers that test deep understanding of Rule 613:
 *   - Humility + Opalescence (layers 4, 6, 7b — timestamp matters)
 *   - Blood Moon + Urborg (layer 4 dependency — ability removal before type change)
 *   - Humility + Arcane Adaptation (layer 4 before layer 6)
 *   - March of the Machines + Mycosynth Lattice (layers 4, 7b — animating artifacts)
 *   - Multiple set-P/T effects with timestamp ordering
 *
 * Each test constructs the scenario using inline card definitions with direct
 * ContinuousEffectSourceComponent injection for effects that don't have existing
 * StaticAbility types (e.g., "all creatures lose all abilities" as a global effect).
 */
class ClassicLayerScenariosTest : FunSpec({

    val projector = StateProjector()

    // =========================================================================
    // Helpers
    // =========================================================================

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

    fun GameTestDriver.attachAura(auraId: EntityId, targetId: EntityId) {
        replaceState(state.updateEntity(auraId) { container ->
            container.with(com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(targetId))
        })
    }

    fun GameTestDriver.addCounters(entityId: EntityId, type: CounterType, count: Int) {
        replaceState(state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(type, count))
        })
    }

    /**
     * Inject a ContinuousEffectSourceComponent directly onto an entity.
     * Used for effects that don't have a matching StaticAbility (e.g., group RemoveAllAbilities).
     */
    fun GameTestDriver.addContinuousEffects(entityId: EntityId, effects: List<ContinuousEffectData>) {
        replaceState(state.updateEntity(entityId) { container ->
            val existing = container.get<ContinuousEffectSourceComponent>()
            val merged = if (existing != null) existing.effects + effects else effects
            container.with(ContinuousEffectSourceComponent(merged))
        })
    }

    // =========================================================================
    // Test cards — inline definitions for classic layer scenarios
    // =========================================================================

    // --- Humility: "All creatures lose all abilities and have base P/T 1/1." ---
    // Uses direct ContinuousEffectData injection since LoseAllAbilities only supports StaticTarget.
    val HumilityEffects = listOf(
        ContinuousEffectData(
            modification = Modification.RemoveAllAbilities,
            affectsFilter = AffectsFilter.AllCreatures
        ),
        ContinuousEffectData(
            modification = Modification.SetPowerToughness(1, 1),
            affectsFilter = AffectsFilter.AllCreatures
        )
    )

    // --- Opalescence: "Each other non-Aura enchantment is a creature in addition ---
    // --- to its other types with P/T each equal to its mana value." ---
    // Simplified: "Each other enchantment is also a creature with P/T = CMC."
    // For testing purposes, we use a fixed P/T since DynamicAmount.CMC would require
    // additional infrastructure. We test the layer interactions, not the CMC calculation.

    // --- Blood Moon: "Nonbasic lands are Mountains." ---
    // Layer 4: SetBasicLandTypes to {Mountain} for all nonbasic lands.
    // This also implicitly removes abilities (since changing to a basic land type
    // replaces the land's existing subtypes per Rule 305.7).

    // --- Urborg, Tomb of Yawgmoth: "Each land is a Swamp in addition to its other land types." ---
    // Layer 4: AddSubtype("Swamp") to all lands.

    // --- March of the Machines: "Each noncreature artifact is an artifact creature ---
    // --- with P/T equal to its mana value." ---

    // --- Arcane Adaptation: "Creatures you control are the chosen type ---
    // --- in addition to their other types." ---

    // =========================================================================
    // SCENARIO 1: Humility + Opalescence
    //
    // The canonical hardest layer interaction in MTG.
    //
    // Humility (Layer 6: all creatures lose abilities; Layer 7b: all creatures are 1/1)
    // Opalescence (Layer 4: non-Aura enchantments become creatures with P/T = CMC)
    //
    // Key: Layer 4 (type-changing) applies BEFORE Layer 6 (ability removal)
    // and Layer 7b (set P/T). So Opalescence makes Humility a creature first,
    // then Humility removes abilities and sets P/T.
    //
    // Since Humility is now a creature (from Opalescence), it is affected by
    // its own ability. But because Humility's ability starts applying in layers
    // 6 and 7b (after layer 4), it still functions — it was "turned on" in
    // layer 4 (became a creature) but its effects don't apply until later layers.
    //
    // The result depends on TIMESTAMPS within Layer 7b:
    //   - If Opalescence entered first: Opalescence sets Humility to 4/4 (CMC=4),
    //     then Humility sets all creatures (including itself) to 1/1. → Humility is 1/1.
    //   - If Humility entered first: Humility sets 1/1, then Opalescence sets 4/4.
    //     → Humility is 4/4.
    //
    // Other creatures are ALWAYS 1/1 (Humility's 7b applies after any base P/T).
    // =========================================================================

    context("Scenario 1: Humility + Opalescence") {

        // Simplified Opalescence: "All other enchantments are also creatures with base P/T 4/4."
        // (In real MTG, P/T = CMC; we use fixed 4/4 for Humility's CMC of 4)
        /** Helper to create Opalescence effects for a given source entity and CMC-based P/T. */
        fun opalescenceEffects(power: Int, toughness: Int) = listOf(
            // Layer 4: Other enchantments become creatures
            ContinuousEffectData(
                modification = Modification.AddType("CREATURE"),
                affectsFilter = AffectsFilter.Generic(
                    GroupFilter(
                        baseFilter = GameObjectFilter.Companion.Enchantment,
                        excludeSelf = true
                    )
                )
            ),
            // Layer 7b: Other enchantments have P/T equal to CMC (simplified as fixed values)
            ContinuousEffectData(
                modification = Modification.SetPowerToughness(power, toughness),
                affectsFilter = AffectsFilter.Generic(
                    GroupFilter(
                        baseFilter = GameObjectFilter.Companion.Enchantment,
                        excludeSelf = true
                    )
                )
            )
        )

        // Blank enchantment shells (no abilities — effects injected manually)
        val BlankOpalescence = CardDefinition.enchantment(
            name = "Opalescence",
            manaCost = ManaCost.parse("{2}{W}{W}"),
            script = CardScript()
        )

        val BlankHumility = CardDefinition.enchantment(
            name = "Humility",
            manaCost = ManaCost.parse("{2}{W}{W}"),
            script = CardScript()
        )

        test("Opalescence first, then Humility — Humility is 1/1 (later timestamp wins in 7b)") {
            // Opalescence enters first → makes Humility a 4/4 creature (L4 + L7b)
            // Humility enters second → sets ALL creatures to 1/1 (L7b, later timestamp)
            // Result: Both are creatures. Humility's 7b (timestamp 2) overrides Opalescence's 7b (timestamp 1).
            // Humility is 1/1. Both lose abilities (L6).
            val driver = createDriver(BlankOpalescence, BlankHumility)
            driver.init()
            val p = driver.activePlayer!!

            val opal = driver.putPermanentOnBattlefield(p, "Opalescence")
            driver.addContinuousEffects(opal, opalescenceEffects(4, 4))

            val humility = driver.putPermanentOnBattlefield(p, "Humility")
            driver.addContinuousEffects(humility, HumilityEffects)

            // Also add a regular creature to verify it's affected
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")

            val projected = projector.project(driver.state)

            // Opalescence makes Humility a creature (L4)
            projected.isCreature(humility) shouldBe true
            // Humility makes Opalescence a creature via... wait, Humility doesn't add types.
            // Opalescence's L4 makes Humility a creature. Humility doesn't make Opalescence a creature.
            // Opalescence is only a creature if something makes it one. Opalescence makes OTHER enchantments creatures.
            // So Humility (an enchantment) becomes a creature from Opalescence's L4.
            // Opalescence itself is NOT a creature (it excludes itself with excludeSelf).

            // Humility is a creature (made by Opalescence's L4)
            // L6: Humility loses all abilities (because it's a creature)
            projected.hasLostAllAbilities(humility) shouldBe true
            // L7b: Opalescence sets Humility to 4/4 (timestamp 1), then Humility sets to 1/1 (timestamp 2)
            // Later timestamp wins → Humility is 1/1
            projected.getPower(humility) shouldBe 1
            projected.getToughness(humility) shouldBe 1

            // Grizzly Bears: L6 loses abilities, L7b set to 1/1
            projected.getPower(bears) shouldBe 1
            projected.getToughness(bears) shouldBe 1
            projected.hasLostAllAbilities(bears) shouldBe true
        }

        test("Humility first, then Opalescence — Humility is 4/4 (later timestamp wins in 7b)") {
            // Humility enters first → sets all creatures to 1/1 (L7b, timestamp 1)
            // Opalescence enters second → makes Humility a creature, sets it to 4/4 (L4 + L7b, timestamp 2)
            // Result: In L7b, Humility sets 1/1 (ts1), Opalescence sets 4/4 (ts2).
            // Later timestamp wins → Humility is 4/4.
            val driver = createDriver(BlankHumility, BlankOpalescence)
            driver.init()
            val p = driver.activePlayer!!

            val humility = driver.putPermanentOnBattlefield(p, "Humility")
            driver.addContinuousEffects(humility, HumilityEffects)

            val opal = driver.putPermanentOnBattlefield(p, "Opalescence")
            driver.addContinuousEffects(opal, opalescenceEffects(4, 4))

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")

            val projected = projector.project(driver.state)

            // Humility is a creature (from Opalescence L4)
            projected.isCreature(humility) shouldBe true
            // L6: Humility loses abilities (it's a creature)
            projected.hasLostAllAbilities(humility) shouldBe true
            // L7b: Humility sets 1/1 (ts1), then Opalescence sets 4/4 (ts2) → 4/4 wins
            projected.getPower(humility) shouldBe 4
            projected.getToughness(humility) shouldBe 4

            // Grizzly Bears still 1/1 — only Humility's 7b applies (Opalescence only targets enchantments)
            projected.getPower(bears) shouldBe 1
            projected.getToughness(bears) shouldBe 1
        }

        test("Opalescence + Humility + anthem: anthem is also a creature, loses abilities, no pump") {
            // Opalescence makes Glorious Anthem a creature (L4).
            // Humility then removes all creature abilities (L6), including the anthem's pump.
            // So bears are just 1/1 — the anthem's +1/+1 is suppressed.
            val GloriousAnthem = CardDefinition.enchantment(
                name = "Glorious Anthem",
                manaCost = ManaCost.parse("{1}{W}{W}"),
                script = CardScript(
                    staticAbilities = listOf(
                        ModifyStatsForCreatureGroup(
                            powerBonus = 1,
                            toughnessBonus = 1,
                            filter = GroupFilter.AllCreatures
                        )
                    )
                )
            )

            val driver = createDriver(BlankOpalescence, BlankHumility, GloriousAnthem)
            driver.init()
            val p = driver.activePlayer!!

            val opal = driver.putPermanentOnBattlefield(p, "Opalescence")
            driver.addContinuousEffects(opal, opalescenceEffects(4, 4))
            val humility = driver.putPermanentOnBattlefield(p, "Humility")
            driver.addContinuousEffects(humility, HumilityEffects)
            driver.putPermanentOnBattlefield(p, "Glorious Anthem")

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")

            val projected = projector.project(driver.state)

            // Opalescence makes Glorious Anthem a creature, Humility removes its abilities.
            // The anthem's +1/+1 pump is suppressed. Bears are just 1/1.
            projected.getPower(bears) shouldBe 1
            projected.getToughness(bears) shouldBe 1
        }

        test("Opalescence + Humility + counters: counters apply on top of 1/1 base") {
            // L7b: 1/1 (Humility), L7d: +1/+1 counters → 3/3
            val driver = createDriver(BlankOpalescence, BlankHumility)
            driver.init()
            val p = driver.activePlayer!!

            val opal = driver.putPermanentOnBattlefield(p, "Opalescence")
            driver.addContinuousEffects(opal, opalescenceEffects(4, 4))
            val humility = driver.putPermanentOnBattlefield(p, "Humility")
            driver.addContinuousEffects(humility, HumilityEffects)

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            driver.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 2)

            val projected = projector.project(driver.state)

            // Bears: L7b 1/1 + L7d 2x +1/+1 = 3/3
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
        }
    }

    // =========================================================================
    // SCENARIO 2: Blood Moon + Urborg, Tomb of Yawgmoth
    //
    // Blood Moon: "Nonbasic lands are Mountains." (Layer 4: SetBasicLandTypes)
    // Urborg: "Each land is a Swamp in addition to its other land types." (Layer 4: AddSubtype)
    //
    // DEPENDENCY (Rule 613.8):
    //   Blood Moon's effect changes nonbasic lands to Mountains. This removes
    //   Urborg's abilities (since Urborg becomes a basic Mountain with no abilities).
    //   Therefore Urborg's effect DEPENDS on Blood Moon's — Blood Moon applies first.
    //
    //   After Blood Moon applies: Urborg loses its ability → Urborg's effect
    //   doesn't exist → all nonbasic lands are Mountains (not Swamps).
    //
    //   Basic lands are unaffected by Blood Moon (it only targets nonbasic).
    //   They are also unaffected by Urborg since Urborg lost its ability.
    //
    // Result (regardless of timestamp):
    //   - Nonbasic lands (including Urborg) are Mountains only
    //   - Basic lands remain whatever they are
    // =========================================================================

    context("Scenario 2: Blood Moon + Urborg") {

        // Filter: nonbasic lands (Land + NOT BasicLand)
        val nonbasicLandFilter = GameObjectFilter(
            cardPredicates = listOf(CardPredicate.IsLand, CardPredicate.Not(CardPredicate.IsBasicLand))
        )

        // Blood Moon: nonbasic lands become Mountains (replaces land subtypes)
        val BloodMoonEffects = listOf(
            ContinuousEffectData(
                modification = Modification.SetBasicLandTypes(setOf("Mountain")),
                affectsFilter = AffectsFilter.Generic(
                    GroupFilter(baseFilter = nonbasicLandFilter)
                )
            )
        )

        // Urborg: each land is a Swamp in addition to other types
        val UrborgEffects = listOf(
            ContinuousEffectData(
                modification = Modification.AddSubtype("Swamp"),
                affectsFilter = AffectsFilter.Generic(
                    GroupFilter(baseFilter = GameObjectFilter.Companion.Land)
                )
            )
        )

        val BlankBloodMoon = CardDefinition.enchantment(
            name = "Blood Moon",
            manaCost = ManaCost.parse("{2}{R}"),
            script = CardScript()
        )

        // Urborg is a legendary land
        val BlankUrborg = CardDefinition(
            name = "Urborg, Tomb of Yawgmoth",
            manaCost = ManaCost.ZERO,
            typeLine = TypeLine(
                cardTypes = setOf(CardType.LAND),
                supertypes = setOf(Supertype.LEGENDARY),
                subtypes = emptySet()
            ),
            script = CardScript()
        )

        // A nonbasic land for testing
        val TestNonbasicLand = CardDefinition(
            name = "Tropical Island",
            manaCost = ManaCost.ZERO,
            typeLine = TypeLine(
                cardTypes = setOf(CardType.LAND),
                subtypes = setOf(Subtype("Island"), Subtype("Forest"))
            ),
            script = CardScript()
        )

        test("Blood Moon makes nonbasic lands Mountains") {
            val driver = createDriver(BlankBloodMoon, TestNonbasicLand)
            driver.init()
            val p = driver.activePlayer!!

            val bloodMoon = driver.putPermanentOnBattlefield(p, "Blood Moon")
            driver.addContinuousEffects(bloodMoon, BloodMoonEffects)

            val tropIsland = driver.putPermanentOnBattlefield(p, "Tropical Island")

            val projected = projector.project(driver.state)

            // Tropical Island is nonbasic → Blood Moon replaces subtypes with Mountain
            projected.hasSubtype(tropIsland, "Mountain") shouldBe true
            projected.hasSubtype(tropIsland, "Island") shouldBe false
            projected.hasSubtype(tropIsland, "Forest") shouldBe false
        }

        test("Urborg first, then Blood Moon — Blood Moon is later, nonbasics become Mountains then gain Swamp") {
            // Both are Layer 4. They form a dependency cycle (shared entities) which is
            // broken by timestamp: Urborg (ts1) → Blood Moon (ts2).
            // Urborg adds Swamp first, then Blood Moon replaces all land subtypes with Mountain.
            // Net result: nonbasic lands are Mountains (Blood Moon overwrites Urborg's Swamp).
            // Urborg itself is also nonbasic, so it becomes a Mountain too.
            // Note: In real MTG, Blood Moon would remove Urborg's ability entirely (Rule 305.7),
            // but the engine doesn't yet model ability removal from land-type replacement.
            val driver = createDriver(BlankUrborg, BlankBloodMoon, TestNonbasicLand)
            driver.init()
            val p = driver.activePlayer!!

            val urborg = driver.putPermanentOnBattlefield(p, "Urborg, Tomb of Yawgmoth")
            driver.addContinuousEffects(urborg, UrborgEffects)

            val bloodMoon = driver.putPermanentOnBattlefield(p, "Blood Moon")
            driver.addContinuousEffects(bloodMoon, BloodMoonEffects)

            val tropIsland = driver.putPermanentOnBattlefield(p, "Tropical Island")

            val projected = projector.project(driver.state)

            // Urborg (ts1) adds Swamp to everything first, then Blood Moon (ts2) replaces
            // nonbasic land subtypes with Mountain. SetBasicLandTypes overwrites the Swamp.
            projected.hasSubtype(urborg, "Mountain") shouldBe true

            // Tropical Island: Urborg adds Swamp, then Blood Moon replaces with Mountain
            projected.hasSubtype(tropIsland, "Mountain") shouldBe true
            projected.hasSubtype(tropIsland, "Island") shouldBe false
        }

        test("Blood Moon does not affect basic lands") {
            val driver = createDriver(BlankBloodMoon)
            driver.init()
            val p = driver.activePlayer!!

            val bloodMoon = driver.putPermanentOnBattlefield(p, "Blood Moon")
            driver.addContinuousEffects(bloodMoon, BloodMoonEffects)

            // Plains is a basic land — should not be affected
            val plains = driver.putPermanentOnBattlefield(p, "Plains")

            val projected = projector.project(driver.state)

            projected.hasSubtype(plains, "Plains") shouldBe true
            projected.hasSubtype(plains, "Mountain") shouldBe false
        }
    }

    // =========================================================================
    // SCENARIO 3: Humility + Arcane Adaptation
    //
    // Arcane Adaptation: "Creatures you control are the chosen type in addition
    // to their other types." (Layer 4 — type-changing)
    //
    // Humility: "All creatures lose all abilities and have base P/T 1/1."
    // (Layer 6 — ability removal, Layer 7b — set P/T)
    //
    // Layer 4 applies BEFORE Layer 6. So Arcane Adaptation's type-granting
    // effect is applied first (it's in layer 4). Then Humility removes abilities
    // in layer 6. But Arcane Adaptation is not a creature, so Humility's
    // "all creatures lose abilities" doesn't affect it.
    //
    // Result: Creatures are the chosen type AND are 1/1 with no abilities.
    // Arcane Adaptation's effect persists because it's an enchantment, not a creature.
    // =========================================================================

    context("Scenario 3: Humility + Arcane Adaptation") {

        val BlankHumility = CardDefinition.enchantment(
            name = "Humility",
            manaCost = ManaCost.parse("{2}{W}{W}"),
            script = CardScript()
        )

        // Arcane Adaptation adds a creature type to all creatures you control
        val ArcaneAdaptation = CardDefinition.enchantment(
            name = "Arcane Adaptation",
            manaCost = ManaCost.parse("{2}{U}"),
            script = CardScript(
                staticAbilities = listOf(
                    GrantAdditionalTypesToGroup(
                        filter = GroupFilter.AllCreaturesYouControl,
                        addSubtypes = listOf("Angel")
                    )
                )
            )
        )

        test("Arcane Adaptation type-change persists through Humility ability removal") {
            val driver = createDriver(BlankHumility, ArcaneAdaptation)
            driver.init()
            val p = driver.activePlayer!!

            driver.putPermanentOnBattlefield(p, "Arcane Adaptation")
            val humility = driver.putPermanentOnBattlefield(p, "Humility")
            driver.addContinuousEffects(humility, HumilityEffects)

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")

            val projected = projector.project(driver.state)

            // L4: Bears gains Angel subtype from Arcane Adaptation
            projected.hasSubtype(bears, "Angel") shouldBe true
            projected.hasSubtype(bears, "Bear") shouldBe true

            // L6: Bears loses all abilities
            projected.hasLostAllAbilities(bears) shouldBe true

            // L7b: Bears is 1/1
            projected.getPower(bears) shouldBe 1
            projected.getToughness(bears) shouldBe 1
        }

        test("Arcane Adaptation itself is not a creature and is unaffected by Humility") {
            val driver = createDriver(BlankHumility, ArcaneAdaptation)
            driver.init()
            val p = driver.activePlayer!!

            val adaptation = driver.putPermanentOnBattlefield(p, "Arcane Adaptation")
            val humility = driver.putPermanentOnBattlefield(p, "Humility")
            driver.addContinuousEffects(humility, HumilityEffects)

            val projected = projector.project(driver.state)

            // Arcane Adaptation is an enchantment, not a creature
            projected.isCreature(adaptation) shouldBe false
            // It should NOT have lost its abilities (Humility only affects creatures)
            projected.hasLostAllAbilities(adaptation) shouldBe false
        }
    }

    // =========================================================================
    // SCENARIO 4: March of the Machines + Mycosynth Lattice
    //
    // Mycosynth Lattice: "All permanents are artifacts in addition to their other types."
    // (Layer 4 — type-changing)
    //
    // March of the Machines: "Each noncreature artifact is an artifact creature
    // with P/T equal to its mana value." (Layer 4 + Layer 7b)
    //
    // Combined: Mycosynth makes EVERYTHING an artifact (L4). March makes all
    // noncreature artifacts into creatures (L4). Together, all noncreature
    // permanents become artifact creatures.
    //
    // Key concern: Lands have CMC 0, so they become 0/0 creatures and die to
    // state-based actions. (We verify the P/T, not the SBA death.)
    // =========================================================================

    context("Scenario 4: March of the Machines + Mycosynth Lattice") {

        // Mycosynth Lattice: all permanents are artifacts
        val MycosynthLattice = CardDefinition.enchantment(
            name = "Mycosynth Lattice",
            manaCost = ManaCost.parse("{6}"),
            script = CardScript(
                staticAbilities = listOf(
                    GrantAdditionalTypesToGroup(
                        filter = GroupFilter.AllPermanents,
                        addCardTypes = listOf("ARTIFACT")
                    )
                )
            )
        )

        // March of the Machines: noncreature artifacts become creatures with P/T = CMC
        // Simplified: we use a fixed P/T of 0/0 for lands (CMC 0) since we're testing
        // the type-changing interaction, not the dynamic CMC calculation.
        // For proper testing, we create separate effects for different CMC values.

        // We need to make noncreature artifacts into creatures with set P/T.
        // This requires AnimateLandGroup-style multi-layer effects.
        // Using direct ContinuousEffectData injection.

        // Filter: noncreature artifacts
        val noncreatureArtifactFilter = GameObjectFilter(
            cardPredicates = listOf(CardPredicate.IsArtifact, CardPredicate.IsNoncreature)
        )

        val MarchOfTheMachinesEffects = listOf(
            // Layer 4: Noncreature artifacts become creatures
            ContinuousEffectData(
                modification = Modification.AddType("CREATURE"),
                affectsFilter = AffectsFilter.Generic(
                    GroupFilter(baseFilter = noncreatureArtifactFilter)
                )
            ),
            // Layer 7b: Set P/T (simplified to 0/0 for test — real card uses CMC)
            ContinuousEffectData(
                modification = Modification.SetPowerToughness(0, 0),
                affectsFilter = AffectsFilter.Generic(
                    GroupFilter(baseFilter = noncreatureArtifactFilter)
                )
            )
        )

        val BlankMarch = CardDefinition.enchantment(
            name = "March of the Machines",
            manaCost = ManaCost.parse("{3}{U}"),
            script = CardScript()
        )

        test("March alone: noncreature artifacts become creatures") {
            // March of the Machines animates pre-existing artifacts into creatures
            val SolRing = CardDefinition.artifact(
                name = "Sol Ring",
                manaCost = ManaCost.parse("{1}")
            )
            val driver = createDriver(BlankMarch, SolRing)
            driver.init()
            val p = driver.activePlayer!!

            val ring = driver.putPermanentOnBattlefield(p, "Sol Ring")
            val march = driver.putPermanentOnBattlefield(p, "March of the Machines")
            driver.addContinuousEffects(march, MarchOfTheMachinesEffects)

            val projected = projector.project(driver.state)

            // Sol Ring is an artifact → March makes it a creature
            projected.hasType(ring, "ARTIFACT") shouldBe true
            projected.isCreature(ring) shouldBe true
            // P/T set to 0/0 (simplified — real card would use CMC)
            projected.getPower(ring) shouldBe 0
            projected.getToughness(ring) shouldBe 0
        }

        test("Mycosynth + March: existing creatures keep their base P/T") {
            val driver = createDriver(MycosynthLattice, BlankMarch)
            driver.init()
            val p = driver.activePlayer!!

            driver.putPermanentOnBattlefield(p, "Mycosynth Lattice")
            val march = driver.putPermanentOnBattlefield(p, "March of the Machines")
            driver.addContinuousEffects(march, MarchOfTheMachinesEffects)

            // Grizzly Bears is already a creature — March only affects noncreature artifacts
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")

            val projected = projector.project(driver.state)

            // Bears becomes an artifact (from Mycosynth) but stays a creature
            projected.hasType(bears, "ARTIFACT") shouldBe true
            projected.isCreature(bears) shouldBe true
            // March doesn't affect it (it's already a creature) — base P/T preserved
            projected.getPower(bears) shouldBe 2
            projected.getToughness(bears) shouldBe 2
        }
    }

    // =========================================================================
    // SCENARIO 5: Multiple Set-P/T Effects (Timestamp Ordering in Layer 7b)
    //
    // When multiple Layer 7b effects apply to the same creature, the one
    // with the LATEST timestamp wins (they overwrite each other in order).
    //
    // This models real-world scenarios like:
    //   - Humility (1/1) then Turn to Frog (1/1 but later) → 1/1
    //   - Turn to Frog (1/1) then Opalescence (CMC/CMC) → CMC/CMC
    //   - Lignify (0/4) then Darksteel Mutation (0/1) → 0/1
    // =========================================================================

    context("Scenario 5: Multiple set-P/T with timestamp ordering") {

        /** "Enchanted creature has base P/T 0/4." */
        val Lignify = CardDefinition.enchantment(
            name = "Lignify",
            manaCost = ManaCost.parse("{1}{G}"),
            script = CardScript(
                staticAbilities = listOf(
                    SetBasePowerToughnessStatic(power = 0, toughness = 4, target = StaticTarget.AttachedCreature)
                )
            )
        )

        /** "Enchanted creature has base P/T 0/1." */
        val DarksteelMutation = CardDefinition.enchantment(
            name = "Darksteel Mutation",
            manaCost = ManaCost.parse("{1}{W}"),
            script = CardScript(
                staticAbilities = listOf(
                    SetBasePowerToughnessStatic(power = 0, toughness = 1, target = StaticTarget.AttachedCreature)
                )
            )
        )

        /** "Enchanted creature has base P/T 1/1." (Turn to Frog-like) */
        val TurnToFrog = CardDefinition.enchantment(
            name = "Turn to Frog",
            manaCost = ManaCost.parse("{1}{U}"),
            script = CardScript(
                staticAbilities = listOf(
                    SetBasePowerToughnessStatic(power = 1, toughness = 1, target = StaticTarget.AttachedCreature)
                )
            )
        )

        /** "Enchanted creature has base P/T 5/5." */
        val SizeUp = CardDefinition.enchantment(
            name = "Size Up",
            manaCost = ManaCost.parse("{3}{G}"),
            script = CardScript(
                staticAbilities = listOf(
                    SetBasePowerToughnessStatic(power = 5, toughness = 5, target = StaticTarget.AttachedCreature)
                )
            )
        )

        test("Lignify then Darksteel Mutation — later timestamp wins (0/1)") {
            val driver = createDriver(Lignify, DarksteelMutation)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val lignify = driver.putPermanentOnBattlefield(p, "Lignify")
            driver.attachAura(lignify, bears)
            val mutation = driver.putPermanentOnBattlefield(p, "Darksteel Mutation")
            driver.attachAura(mutation, bears)

            val projected = projector.project(driver.state)

            // Lignify (ts1) sets 0/4, Darksteel Mutation (ts2) sets 0/1 → 0/1 wins
            projected.getPower(bears) shouldBe 0
            projected.getToughness(bears) shouldBe 1
        }

        test("Darksteel Mutation then Lignify — later timestamp wins (0/4)") {
            val driver = createDriver(Lignify, DarksteelMutation)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val mutation = driver.putPermanentOnBattlefield(p, "Darksteel Mutation")
            driver.attachAura(mutation, bears)
            val lignify = driver.putPermanentOnBattlefield(p, "Lignify")
            driver.attachAura(lignify, bears)

            val projected = projector.project(driver.state)

            // Darksteel Mutation (ts1) sets 0/1, Lignify (ts2) sets 0/4 → 0/4 wins
            projected.getPower(bears) shouldBe 0
            projected.getToughness(bears) shouldBe 4
        }

        test("three set-P/T effects — last timestamp wins") {
            val driver = createDriver(Lignify, TurnToFrog, SizeUp)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val lignify = driver.putPermanentOnBattlefield(p, "Lignify")
            driver.attachAura(lignify, bears)
            val frog = driver.putPermanentOnBattlefield(p, "Turn to Frog")
            driver.attachAura(frog, bears)
            val sizeUp = driver.putPermanentOnBattlefield(p, "Size Up")
            driver.attachAura(sizeUp, bears)

            val projected = projector.project(driver.state)

            // Lignify(0/4) → TurnToFrog(1/1) → SizeUp(5/5) → last wins: 5/5
            projected.getPower(bears) shouldBe 5
            projected.getToughness(bears) shouldBe 5
        }

        test("set P/T + modification + counters compose correctly across sublayers") {
            // L7b: 0/4 (Lignify), L7c: +2/+2 (anthem-like), L7d: +1/+1 counter
            // Result: 0/4 + 2/2 + 1/1 = 3/7
            val BoostAura = CardDefinition.enchantment(
                name = "Boost Aura",
                manaCost = ManaCost.parse("{1}{G}"),
                script = CardScript(
                    staticAbilities = listOf(
                        ModifyStats(powerBonus = 2, toughnessBonus = 2, target = StaticTarget.AttachedCreature)
                    )
                )
            )

            val driver = createDriver(Lignify, BoostAura)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val lignify = driver.putPermanentOnBattlefield(p, "Lignify")
            driver.attachAura(lignify, bears)
            val boost = driver.putPermanentOnBattlefield(p, "Boost Aura")
            driver.attachAura(boost, bears)
            driver.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            val projected = projector.project(driver.state)

            // L7b: 0/4, L7c: +2/+2 → 2/6, L7d: +1/+1 → 3/7
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 7
        }
    }

    // =========================================================================
    // SCENARIO 6: Humility + Lord Effects (Cross-Layer Dependency)
    //
    // Humility removes abilities in Layer 6 and sets P/T in Layer 7b.
    // A lord effect like "Other creatures get +1/+1" is a static ability on
    // a creature. If Humility removes the lord's abilities, does the +1/+1
    // still apply?
    //
    // Answer: NO. Humility removes the lord's static abilities in Layer 6.
    // When Layer 7c tries to apply the lord's +1/+1, the ability no longer
    // exists. All creatures end up as 1/1 with no abilities.
    //
    // However, an anthem enchantment (non-creature) like Glorious Anthem
    // still works because Humility only affects creatures.
    // =========================================================================

    context("Scenario 6: Humility + Lord Effects") {

        val BlankHumility = CardDefinition.enchantment(
            name = "Humility",
            manaCost = ManaCost.parse("{2}{W}{W}"),
            script = CardScript()
        )

        val BearLord = CardDefinition.creature(
            name = "Bear Lord",
            manaCost = ManaCost.parse("{1}{G}{G}"),
            subtypes = setOf(Subtype("Bear")),
            power = 2,
            toughness = 2,
            script = CardScript(
                staticAbilities = listOf(
                    ModifyStatsForCreatureGroup(
                        powerBonus = 1,
                        toughnessBonus = 1,
                        filter = GroupFilter.OtherCreaturesYouControl
                    )
                )
            )
        )

        val GloriousAnthem = CardDefinition.enchantment(
            name = "Glorious Anthem",
            manaCost = ManaCost.parse("{1}{W}{W}"),
            script = CardScript(
                staticAbilities = listOf(
                    ModifyStatsForCreatureGroup(
                        powerBonus = 1,
                        toughnessBonus = 1,
                        filter = GroupFilter.AllCreatures
                    )
                )
            )
        )

        test("Humility suppresses creature lord's +1/+1 — all creatures are 1/1") {
            val driver = createDriver(BlankHumility, BearLord)
            driver.init()
            val p = driver.activePlayer!!

            val humility = driver.putPermanentOnBattlefield(p, "Humility")
            driver.addContinuousEffects(humility, HumilityEffects)
            val lord = driver.putCreatureOnBattlefield(p, "Bear Lord")
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")

            val projected = projector.project(driver.state)

            // Lord's ability is removed by Humility (L6) → no +1/+1 in L7c
            // All creatures are 1/1 from Humility's L7b
            projected.getPower(lord) shouldBe 1
            projected.getToughness(lord) shouldBe 1
            projected.getPower(bears) shouldBe 1
            projected.getToughness(bears) shouldBe 1
            projected.hasLostAllAbilities(lord) shouldBe true
        }

        test("Humility does NOT suppress enchantment anthem — creatures get +1/+1 on top of 1/1") {
            val driver = createDriver(BlankHumility, GloriousAnthem)
            driver.init()
            val p = driver.activePlayer!!

            val humility = driver.putPermanentOnBattlefield(p, "Humility")
            driver.addContinuousEffects(humility, HumilityEffects)
            driver.putPermanentOnBattlefield(p, "Glorious Anthem")
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")

            val projected = projector.project(driver.state)

            // Glorious Anthem is NOT a creature → Humility doesn't remove its ability
            // L7b: 1/1 from Humility, L7c: +1/+1 from Anthem → 2/2
            projected.getPower(bears) shouldBe 2
            projected.getToughness(bears) shouldBe 2
        }

        test("Humility + lord + anthem: lord suppressed, anthem works") {
            val driver = createDriver(BlankHumility, BearLord, GloriousAnthem)
            driver.init()
            val p = driver.activePlayer!!

            val humility = driver.putPermanentOnBattlefield(p, "Humility")
            driver.addContinuousEffects(humility, HumilityEffects)
            driver.putCreatureOnBattlefield(p, "Bear Lord")
            driver.putPermanentOnBattlefield(p, "Glorious Anthem")
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")

            val projected = projector.project(driver.state)

            // Lord's ability gone (Humility L6), anthem still works (not a creature)
            // L7b: 1/1, L7c: +1/+1 (anthem only, lord suppressed) → 2/2
            projected.getPower(bears) shouldBe 2
            projected.getToughness(bears) shouldBe 2
        }
    }

    // =========================================================================
    // SCENARIO 7: Type Change Enables Tribal Lord Pump
    //
    // If an effect adds a creature type (Layer 4), and a tribal lord pumps
    // creatures of that type (Layer 7c), the creature should get the pump
    // because Layer 4 applies before Layer 7c.
    //
    // This is a simpler version of the Blood Moon dependency — testing that
    // the engine re-resolves subtype-dependent filters after type changes.
    // =========================================================================

    context("Scenario 7: Type change enables tribal pump") {

        /** "All creatures are Angels in addition to their other types." */
        val AllCreaturesAreAngels = CardDefinition.enchantment(
            name = "Angel Decree",
            manaCost = ManaCost.parse("{2}{W}"),
            script = CardScript(
                staticAbilities = listOf(
                    GrantAdditionalTypesToGroup(
                        filter = GroupFilter.AllCreatures,
                        addSubtypes = listOf("Angel")
                    )
                )
            )
        )

        /** "Other Angel creatures get +1/+1." */
        val AngelLord = CardDefinition.creature(
            name = "Angel Lord",
            manaCost = ManaCost.parse("{3}{W}{W}"),
            subtypes = setOf(Subtype("Angel")),
            power = 3,
            toughness = 3,
            script = CardScript(
                staticAbilities = listOf(
                    ModifyStatsForCreatureGroup(
                        powerBonus = 1,
                        toughnessBonus = 1,
                        filter = GroupFilter.allCreaturesWithSubtype("Angel").other()
                    )
                )
            )
        )

        test("type-granting enchantment makes Bears qualify for Angel lord pump") {
            val driver = createDriver(AllCreaturesAreAngels, AngelLord)
            driver.init()
            val p = driver.activePlayer!!

            driver.putPermanentOnBattlefield(p, "Angel Decree")
            driver.putCreatureOnBattlefield(p, "Angel Lord")
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")

            val projected = projector.project(driver.state)

            // L4: Bears becomes an Angel
            projected.hasSubtype(bears, "Angel") shouldBe true
            projected.hasSubtype(bears, "Bear") shouldBe true
            // L7c: Angel Lord pumps other Angels → Bears gets +1/+1
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
        }

        test("without type-granting, Bears does NOT qualify for Angel lord pump") {
            val driver = createDriver(AngelLord)
            driver.init()
            val p = driver.activePlayer!!

            driver.putCreatureOnBattlefield(p, "Angel Lord")
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")

            val projected = projector.project(driver.state)

            // No Angel Decree → Bears is just a Bear → no pump
            projected.hasSubtype(bears, "Angel") shouldBe false
            projected.getPower(bears) shouldBe 2
            projected.getToughness(bears) shouldBe 2
        }
    }

    // =========================================================================
    // SCENARIO 8: Layer 6 Dependency — Remove All Abilities + Grant Keyword
    //
    // This tests the core dependency rule:
    //   GrantKeyword DEPENDS ON RemoveAllAbilities (Rule 613.8)
    //   because applying removal changes what the grant accomplishes.
    //
    // Therefore RemoveAll is applied FIRST regardless of timestamp,
    // then GrantKeyword re-adds the keyword.
    // =========================================================================

    context("Scenario 8: Ability removal + keyword grant dependency") {

        /** 3/3 with flying, first strike, and trample */
        val LoadedCreature = CardDefinition.creature(
            name = "Loaded Creature",
            manaCost = ManaCost.parse("{3}{G}{W}"),
            subtypes = setOf(Subtype("Angel")),
            power = 3,
            toughness = 3,
            keywords = setOf(Keyword.FLYING, Keyword.FIRST_STRIKE, Keyword.TRAMPLE)
        )

        /** "Enchanted creature loses all abilities." */
        val AbilityDrain = CardDefinition.enchantment(
            name = "Ability Drain",
            manaCost = ManaCost.parse("{1}{B}"),
            script = CardScript(
                staticAbilities = listOf(
                    LoseAllAbilities(target = StaticTarget.AttachedCreature)
                )
            )
        )

        /** "All creatures have flying." */
        val MassFlying = CardDefinition.enchantment(
            name = "Mass Flying",
            manaCost = ManaCost.parse("{2}{U}"),
            script = CardScript(
                staticAbilities = listOf(
                    GrantKeywordToCreatureGroup(
                        keyword = Keyword.FLYING,
                        filter = GroupFilter.AllCreatures
                    )
                )
            )
        )

        /** "Creatures you control have vigilance." */
        val VigilanceAnthem = CardDefinition.enchantment(
            name = "Vigilance Anthem",
            manaCost = ManaCost.parse("{1}{W}"),
            script = CardScript(
                staticAbilities = listOf(
                    GrantKeywordToCreatureGroup(
                        keyword = Keyword.VIGILANCE,
                        filter = GroupFilter.AllCreaturesYouControl
                    )
                )
            )
        )

        test("ability drain + mass flying: base keywords removed, flying re-granted") {
            val driver = createDriver(LoadedCreature, AbilityDrain, MassFlying)
            driver.init()
            val p = driver.activePlayer!!

            val angel = driver.putCreatureOnBattlefield(p, "Loaded Creature")
            val drain = driver.putPermanentOnBattlefield(p, "Ability Drain")
            driver.attachAura(drain, angel)
            driver.putPermanentOnBattlefield(p, "Mass Flying")

            val projected = projector.project(driver.state)

            // Base flying, first strike, trample removed by Ability Drain
            projected.hasKeyword(angel, Keyword.FIRST_STRIKE) shouldBe false
            projected.hasKeyword(angel, Keyword.TRAMPLE) shouldBe false
            // Flying re-granted by Mass Flying (dependency: drain first, then grant)
            projected.hasKeyword(angel, Keyword.FLYING) shouldBe true
        }

        test("ability drain + multiple keyword grants: all grants apply after removal") {
            val driver = createDriver(LoadedCreature, AbilityDrain, MassFlying, VigilanceAnthem)
            driver.init()
            val p = driver.activePlayer!!

            val angel = driver.putCreatureOnBattlefield(p, "Loaded Creature")
            val drain = driver.putPermanentOnBattlefield(p, "Ability Drain")
            driver.attachAura(drain, angel)
            driver.putPermanentOnBattlefield(p, "Mass Flying")
            driver.putPermanentOnBattlefield(p, "Vigilance Anthem")

            val projected = projector.project(driver.state)

            // Base abilities gone
            projected.hasKeyword(angel, Keyword.FIRST_STRIKE) shouldBe false
            projected.hasKeyword(angel, Keyword.TRAMPLE) shouldBe false
            // Both grants apply after removal
            projected.hasKeyword(angel, Keyword.FLYING) shouldBe true
            projected.hasKeyword(angel, Keyword.VIGILANCE) shouldBe true
        }

        test("dependency overrides timestamp: grant BEFORE drain still works") {
            // Mass Flying enters BEFORE Ability Drain.
            // Without dependency, pure timestamp would apply grant first, then drain removes.
            // With dependency, drain is applied first regardless → grant still works.
            val driver = createDriver(LoadedCreature, AbilityDrain, MassFlying)
            driver.init()
            val p = driver.activePlayer!!

            driver.putPermanentOnBattlefield(p, "Mass Flying")  // earlier timestamp
            val angel = driver.putCreatureOnBattlefield(p, "Loaded Creature")
            val drain = driver.putPermanentOnBattlefield(p, "Ability Drain")  // later timestamp
            driver.attachAura(drain, angel)

            val projected = projector.project(driver.state)

            // Dependency overrides timestamp: drain applied first, then grant
            projected.hasKeyword(angel, Keyword.FLYING) shouldBe true
            projected.hasKeyword(angel, Keyword.FIRST_STRIKE) shouldBe false
        }
    }

    // =========================================================================
    // SCENARIO 9: Grand Unification — All Classic Interactions Combined
    //
    // This test combines multiple classic scenarios into one comprehensive
    // board state that exercises all layers and dependencies simultaneously.
    // =========================================================================

    context("Scenario 9: Grand Unification") {

        test("Humility + enchantment anthem + type change + keyword grant + set P/T + counters") {
            // Setup:
            //   - Humility (L6: all creatures lose abilities, L7b: all creatures 1/1)
            //   - Angel Decree (L4: all creatures gain Angel subtype)
            //   - Glorious Anthem enchantment (L7c: all creatures +1/+1 — NOT a creature, survives Humility)
            //   - Mass Flying enchantment (L6: all creatures have flying — dependency: after Humility's removal)
            //   - Grizzly Bears with 2 +1/+1 counters
            //
            // Expected:
            //   L4: Bears gains Angel subtype
            //   L6: Bears loses abilities (Humility), then gains flying (Mass Flying, dependency)
            //   L7b: Bears is 1/1 (Humility)
            //   L7c: Bears gets +1/+1 (Glorious Anthem — enchantment, not suppressed)
            //   L7d: Bears gets +2/+2 from counters
            //   Final: 4/4, has flying, has Angel subtype, lost base abilities

            val BlankHumility = CardDefinition.enchantment(
                name = "Humility",
                manaCost = ManaCost.parse("{2}{W}{W}"),
                script = CardScript()
            )
            val AngelDecree = CardDefinition.enchantment(
                name = "Angel Decree",
                manaCost = ManaCost.parse("{2}{W}"),
                script = CardScript(
                    staticAbilities = listOf(
                        GrantAdditionalTypesToGroup(
                            filter = GroupFilter.AllCreatures,
                            addSubtypes = listOf("Angel")
                        )
                    )
                )
            )
            val GloriousAnthem = CardDefinition.enchantment(
                name = "Glorious Anthem",
                manaCost = ManaCost.parse("{1}{W}{W}"),
                script = CardScript(
                    staticAbilities = listOf(
                        ModifyStatsForCreatureGroup(
                            powerBonus = 1,
                            toughnessBonus = 1,
                            filter = GroupFilter.AllCreatures
                        )
                    )
                )
            )
            val MassFlying = CardDefinition.enchantment(
                name = "Mass Flying",
                manaCost = ManaCost.parse("{2}{U}"),
                script = CardScript(
                    staticAbilities = listOf(
                        GrantKeywordToCreatureGroup(
                            keyword = Keyword.FLYING,
                            filter = GroupFilter.AllCreatures
                        )
                    )
                )
            )

            val driver = createDriver(BlankHumility, AngelDecree, GloriousAnthem, MassFlying)
            driver.init()
            val p = driver.activePlayer!!

            driver.putPermanentOnBattlefield(p, "Angel Decree")
            val humility = driver.putPermanentOnBattlefield(p, "Humility")
            driver.addContinuousEffects(humility, HumilityEffects)
            driver.putPermanentOnBattlefield(p, "Glorious Anthem")
            driver.putPermanentOnBattlefield(p, "Mass Flying")

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            driver.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 2)

            val projected = projector.project(driver.state)

            // L4: Angel subtype added
            projected.hasSubtype(bears, "Angel") shouldBe true
            projected.hasSubtype(bears, "Bear") shouldBe true

            // L6: Abilities removed by Humility, then flying re-granted by Mass Flying (dependency)
            projected.hasLostAllAbilities(bears) shouldBe true
            projected.hasKeyword(bears, Keyword.FLYING) shouldBe true

            // L7b: 1/1 (Humility)
            // L7c: +1/+1 (Glorious Anthem — enchantment, not suppressed by Humility)
            // L7d: +2/+2 (counters)
            // Final: 1+1+2 / 1+1+2 = 4/4
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 4
        }
    }
})
