package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Comprehensive test suite for the Rule 613 layer system.
 *
 * Tests are organized by layer and then by cross-layer interactions.
 * Each test uses inline card definitions to isolate the specific
 * layer behavior being verified.
 *
 * Layer order (Rule 613):
 *   1. Copy effects
 *   2. Control-changing effects
 *   3. Text-changing effects
 *   4. Type-changing effects
 *   5. Color-changing effects
 *   6. Ability-adding/removing effects
 *   7. Power/toughness (7a CDA, 7b set, 7c modify, 7d counters, 7e switch)
 */
class LayerSystemTest : FunSpec({

    val projector = StateProjector()

    // =========================================================================
    // Test cards — inline definitions for layer testing
    // =========================================================================

    // --- Layer 4: Type-changing ---

    /** "All creatures are Walls in addition to their other types." */
    val AllCreaturesAreWalls = CardDefinition.enchantment(
        name = "Wall Decree",
        manaCost = ManaCost.parse("{2}{W}"),
        script = CardScript(
            staticAbilities = listOf(
                GrantAdditionalTypesToGroup(
                    filter = GroupFilter.AllCreatures,
                    addSubtypes = listOf("Wall")
                )
            )
        )
    )

    /** "All creatures are also artifacts." */
    val AllCreaturesAreArtifacts = CardDefinition.enchantment(
        name = "Artifact Decree",
        manaCost = ManaCost.parse("{3}"),
        script = CardScript(
            staticAbilities = listOf(
                GrantAdditionalTypesToGroup(
                    filter = GroupFilter.AllCreatures,
                    addCardTypes = listOf("ARTIFACT")
                )
            )
        )
    )

    // --- Layer 5: Color-changing ---

    /** "Enchanted creature is blue." (Aura — uses AttachedCreature target) */
    val PaintBlue = CardDefinition.enchantment(
        name = "Paint Blue",
        manaCost = ManaCost.parse("{U}"),
        script = CardScript(
            staticAbilities = listOf(
                GrantColor(color = Color.BLUE, target = StaticTarget.AttachedCreature)
            )
        )
    )

    // --- Layer 6: Ability-adding/removing ---

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

    /** "All creatures lose all abilities and have base P/T 1/1." (Humility-like) */
    val Humility = CardDefinition.enchantment(
        name = "Test Humility",
        manaCost = ManaCost.parse("{2}{W}{W}"),
        script = CardScript(
            staticAbilities = listOf(
                LoseAllAbilities(target = StaticTarget.SourceCreature),
                SetBasePowerToughnessStatic(power = 1, toughness = 1, target = StaticTarget.SourceCreature)
            )
        )
    )

    /** "Enchanted creature loses all abilities." */
    val LoseAbilitiesAura = CardDefinition.enchantment(
        name = "Ability Drain",
        manaCost = ManaCost.parse("{1}{B}"),
        script = CardScript(
            staticAbilities = listOf(
                LoseAllAbilities(target = StaticTarget.AttachedCreature)
            )
        )
    )

    // --- Layer 7: Power/Toughness ---

    /** "All creatures get +1/+1." (Glorious Anthem) */
    val GloriousAnthem = CardDefinition.enchantment(
        name = "Test Anthem",
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

    /** "Other creatures you control get +1/+1." */
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

    /** "Other Bear creatures get +1/+1." */
    val TribalBearLord = CardDefinition.creature(
        name = "Tribal Bear Lord",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2,
        script = CardScript(
            staticAbilities = listOf(
                ModifyStatsForCreatureGroup(
                    powerBonus = 1,
                    toughnessBonus = 1,
                    filter = GroupFilter.allCreaturesWithSubtype("Bear").other()
                )
            )
        )
    )

    /** "Enchanted creature has base power and toughness 0/4." */
    val FreezeAura = CardDefinition.enchantment(
        name = "Freeze Aura",
        manaCost = ManaCost.parse("{1}{U}"),
        script = CardScript(
            staticAbilities = listOf(
                SetBasePowerToughnessStatic(power = 0, toughness = 4, target = StaticTarget.AttachedCreature)
            )
        )
    )

    /** "Enchanted creature gets +2/+2." */
    val BoostAura = CardDefinition.enchantment(
        name = "Boost Aura",
        manaCost = ManaCost.parse("{1}{G}"),
        script = CardScript(
            staticAbilities = listOf(
                ModifyStats(powerBonus = 2, toughnessBonus = 2, target = StaticTarget.AttachedCreature)
            )
        )
    )

    // --- Cross-layer cards ---

    /** "All creatures are 1/1 and lose all abilities." + "All creatures get +1/+1." */
    val HumilityPlusAnthem = CardDefinition.enchantment(
        name = "Humble Anthem",
        manaCost = ManaCost.parse("{4}{W}"),
        script = CardScript(
            staticAbilities = listOf(
                LoseAllAbilities(target = StaticTarget.SourceCreature),
                SetBasePowerToughnessStatic(power = 1, toughness = 1, target = StaticTarget.SourceCreature),
                ModifyStatsForCreatureGroup(
                    powerBonus = 1,
                    toughnessBonus = 1,
                    filter = GroupFilter.AllCreatures
                )
            )
        )
    )

    // --- Creature with keywords ---

    /** 3/3 flyer */
    val FlyingSerpent = CardDefinition.creature(
        name = "Flying Serpent",
        manaCost = ManaCost.parse("{2}{U}"),
        subtypes = setOf(Subtype("Serpent")),
        power = 3,
        toughness = 3,
        keywords = setOf(Keyword.FLYING)
    )

    /** 2/2 with flying and lifelink */
    val FlyingLifelinker = CardDefinition.creature(
        name = "Flying Lifelinker",
        manaCost = ManaCost.parse("{1}{W}{B}"),
        subtypes = setOf(Subtype("Angel")),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.FLYING, Keyword.LIFELINK)
    )

    /** "Enchanted creature gets +1/+1 and has flying." */
    val WingsAndMight = CardDefinition.enchantment(
        name = "Wings and Might",
        manaCost = ManaCost.parse("{1}{U}"),
        script = CardScript(
            staticAbilities = listOf(
                ModifyStats(powerBonus = 1, toughnessBonus = 1, target = StaticTarget.AttachedCreature),
                GrantKeyword(keyword = Keyword.FLYING, target = StaticTarget.AttachedCreature)
            )
        )
    )

    // =========================================================================
    // Helper
    // =========================================================================

    fun createDriver(vararg extraCards: CardDefinition): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extraCards.toList())
        return driver
    }

    fun GameTestDriver.init() {
        initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            skipMulligans = true
        )
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /**
     * Helper to attach an aura-like permanent to a creature.
     * Adds the AttachedToComponent on the aura entity.
     */
    fun GameTestDriver.attachAura(auraId: com.wingedsheep.sdk.model.EntityId, targetId: com.wingedsheep.sdk.model.EntityId) {
        replaceState(state.updateEntity(auraId) { container ->
            container.with(com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(targetId))
        })
    }

    /**
     * Helper to add +1/+1 counters to a creature.
     */
    fun GameTestDriver.addCounters(entityId: com.wingedsheep.sdk.model.EntityId, type: CounterType, count: Int) {
        replaceState(state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(type, count))
        })
    }

    // =========================================================================
    // LAYER 4: Type-changing effects
    // =========================================================================

    context("Layer 4 — Type-changing effects") {

        test("static ability adds subtype to all creatures") {
            val driver = createDriver(AllCreaturesAreWalls)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            driver.putPermanentOnBattlefield(p, "Wall Decree")

            val projected = projector.project(driver.state)
            projected.hasSubtype(bears, "Wall") shouldBe true
            // Original subtype preserved
            projected.hasSubtype(bears, "Bear") shouldBe true
        }

        test("static ability adds card type to all creatures") {
            val driver = createDriver(AllCreaturesAreArtifacts)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            driver.putPermanentOnBattlefield(p, "Artifact Decree")

            val projected = projector.project(driver.state)
            projected.hasType(bears, "ARTIFACT") shouldBe true
            projected.hasType(bears, "CREATURE") shouldBe true
        }
    }

    // =========================================================================
    // LAYER 5: Color-changing effects
    // =========================================================================

    context("Layer 5 — Color-changing effects") {

        test("aura adds color to enchanted creature") {
            val driver = createDriver(PaintBlue)
            driver.init()
            val p = driver.activePlayer!!

            // Grizzly Bears is green
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val aura = driver.putPermanentOnBattlefield(p, "Paint Blue")
            driver.attachAura(aura, bears)

            val projected = projector.project(driver.state)
            projected.hasColor(bears, Color.BLUE) shouldBe true
        }
    }

    // =========================================================================
    // LAYER 6: Ability-adding/removing effects
    // =========================================================================

    context("Layer 6 — Ability-adding/removing effects") {

        test("global flying grants flying to all creatures") {
            val driver = createDriver(MassFlying)
            driver.init()
            val p = driver.activePlayer!!
            val opp = driver.getOpponent(p)

            val myBears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val theirBears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
            driver.putPermanentOnBattlefield(p, "Mass Flying")

            val projected = projector.project(driver.state)
            projected.hasKeyword(myBears, Keyword.FLYING) shouldBe true
            projected.hasKeyword(theirBears, Keyword.FLYING) shouldBe true
        }

        test("controller-scoped vigilance only affects your creatures") {
            val driver = createDriver(VigilanceAnthem)
            driver.init()
            val p = driver.activePlayer!!
            val opp = driver.getOpponent(p)

            val myBears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val theirBears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
            driver.putPermanentOnBattlefield(p, "Vigilance Anthem")

            val projected = projector.project(driver.state)
            projected.hasKeyword(myBears, Keyword.VIGILANCE) shouldBe true
            projected.hasKeyword(theirBears, Keyword.VIGILANCE) shouldBe false
        }

        test("lose all abilities removes keywords") {
            val driver = createDriver(FlyingSerpent, LoseAbilitiesAura)
            driver.init()
            val p = driver.activePlayer!!

            val serpent = driver.putCreatureOnBattlefield(p, "Flying Serpent")
            val aura = driver.putPermanentOnBattlefield(p, "Ability Drain")
            driver.attachAura(aura, serpent)

            val projected = projector.project(driver.state)
            projected.hasKeyword(serpent, Keyword.FLYING) shouldBe false
        }

        test("lose all abilities removes multiple keywords") {
            val driver = createDriver(FlyingLifelinker, LoseAbilitiesAura)
            driver.init()
            val p = driver.activePlayer!!

            val angel = driver.putCreatureOnBattlefield(p, "Flying Lifelinker")
            val aura = driver.putPermanentOnBattlefield(p, "Ability Drain")
            driver.attachAura(aura, angel)

            val projected = projector.project(driver.state)
            projected.hasKeyword(angel, Keyword.FLYING) shouldBe false
            projected.hasKeyword(angel, Keyword.LIFELINK) shouldBe false
        }
    }

    // =========================================================================
    // LAYER 7: Power/toughness effects
    // =========================================================================

    context("Layer 7 — Power/toughness effects") {

        test("7b: set base P/T overrides printed values") {
            val driver = createDriver(FreezeAura)
            driver.init()
            val p = driver.activePlayer!!

            // Grizzly Bears is 2/2
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val aura = driver.putPermanentOnBattlefield(p, "Freeze Aura")
            driver.attachAura(aura, bears)

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 0
            projected.getToughness(bears) shouldBe 4
        }

        test("7c: static +N/+N modifies stats") {
            val driver = createDriver(BoostAura)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val aura = driver.putPermanentOnBattlefield(p, "Boost Aura")
            driver.attachAura(aura, bears)

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 4
        }

        test("7b then 7c: set base P/T then modify — modifications apply on top of set value") {
            val driver = createDriver(FreezeAura, BoostAura)
            driver.init()
            val p = driver.activePlayer!!

            // Bears 2/2 → set to 0/4 (7b) → +2/+2 (7c) = 2/6
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val freeze = driver.putPermanentOnBattlefield(p, "Freeze Aura")
            driver.attachAura(freeze, bears)
            val boost = driver.putPermanentOnBattlefield(p, "Boost Aura")
            driver.attachAura(boost, bears)

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 2
            projected.getToughness(bears) shouldBe 6
        }

        test("7d: +1/+1 counters add to stats") {
            val driver = createDriver()
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            driver.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 2)

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 4
        }

        test("7d: -1/-1 counters subtract from stats") {
            val driver = createDriver()
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            driver.addCounters(bears, CounterType.MINUS_ONE_MINUS_ONE, 1)

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 1
            projected.getToughness(bears) shouldBe 1
        }

        test("7d: +1/+1 and -1/-1 counters net out") {
            val driver = createDriver()
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            driver.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 3)
            driver.addCounters(bears, CounterType.MINUS_ONE_MINUS_ONE, 1)

            val projected = projector.project(driver.state)
            // Net +2 counters: 2+2 = 4, 2+2 = 4
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 4
        }

        test("7b then 7d: set base P/T then counters — counters apply after set") {
            val driver = createDriver(FreezeAura)
            driver.init()
            val p = driver.activePlayer!!

            // Bears 2/2 → set 0/4 (7b) → +1/+1 counter (7d) = 1/5
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val freeze = driver.putPermanentOnBattlefield(p, "Freeze Aura")
            driver.attachAura(freeze, bears)
            driver.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 1
            projected.getToughness(bears) shouldBe 5
        }

        test("global anthem affects all creatures") {
            val driver = createDriver(GloriousAnthem)
            driver.init()
            val p = driver.activePlayer!!
            val opp = driver.getOpponent(p)

            val myBears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val theirLions = driver.putCreatureOnBattlefield(opp, "Savannah Lions")
            driver.putPermanentOnBattlefield(p, "Test Anthem")

            val projected = projector.project(driver.state)
            projected.getPower(myBears) shouldBe 3
            projected.getToughness(myBears) shouldBe 3
            projected.getPower(theirLions) shouldBe 2
            projected.getToughness(theirLions) shouldBe 2
        }

        test("lord effect: other creatures you control get +1/+1 — excludes self") {
            val driver = createDriver(BearLord)
            driver.init()
            val p = driver.activePlayer!!

            val lord = driver.putCreatureOnBattlefield(p, "Bear Lord")
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")

            val projected = projector.project(driver.state)
            // Lord doesn't pump itself
            projected.getPower(lord) shouldBe 2
            projected.getToughness(lord) shouldBe 2
            // Bears gets +1/+1
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
        }

        test("lord effect does not affect opponent's creatures") {
            val driver = createDriver(BearLord)
            driver.init()
            val p = driver.activePlayer!!
            val opp = driver.getOpponent(p)

            driver.putCreatureOnBattlefield(p, "Bear Lord")
            val theirBears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

            val projected = projector.project(driver.state)
            projected.getPower(theirBears) shouldBe 2
            projected.getToughness(theirBears) shouldBe 2
        }

        test("two lords stack: creature gets +2/+2") {
            val driver = createDriver(BearLord)
            driver.init()
            val p = driver.activePlayer!!

            val lord1 = driver.putCreatureOnBattlefield(p, "Bear Lord")
            val lord2 = driver.putCreatureOnBattlefield(p, "Bear Lord")
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")

            val projected = projector.project(driver.state)
            // Each lord pumps the other and both pump bears
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 4
            projected.getPower(lord1) shouldBe 3
            projected.getToughness(lord1) shouldBe 3
            projected.getPower(lord2) shouldBe 3
            projected.getToughness(lord2) shouldBe 3
        }

        test("tribal lord only pumps matching subtype") {
            val driver = createDriver(TribalBearLord)
            driver.init()
            val p = driver.activePlayer!!

            driver.putCreatureOnBattlefield(p, "Tribal Bear Lord")
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val lions = driver.putCreatureOnBattlefield(p, "Savannah Lions")

            val projected = projector.project(driver.state)
            // Bears are Bear type — get pumped
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
            // Lions are Cat type — no pump
            projected.getPower(lions) shouldBe 1
            projected.getToughness(lions) shouldBe 1
        }
    }

    // =========================================================================
    // CROSS-LAYER INTERACTIONS
    // =========================================================================

    context("Cross-layer interactions") {

        test("Layer 4 (type) + Layer 7c (tribal pump): adding subtype makes creature qualify for tribal lord") {
            val driver = createDriver(AllCreaturesAreWalls, TribalBearLord)
            driver.init()
            val p = driver.activePlayer!!

            // Wall Decree adds Wall subtype — but tribal Bear lord pumps Bears, not Walls
            driver.putPermanentOnBattlefield(p, "Wall Decree")
            driver.putCreatureOnBattlefield(p, "Tribal Bear Lord")
            val lions = driver.putCreatureOnBattlefield(p, "Savannah Lions")

            val projected = projector.project(driver.state)
            // Lions is a Cat+Wall, not a Bear — tribal Bear lord doesn't help
            projected.hasSubtype(lions, "Wall") shouldBe true
            projected.getPower(lions) shouldBe 1
            projected.getToughness(lions) shouldBe 1
        }

        test("Layer 6 (lose abilities) + Layer 7c (aura pump): loss of abilities doesn't remove aura P/T bonus") {
            val driver = createDriver(FlyingSerpent, LoseAbilitiesAura, BoostAura)
            driver.init()
            val p = driver.activePlayer!!

            // Serpent 3/3 flying → lose abilities → +2/+2 aura
            val serpent = driver.putCreatureOnBattlefield(p, "Flying Serpent")
            val drain = driver.putPermanentOnBattlefield(p, "Ability Drain")
            driver.attachAura(drain, serpent)
            val boost = driver.putPermanentOnBattlefield(p, "Boost Aura")
            driver.attachAura(boost, serpent)

            val projected = projector.project(driver.state)
            // Flying is gone (layer 6)
            projected.hasKeyword(serpent, Keyword.FLYING) shouldBe false
            // Aura P/T bonus is external, still applies (layer 7c)
            projected.getPower(serpent) shouldBe 5
            projected.getToughness(serpent) shouldBe 5
        }

        test("Layer 6 (lose abilities) + Layer 6 (grant keyword): dependency — remove before grant") {
            val driver = createDriver(FlyingSerpent, LoseAbilitiesAura, WingsAndMight)
            driver.init()
            val p = driver.activePlayer!!

            // Serpent 3/3 with base flying → lose all abilities → Wings and Might grants flying
            // GrantKeyword depends on RemoveAllAbilities, so removal happens first, then grant
            val serpent = driver.putCreatureOnBattlefield(p, "Flying Serpent")
            val drain = driver.putPermanentOnBattlefield(p, "Ability Drain")
            driver.attachAura(drain, serpent)
            val wings = driver.putPermanentOnBattlefield(p, "Wings and Might")
            driver.attachAura(wings, serpent)

            val projected = projector.project(driver.state)
            // Dependency: RemoveAllAbilities applied first, then GrantKeyword re-adds flying
            projected.hasKeyword(serpent, Keyword.FLYING) shouldBe true
            // +1/+1 from Wings and Might still applies
            projected.getPower(serpent) shouldBe 4
            projected.getToughness(serpent) shouldBe 4
        }

        test("Layer 6 (lose abilities) does NOT remove granted keywords from later-timestamp aura") {
            val driver = createDriver(FlyingLifelinker, LoseAbilitiesAura, WingsAndMight)
            driver.init()
            val p = driver.activePlayer!!

            // Angel 2/2 with flying+lifelink → lose all abilities → Wings grants flying
            val angel = driver.putCreatureOnBattlefield(p, "Flying Lifelinker")
            val drain = driver.putPermanentOnBattlefield(p, "Ability Drain")
            driver.attachAura(drain, angel)
            val wings = driver.putPermanentOnBattlefield(p, "Wings and Might")
            driver.attachAura(wings, angel)

            val projected = projector.project(driver.state)
            // Base flying + lifelink removed
            projected.hasKeyword(angel, Keyword.LIFELINK) shouldBe false
            // Flying re-granted by Wings and Might (GrantKeyword depends on RemoveAllAbilities)
            projected.hasKeyword(angel, Keyword.FLYING) shouldBe true
        }

        test("Layer 7b (set) + 7c (modify) + 7d (counters): all sublayers compose correctly") {
            val driver = createDriver(FreezeAura, BoostAura)
            driver.init()
            val p = driver.activePlayer!!

            // Bears 2/2 → set 0/4 (7b) → +2/+2 aura (7c) → +1/+1 counter (7d) = 3/7
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val freeze = driver.putPermanentOnBattlefield(p, "Freeze Aura")
            driver.attachAura(freeze, bears)
            val boost = driver.putPermanentOnBattlefield(p, "Boost Aura")
            driver.attachAura(boost, bears)
            driver.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 7
        }

        test("multiple anthems stack") {
            val driver = createDriver(GloriousAnthem)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            driver.putPermanentOnBattlefield(p, "Test Anthem")
            driver.putPermanentOnBattlefield(p, "Test Anthem")

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 4
        }

        test("anthem + lord + counters all stack") {
            val driver = createDriver(GloriousAnthem, BearLord)
            driver.init()
            val p = driver.activePlayer!!

            // Bears 2/2 + anthem +1/+1 + lord +1/+1 + 1 counter = 5/5
            driver.putPermanentOnBattlefield(p, "Test Anthem")
            driver.putCreatureOnBattlefield(p, "Bear Lord")
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            driver.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 5
            projected.getToughness(bears) shouldBe 5
        }

        test("aura with multiple layer effects applies each in correct layer") {
            val driver = createDriver(WingsAndMight)
            driver.init()
            val p = driver.activePlayer!!

            // Bears 2/2 → Wings and Might (+1/+1 in L7c, flying in L6)
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val wings = driver.putPermanentOnBattlefield(p, "Wings and Might")
            driver.attachAura(wings, bears)

            val projected = projector.project(driver.state)
            projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
        }
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    context("Edge cases") {

        test("creature entering after anthem still gets pumped") {
            val driver = createDriver(GloriousAnthem)
            driver.init()
            val p = driver.activePlayer!!

            // Anthem first, creature second
            driver.putPermanentOnBattlefield(p, "Test Anthem")
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
        }

        test("removing enchantment removes its effects from projection") {
            val driver = createDriver(GloriousAnthem)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val anthem = driver.putPermanentOnBattlefield(p, "Test Anthem")

            // Verify pumped
            var projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 3

            // Simulate enchantment removal (remove from battlefield)
            val zone = com.wingedsheep.engine.state.ZoneKey(p, Zone.BATTLEFIELD)
            driver.replaceState(driver.state.removeFromZone(zone, anthem))

            // No longer pumped
            projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 2
            projected.getToughness(bears) shouldBe 2
        }

        test("keyword counter grants keyword even without static ability") {
            val driver = createDriver()
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            driver.addCounters(bears, CounterType.FLYING, 1)

            val projected = projector.project(driver.state)
            projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
        }

        test("zero stat creature with counters has correct final P/T") {
            val driver = createDriver(FreezeAura)
            driver.init()
            val p = driver.activePlayer!!

            // Set to 0/4 then add counters → 2/6
            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val freeze = driver.putPermanentOnBattlefield(p, "Freeze Aura")
            driver.attachAura(freeze, bears)
            driver.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 2)

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 2
            projected.getToughness(bears) shouldBe 6
        }

        test("non-creature permanent is not affected by creature-only effects") {
            val driver = createDriver(GloriousAnthem, MassFlying)
            driver.init()
            val p = driver.activePlayer!!

            val anthem = driver.putPermanentOnBattlefield(p, "Test Anthem")
            driver.putPermanentOnBattlefield(p, "Mass Flying")

            val projected = projector.project(driver.state)
            // Enchantment is not a creature — no P/T or flying
            projected.getPower(anthem) shouldBe null
            projected.hasKeyword(anthem, Keyword.FLYING) shouldBe false
        }
    }

    // =========================================================================
    // FACE-DOWN CREATURES
    // =========================================================================

    context("Face-down creatures") {

        test("face-down creature is 2/2 with no keywords or colors") {
            val driver = createDriver(FlyingSerpent)
            driver.init()
            val p = driver.activePlayer!!

            // Manually create a face-down creature
            val serpent = driver.putCreatureOnBattlefield(p, "Flying Serpent")
            driver.replaceState(driver.state.updateEntity(serpent) { container ->
                container.with(com.wingedsheep.engine.state.components.identity.FaceDownComponent)
            })

            val projected = projector.project(driver.state)
            projected.getPower(serpent) shouldBe 2
            projected.getToughness(serpent) shouldBe 2
            projected.hasKeyword(serpent, Keyword.FLYING) shouldBe false
            projected.isFaceDown(serpent) shouldBe true
            projected.getColors(serpent) shouldBe emptySet()
        }

        test("face-down creature still gets global anthem bonus") {
            val driver = createDriver(FlyingSerpent, GloriousAnthem)
            driver.init()
            val p = driver.activePlayer!!

            val serpent = driver.putCreatureOnBattlefield(p, "Flying Serpent")
            driver.replaceState(driver.state.updateEntity(serpent) { container ->
                container.with(com.wingedsheep.engine.state.components.identity.FaceDownComponent)
            })
            driver.putPermanentOnBattlefield(p, "Test Anthem")

            val projected = projector.project(driver.state)
            // 2/2 base (face-down) + 1/1 (anthem) = 3/3
            projected.getPower(serpent) shouldBe 3
            projected.getToughness(serpent) shouldBe 3
            // Still no keywords from base card
            projected.hasKeyword(serpent, Keyword.FLYING) shouldBe false
        }
    }

    // =========================================================================
    // LAYER ORDERING VERIFICATION
    //
    // Each test proves a specific layer boundary: the asserted result would
    // be DIFFERENT if the layers were applied in the wrong order.
    // Each test documents the "CORRECT" vs "WRONG" result so it's clear
    // what breaks if the ordering is violated.
    //
    // Layer order: L2 Control → L4 Type → L5 Color → L6 Ability
    //              → L7a CDA → L7b Set → L7c Modify → L7d Counters
    // =========================================================================

    context("Layer ordering — each test fails if the order is wrong") {

        // -----------------------------------------------------------------
        // L2 → L6: Control must resolve before ability-granting effects,
        // so "creatures you control have X" sees the new controller.
        // -----------------------------------------------------------------

        test("L2 → L6: stolen creature gains thief's controller-scoped keyword") {
            // Player A has Vigilance Anthem ("creatures you control have vigilance")
            // Player A steals Player B's Grizzly Bears with Control Aura
            //
            // CORRECT (L2 first): Bears controlled by A → gets vigilance
            // WRONG   (L6 first): Bears controlled by B → no vigilance
            val ControlAura = CardDefinition.enchantment(
                name = "Control Aura",
                manaCost = ManaCost.parse("{3}{U}"),
                script = CardScript(
                    staticAbilities = listOf(ControlEnchantedPermanent)
                )
            )
            val driver = createDriver(VigilanceAnthem, ControlAura)
            driver.init()
            val p = driver.activePlayer!!
            val opp = driver.getOpponent(p)

            driver.putPermanentOnBattlefield(p, "Vigilance Anthem")
            val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
            val aura = driver.putPermanentOnBattlefield(p, "Control Aura")
            driver.attachAura(aura, bears)

            val projected = projector.project(driver.state)
            projected.getController(bears) shouldBe p
            projected.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
        }

        // -----------------------------------------------------------------
        // L2 → L7c: Control must resolve before P/T modification effects,
        // so "other creatures you control get +1/+1" sees the new controller.
        // -----------------------------------------------------------------

        test("L2 → L7c: stolen creature gets pumped by thief's lord") {
            // Player A has Bear Lord ("other creatures you control get +1/+1")
            // Player A steals Player B's Grizzly Bears with Control Aura
            //
            // CORRECT (L2 first): Bears controlled by A → lord pumps → 3/3
            // WRONG   (L7 first): Bears controlled by B → no pump → 2/2
            val ControlAura = CardDefinition.enchantment(
                name = "Control Aura L7",
                manaCost = ManaCost.parse("{3}{U}"),
                script = CardScript(
                    staticAbilities = listOf(ControlEnchantedPermanent)
                )
            )
            val driver = createDriver(BearLord, ControlAura)
            driver.init()
            val p = driver.activePlayer!!
            val opp = driver.getOpponent(p)

            driver.putCreatureOnBattlefield(p, "Bear Lord")
            val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
            val aura = driver.putPermanentOnBattlefield(p, "Control Aura L7")
            driver.attachAura(aura, bears)

            val projected = projector.project(driver.state)
            projected.getController(bears) shouldBe p
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
        }

        // -----------------------------------------------------------------
        // L4 → L7c: Type-changing (adding subtype) must resolve before
        // Layer 7 P/T modifications, because the engine re-resolves
        // subtype-dependent filters before applying L7 effects.
        // -----------------------------------------------------------------

        test("L4 → L7c: adding Bear subtype makes creature qualify for tribal lord pump") {
            // Savannah Lions is a Cat. Bear Decree adds "Bear" to all creatures (L4).
            // Tribal Bear Lord gives other Bears +1/+1 (L7c).
            //
            // CORRECT (L4 first): Lions gains Bear → lord pumps → 2/2
            // WRONG   (L7 first): Lions is still just a Cat → no pump → 1/1
            val AllCreaturesAreBears = CardDefinition.enchantment(
                name = "Bear Decree",
                manaCost = ManaCost.parse("{2}{G}"),
                script = CardScript(
                    staticAbilities = listOf(
                        GrantAdditionalTypesToGroup(
                            filter = GroupFilter.AllCreatures,
                            addSubtypes = listOf("Bear")
                        )
                    )
                )
            )
            val driver = createDriver(TribalBearLord, AllCreaturesAreBears)
            driver.init()
            val p = driver.activePlayer!!

            driver.putPermanentOnBattlefield(p, "Bear Decree")
            driver.putCreatureOnBattlefield(p, "Tribal Bear Lord")
            val lions = driver.putCreatureOnBattlefield(p, "Savannah Lions")

            val projected = projector.project(driver.state)
            projected.hasSubtype(lions, "Bear") shouldBe true
            projected.hasSubtype(lions, "Cat") shouldBe true
            projected.getPower(lions) shouldBe 2
            projected.getToughness(lions) shouldBe 2
        }

        // -----------------------------------------------------------------
        // L6 within-layer dependency: RemoveAllAbilities before GrantKeyword
        // (Rule 613.8). GrantKeyword depends on RemoveAllAbilities because
        // applying the removal changes what the grant accomplishes.
        // -----------------------------------------------------------------

        test("L6 dependency: GrantKeyword re-adds flying after RemoveAllAbilities (grant later timestamp)") {
            // Serpent 3/3 flying → Ability Drain (remove all) → Mass Flying (grant flying)
            //
            // CORRECT (dependency: remove first): flying removed, then re-granted → has flying
            // WRONG   (timestamp order: drain is later → last): grant first, then removed → no flying
            val driver = createDriver(FlyingSerpent, LoseAbilitiesAura, MassFlying)
            driver.init()
            val p = driver.activePlayer!!

            val serpent = driver.putCreatureOnBattlefield(p, "Flying Serpent")
            val drain = driver.putPermanentOnBattlefield(p, "Ability Drain")
            driver.attachAura(drain, serpent)
            driver.putPermanentOnBattlefield(p, "Mass Flying")

            val projected = projector.project(driver.state)
            projected.hasKeyword(serpent, Keyword.FLYING) shouldBe true
        }

        test("L6 dependency: GrantKeyword re-adds flying after RemoveAllAbilities (grant earlier timestamp)") {
            // Same scenario but Mass Flying enters BEFORE Ability Drain.
            // Without dependency, pure timestamp would apply grant first, then drain removes → no flying.
            //
            // CORRECT (dependency overrides timestamp): remove first → grant → flying
            // WRONG   (pure timestamp): grant → remove → no flying
            val driver = createDriver(FlyingSerpent, LoseAbilitiesAura, MassFlying)
            driver.init()
            val p = driver.activePlayer!!

            driver.putPermanentOnBattlefield(p, "Mass Flying")  // earlier timestamp
            val serpent = driver.putCreatureOnBattlefield(p, "Flying Serpent")
            val drain = driver.putPermanentOnBattlefield(p, "Ability Drain")  // later timestamp
            driver.attachAura(drain, serpent)

            val projected = projector.project(driver.state)
            projected.hasKeyword(serpent, Keyword.FLYING) shouldBe true
        }

        // -----------------------------------------------------------------
        // L7b → L7c: Set-base-P/T must apply before +N/+N modifications.
        // -----------------------------------------------------------------

        test("L7b → L7c: set base P/T first, then +N/+N on top") {
            // Bears 2/2 → Freeze Aura sets 0/4 (7b) → Boost Aura +2/+2 (7c)
            //
            // CORRECT (7b first): 0/4 then +2/+2 = 2/6
            // WRONG   (7c first): 2/2 + 2/+2 = 4/4, then set overwrites → 0/4
            val driver = createDriver(FreezeAura, BoostAura)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val freeze = driver.putPermanentOnBattlefield(p, "Freeze Aura")
            driver.attachAura(freeze, bears)
            val boost = driver.putPermanentOnBattlefield(p, "Boost Aura")
            driver.attachAura(boost, bears)

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 2
            projected.getToughness(bears) shouldBe 6
        }

        // -----------------------------------------------------------------
        // L7b → L7d: Set-base-P/T must apply before counters.
        // -----------------------------------------------------------------

        test("L7b → L7d: set base P/T first, then counters on top") {
            // Bears 2/2 → Freeze Aura sets 0/4 (7b) → two +1/+1 counters (7d)
            //
            // CORRECT (7b first): 0/4 + 2/+2 = 2/6
            // WRONG   (7d first): 2/2 + 2/+2 = 4/4, then set → 0/4
            val driver = createDriver(FreezeAura)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val freeze = driver.putPermanentOnBattlefield(p, "Freeze Aura")
            driver.attachAura(freeze, bears)
            driver.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 2)

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 2
            projected.getToughness(bears) shouldBe 6
        }

        // -----------------------------------------------------------------
        // L7b → L7c → L7d: Full sublayer pipeline.
        // -----------------------------------------------------------------

        test("L7b → L7c → L7d: full sublayer pipeline composes correctly") {
            // Bears 2/2 → set 0/4 (7b) → anthem +1/+1 (7c) → 2 counters (7d)
            //
            // CORRECT: 0/4 → 1/5 → 3/7
            // WRONG (7c first): 3/3 → set 0/4 → counters 2/6
            // WRONG (7d first): 4/4 → set 0/4 → anthem 1/5
            val driver = createDriver(FreezeAura, GloriousAnthem)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val freeze = driver.putPermanentOnBattlefield(p, "Freeze Aura")
            driver.attachAura(freeze, bears)
            driver.putPermanentOnBattlefield(p, "Test Anthem")
            driver.addCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 2)

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 7
        }

        // -----------------------------------------------------------------
        // Timestamp within same sublayer: later effect wins for Set-P/T.
        // -----------------------------------------------------------------

        test("L7b timestamp: later set-P/T overwrites earlier one") {
            // Two 7b auras on the same creature — later timestamp wins.
            //
            // CORRECT: Warm Aura (later) sets 5/1
            // WRONG:   Freeze Aura (earlier) sets 0/4
            val WarmAura = CardDefinition.enchantment(
                name = "Warm Aura",
                manaCost = ManaCost.parse("{1}{R}"),
                script = CardScript(
                    staticAbilities = listOf(
                        SetBasePowerToughnessStatic(power = 5, toughness = 1, target = StaticTarget.AttachedCreature)
                    )
                )
            )
            val driver = createDriver(FreezeAura, WarmAura)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            val freeze = driver.putPermanentOnBattlefield(p, "Freeze Aura")
            driver.attachAura(freeze, bears)
            val warm = driver.putPermanentOnBattlefield(p, "Warm Aura")
            driver.attachAura(warm, bears)

            val projected = projector.project(driver.state)
            projected.getPower(bears) shouldBe 5
            projected.getToughness(bears) shouldBe 1
        }

        // -----------------------------------------------------------------
        // Grand unification test: one scenario hitting L2, L4, L6 (with
        // dependency), L7b, L7c, L7d — every assertion is order-sensitive.
        //
        // Key design: each effect is chosen so its result DEPENDS on
        // a prior layer being applied first. If any layer is skipped or
        // reordered, at least one assertion changes.
        // -----------------------------------------------------------------

        test("all layers combined: steal + add subtype + remove/grant abilities + set P/T + modify + counters") {
            // Setup:
            //   Player B owns a 3/3 Flying Serpent (base: flying, green).
            //   Player A has:
            //     - Control Aura stealing the Serpent                  (L2: controller → A)
            //     - Bear Decree (all creatures gain Bear subtype)      (L4: Serpent gains Bear)
            //     - Ability Drain aura (lose all abilities)            (L6: base flying removed)
            //     - Vigilance Anthem (creatures YOU control have vig.) (L6: vigilance — only if L2 changed controller!)
            //     - Freeze Aura (set base P/T to 0/4)                 (L7b)
            //     - Tribal Bear Lord (other Bears +1/+1)              (L7c: only if L4 added Bear subtype!)
            //     - Bear Lord (other creatures you control +1/+1)   (L7c: only if L2 changed controller!)
            //     - Test Anthem (all creatures +1/+1)                 (L7c: +1/+1)
            //     - 1 +1/+1 counter on Serpent                       (L7d: +1/+1)
            //
            // Expected final state of Serpent:
            //   Controller: Player A                                    (L2)
            //   Subtypes: Serpent + Bear                                (base + L4)
            //   Flying: NO (removed by L6, NOT re-granted — Vig. Anthem grants vigilance, not flying)
            //   Vigilance: YES (L2 made it "yours" → L6 Vig. Anthem applies; dependency: remove first, then grant)
            //   P/T: 0/4 (L7b) + 1/1 (tribal lord L7c) + 1/1 (bear lord L7c) + 1/1 (anthem L7c) + 1/1 (counter L7d) = 4/8
            //
            // What breaks if layers are wrong:
            //   L2 wrong:  Serpent still Player B's → Vig. Anthem doesn't apply → no vigilance; Bear Lord doesn't pump → P/T = 3/7
            //   L4 wrong:  Serpent not a Bear → tribal lord doesn't pump → P/T = 3/7
            //   L6 wrong:  GrantKeyword applied before RemoveAll → vigilance stripped → no vigilance
            //   L7b wrong: set overwrites modifications → P/T = 0/4

            val controlAura = CardDefinition.enchantment(
                name = "Control Aura",
                manaCost = ManaCost.parse("{3}{U}"),
                script = CardScript(
                    staticAbilities = listOf(ControlEnchantedPermanent)
                )
            )
            val allCreaturesAreBears = CardDefinition.enchantment(
                name = "Bear Decree",
                manaCost = ManaCost.parse("{2}{G}"),
                script = CardScript(
                    staticAbilities = listOf(
                        GrantAdditionalTypesToGroup(
                            filter = GroupFilter.AllCreatures,
                            addSubtypes = listOf("Bear")
                        )
                    )
                )
            )
            val driver = createDriver(
                FlyingSerpent, controlAura, allCreaturesAreBears,
                LoseAbilitiesAura, VigilanceAnthem, FreezeAura,
                TribalBearLord, BearLord, GloriousAnthem
            )
            driver.init()
            val p = driver.activePlayer!!
            val opp = driver.getOpponent(p)

            // Player B's creature (to be stolen)
            val serpent = driver.putCreatureOnBattlefield(opp, "Flying Serpent")

            // L2: steal it
            val ctrlAura = driver.putPermanentOnBattlefield(p, "Control Aura")
            driver.attachAura(ctrlAura, serpent)

            // L4: add Bear subtype to all creatures
            driver.putPermanentOnBattlefield(p, "Bear Decree")

            // L6: remove all abilities, then grant vigilance (controller-scoped)
            val drain = driver.putPermanentOnBattlefield(p, "Ability Drain")
            driver.attachAura(drain, serpent)
            driver.putPermanentOnBattlefield(p, "Vigilance Anthem")

            // L7b: set base P/T
            val freeze = driver.putPermanentOnBattlefield(p, "Freeze Aura")
            driver.attachAura(freeze, serpent)

            // L7c: tribal lord + bear lord + anthem
            driver.putCreatureOnBattlefield(p, "Tribal Bear Lord")
            driver.putCreatureOnBattlefield(p, "Bear Lord")
            driver.putPermanentOnBattlefield(p, "Test Anthem")

            // L7d: +1/+1 counter
            driver.addCounters(serpent, CounterType.PLUS_ONE_PLUS_ONE, 1)

            val projected = projector.project(driver.state)

            // L2: controller is Player A
            projected.getController(serpent) shouldBe p

            // L4: gained Bear subtype, kept Serpent
            projected.hasSubtype(serpent, "Bear") shouldBe true
            projected.hasSubtype(serpent, "Serpent") shouldBe true

            // L6: base flying removed by Ability Drain — NOT re-granted (no global flying source)
            projected.hasKeyword(serpent, Keyword.FLYING) shouldBe false
            // L2→L6: vigilance granted because L2 changed controller to A, making
            // Vigilance Anthem ("creatures you control") apply. Also tests L6
            // dependency: RemoveAllAbilities runs first, then GrantKeyword re-adds vigilance.
            projected.hasKeyword(serpent, Keyword.VIGILANCE) shouldBe true

            // L7: 0/4 (set) + 1/1 (tribal lord — Serpent is a Bear via L4) + 1/1 (bear lord — stolen via L2) + 1/1 (anthem) + 1/1 (counter) = 4/8
            projected.getPower(serpent) shouldBe 4
            projected.getToughness(serpent) shouldBe 8
        }
    }

    // =========================================================================
    // DEPENDENCY GRAPH RESOLUTION (Rule 613.8)
    //
    // The engine resolves dependencies using a directed graph:
    //   1. Create a vertex for each effect
    //   2. For each dependency, create edge: dependent → what it depends on
    //   3. Remove all edges that form cycles
    //   4. Find all vertices with no outgoing edges (no unresolved deps)
    //   5. Among those, apply the one with earliest timestamp
    //   6. Repeat from step 1 with remaining effects
    //
    // These tests verify the graph algorithm itself: cycles, multi-way
    // dependencies, and timestamp fallback.
    //
    // Current dependency triggers in EffectSorter.dependsOn():
    //   - AddType/RemoveType: any effect sharing entities depends on it
    //   - RemoveAllAbilities: GrantKeyword sharing entities depends on it
    // =========================================================================

    context("Dependency graph resolution") {

        // -----------------------------------------------------------------
        // Linear chain: A depends on B
        //
        //   GrantKeyword ──→ RemoveAllAbilities
        //   (A depends on B: apply B first, then A)
        //
        // This is the simplest graph: one edge, no cycles.
        // -----------------------------------------------------------------

        test("linear dependency: GrantKeyword waits for RemoveAllAbilities") {
            // Already tested above in L6 dependency tests, but restated here
            // in graph terms: the graph has one edge, B has no outgoing edges
            // so B is applied first, then A.
            val driver = createDriver(FlyingSerpent, LoseAbilitiesAura, MassFlying)
            driver.init()
            val p = driver.activePlayer!!

            val serpent = driver.putCreatureOnBattlefield(p, "Flying Serpent")
            val drain = driver.putPermanentOnBattlefield(p, "Ability Drain")
            driver.attachAura(drain, serpent)
            driver.putPermanentOnBattlefield(p, "Mass Flying")

            val projected = projector.project(driver.state)
            // Graph: MassFlying(grant) ──→ AbilityDrain(remove)
            // Apply remove first, then grant → flying is restored
            projected.hasKeyword(serpent, Keyword.FLYING) shouldBe true
        }

        // -----------------------------------------------------------------
        // Cycle: two AddType effects on the same entity
        //
        //   AddType("Wall") ←──→ AddType("Warrior")
        //
        // Both are type-changing and share entities, so each depends on
        // the other → mutual dependency → cycle. The engine breaks the
        // cycle and falls back to timestamp order. Both should still apply.
        // -----------------------------------------------------------------

        test("cycle: two AddType effects on same entity — cycle broken, both apply in timestamp order") {
            // Two enchantments both add different subtypes to all creatures.
            // They create a dependency cycle (A depends on B, B depends on A).
            // After cycle-breaking, they apply in timestamp order.
            // Since both are additive, the creature gets BOTH subtypes.
            val addWall = CardDefinition.enchantment(
                name = "Wall Edict",
                manaCost = ManaCost.parse("{1}{W}"),
                script = CardScript(
                    staticAbilities = listOf(
                        GrantAdditionalTypesToGroup(
                            filter = GroupFilter.AllCreatures,
                            addSubtypes = listOf("Wall")
                        )
                    )
                )
            )
            val addWarrior = CardDefinition.enchantment(
                name = "Warrior Edict",
                manaCost = ManaCost.parse("{1}{R}"),
                script = CardScript(
                    staticAbilities = listOf(
                        GrantAdditionalTypesToGroup(
                            filter = GroupFilter.AllCreatures,
                            addSubtypes = listOf("Warrior")
                        )
                    )
                )
            )
            val driver = createDriver(addWall, addWarrior)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            // Wall Edict enters first (earlier timestamp)
            driver.putPermanentOnBattlefield(p, "Wall Edict")
            // Warrior Edict enters second (later timestamp)
            driver.putPermanentOnBattlefield(p, "Warrior Edict")

            val projected = projector.project(driver.state)
            // Graph: Wall ←→ Warrior (cycle)
            // Cycle broken → timestamp order → Wall first, then Warrior
            // Both additive → creature has both subtypes
            projected.hasSubtype(bears, "Bear") shouldBe true
            projected.hasSubtype(bears, "Wall") shouldBe true
            projected.hasSubtype(bears, "Warrior") shouldBe true
        }

        // -----------------------------------------------------------------
        // Chain with independent: A depends on B, C is independent
        //
        //   GrantKeyword ──→ RemoveAllAbilities    AddType (independent)
        //
        // C has no outgoing edges and neither does B. Between them,
        // timestamp determines which goes first. Then A goes last.
        // -----------------------------------------------------------------

        test("chain + independent: independent effect and dependency chain coexist") {
            // RemoveAllAbilities + GrantKeyword (dependent) + AddType (independent)
            // The AddType and RemoveAll are in different layers (L4 vs L6) so
            // they don't interact through the dependency graph within a single layer.
            // Within L6: GrantKeyword depends on RemoveAll → remove first, then grant.
            // L4 is applied before L6 regardless (layer order).
            val addWall = CardDefinition.enchantment(
                name = "Wall Edict",
                manaCost = ManaCost.parse("{1}{W}"),
                script = CardScript(
                    staticAbilities = listOf(
                        GrantAdditionalTypesToGroup(
                            filter = GroupFilter.AllCreatures,
                            addSubtypes = listOf("Wall")
                        )
                    )
                )
            )
            val driver = createDriver(FlyingSerpent, LoseAbilitiesAura, MassFlying, addWall)
            driver.init()
            val p = driver.activePlayer!!

            val serpent = driver.putCreatureOnBattlefield(p, "Flying Serpent")
            driver.putPermanentOnBattlefield(p, "Wall Edict") // L4: independent
            val drain = driver.putPermanentOnBattlefield(p, "Ability Drain") // L6: remove
            driver.attachAura(drain, serpent)
            driver.putPermanentOnBattlefield(p, "Mass Flying") // L6: grant (depends on remove)

            val projected = projector.project(driver.state)
            // L4 applies independently: Wall subtype added
            projected.hasSubtype(serpent, "Wall") shouldBe true
            // L6 graph: MassFlying ──→ AbilityDrain → remove first, grant second
            projected.hasKeyword(serpent, Keyword.FLYING) shouldBe true
        }

        // -----------------------------------------------------------------
        // Multiple grants depending on one removal
        //
        //   GrantFlying ──→ RemoveAllAbilities ←── GrantVigilance
        //
        // Two GrantKeyword effects both depend on RemoveAllAbilities.
        // RemoveAll has no deps → applied first.
        // Then both grants are ready → applied in timestamp order.
        // -----------------------------------------------------------------

        test("fan-in: multiple GrantKeywords depend on single RemoveAllAbilities") {
            // Creature with flying → lose all abilities → two separate keyword grants
            // Both grants depend on the removal. After removal, both apply by timestamp.
            val driver = createDriver(FlyingSerpent, LoseAbilitiesAura, MassFlying, VigilanceAnthem)
            driver.init()
            val p = driver.activePlayer!!

            val serpent = driver.putCreatureOnBattlefield(p, "Flying Serpent")
            val drain = driver.putPermanentOnBattlefield(p, "Ability Drain")
            driver.attachAura(drain, serpent)
            driver.putPermanentOnBattlefield(p, "Mass Flying")       // grants flying
            driver.putPermanentOnBattlefield(p, "Vigilance Anthem")  // grants vigilance (yours)

            val projected = projector.project(driver.state)
            // Graph:
            //   MassFlying ──→ AbilityDrain ←── VigilanceAnthem
            //
            // Step 1: AbilityDrain has no outgoing edges → applied first (removes all)
            // Step 2: Both grants have no outgoing edges → timestamp order
            // Both should apply: base flying removed, then re-granted + vigilance granted
            projected.hasKeyword(serpent, Keyword.FLYING) shouldBe true
            projected.hasKeyword(serpent, Keyword.VIGILANCE) shouldBe true
        }

        // -----------------------------------------------------------------
        // Three-way type-changing cycle
        //
        //   AddWall ←→ AddWarrior ←→ AddWizard ←→ AddWall
        //          (all share entities → all depend on each other)
        //
        // Three AddType effects on the same creature form a fully connected
        // dependency cycle. All cycle edges are removed → pure timestamp.
        // -----------------------------------------------------------------

        test("three-way cycle: three AddType effects — all cycle edges broken, timestamp order") {
            val addWall = CardDefinition.enchantment(
                name = "Wall Edict",
                manaCost = ManaCost.parse("{1}{W}"),
                script = CardScript(
                    staticAbilities = listOf(
                        GrantAdditionalTypesToGroup(
                            filter = GroupFilter.AllCreatures,
                            addSubtypes = listOf("Wall")
                        )
                    )
                )
            )
            val addWarrior = CardDefinition.enchantment(
                name = "Warrior Edict",
                manaCost = ManaCost.parse("{1}{R}"),
                script = CardScript(
                    staticAbilities = listOf(
                        GrantAdditionalTypesToGroup(
                            filter = GroupFilter.AllCreatures,
                            addSubtypes = listOf("Warrior")
                        )
                    )
                )
            )
            val addWizard = CardDefinition.enchantment(
                name = "Wizard Edict",
                manaCost = ManaCost.parse("{1}{U}"),
                script = CardScript(
                    staticAbilities = listOf(
                        GrantAdditionalTypesToGroup(
                            filter = GroupFilter.AllCreatures,
                            addSubtypes = listOf("Wizard")
                        )
                    )
                )
            )
            val driver = createDriver(addWall, addWarrior, addWizard)
            driver.init()
            val p = driver.activePlayer!!

            val bears = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
            // Enter in this order (timestamps: Wall < Warrior < Wizard)
            driver.putPermanentOnBattlefield(p, "Wall Edict")
            driver.putPermanentOnBattlefield(p, "Warrior Edict")
            driver.putPermanentOnBattlefield(p, "Wizard Edict")

            val projected = projector.project(driver.state)
            // Graph: Wall ←→ Warrior ←→ Wizard ←→ Wall (3-cycle)
            // All edges removed → apply by timestamp: Wall, Warrior, Wizard
            // All additive → creature has all three subtypes
            projected.hasSubtype(bears, "Bear") shouldBe true
            projected.hasSubtype(bears, "Wall") shouldBe true
            projected.hasSubtype(bears, "Warrior") shouldBe true
            projected.hasSubtype(bears, "Wizard") shouldBe true
        }

        // -----------------------------------------------------------------
        // Mixed: cycle in type-changing + independent dependency chain
        //
        //   L4: AddWall ←→ AddWarrior  (cycle → timestamp)
        //   L6: GrantFlying ──→ RemoveAll  (linear dependency)
        //
        // The L4 cycle and L6 chain don't interfere — they're in
        // different layers. Each layer resolves its own graph independently.
        // -----------------------------------------------------------------

        test("mixed: L4 cycle + L6 chain resolve independently per layer") {
            val addWall = CardDefinition.enchantment(
                name = "Wall Edict",
                manaCost = ManaCost.parse("{1}{W}"),
                script = CardScript(
                    staticAbilities = listOf(
                        GrantAdditionalTypesToGroup(
                            filter = GroupFilter.AllCreatures,
                            addSubtypes = listOf("Wall")
                        )
                    )
                )
            )
            val addWarrior = CardDefinition.enchantment(
                name = "Warrior Edict",
                manaCost = ManaCost.parse("{1}{R}"),
                script = CardScript(
                    staticAbilities = listOf(
                        GrantAdditionalTypesToGroup(
                            filter = GroupFilter.AllCreatures,
                            addSubtypes = listOf("Warrior")
                        )
                    )
                )
            )
            val driver = createDriver(FlyingSerpent, addWall, addWarrior, LoseAbilitiesAura, MassFlying)
            driver.init()
            val p = driver.activePlayer!!

            val serpent = driver.putCreatureOnBattlefield(p, "Flying Serpent")
            // L4: two type-changing effects → cycle
            driver.putPermanentOnBattlefield(p, "Wall Edict")
            driver.putPermanentOnBattlefield(p, "Warrior Edict")
            // L6: remove + grant → linear dependency
            val drain = driver.putPermanentOnBattlefield(p, "Ability Drain")
            driver.attachAura(drain, serpent)
            driver.putPermanentOnBattlefield(p, "Mass Flying")

            val projected = projector.project(driver.state)
            // L4 cycle resolved: both subtypes added
            projected.hasSubtype(serpent, "Wall") shouldBe true
            projected.hasSubtype(serpent, "Warrior") shouldBe true
            // L6 chain resolved: remove first, then grant flying
            projected.hasKeyword(serpent, Keyword.FLYING) shouldBe true
        }
    }
})
