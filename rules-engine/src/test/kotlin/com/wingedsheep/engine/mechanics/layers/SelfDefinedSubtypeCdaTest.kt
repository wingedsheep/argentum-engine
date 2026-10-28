package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * A card's own printed "[this] is also a [subtype]" (a self-filter [GrantSubtype] at the top of its
 * script — Tajuru Paragon) is a characteristic-defining ability: CR 604.3 makes it function in every
 * zone, and CR 613.3 applies CDAs before every other effect in their layer. So the engine folds the
 * subtype into the entity's own type line and doesn't also project it as a timestamped layer-4 effect.
 */
class SelfDefinedSubtypeCdaTest : FunSpec({

    val wizardElf = card("CDA Probe Wizard Elf") {
        manaCost = "{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
        staticAbility { ability = GrantSubtype("Wizard") }
    }

    // A lord-style subtype grant to others is not a CDA: it stays a battlefield layer-4 effect.
    val knightLord = card("CDA Probe Knight Lord") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        staticAbility { ability = GrantSubtype("Knight", GroupFilter.AllCreatures) }
    }

    // "All creatures are Frogs" — sets creature subtypes, replacing the printed and CDA ones.
    val frogify = card("CDA Probe Frogify") {
        manaCost = "{U}"
        typeLine = "Enchantment"
        staticAbility { ability = TransformPermanent(setSubtypes = setOf("Frog"), filter = GroupFilter.AllCreatures) }
    }

    // "As long as this is tapped, it's also a Wizard" — conditional, so not a CDA (CR 604.3a).
    val conditionalWizard = card("CDA Probe Conditional Wizard") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
        staticAbility {
            ability = GrantSubtype("Wizard")
            condition = Conditions.SourceIsTapped
        }
    }

    // "Wizard spells you cast cost {1} less to cast."
    val wizardDiscount = card("CDA Probe Wizard Discount") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any.withSubtype("Wizard")),
                modification = CostModification.ReduceGeneric(1),
            )
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(wizardElf, knightLord, frogify, conditionalWizard, wizardDiscount))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
    }

    fun GameTestDriver.cardSubtypes(id: EntityId): Set<String> =
        state.getEntity(id)!!.get<CardComponent>()!!.typeLine.subtypes.mapTo(mutableSetOf()) { it.value }

    test("the definition derives the self subtype; a grant to others is not derived") {
        wizardElf.selfDefinedSubtypes shouldBe setOf(Subtype("Wizard"))
        wizardElf.typeLine.subtypes shouldBe setOf(Subtype("Elf"))
        knightLord.selfDefinedSubtypes shouldBe emptySet()
        conditionalWizard.selfDefinedSubtypes shouldBe emptySet()
        wizardElf.characteristicTypeLine.subtypes shouldBe setOf(Subtype("Elf"), Subtype("Wizard"))
    }

    test("CR 604.3: a spell being cast is judged by its CDA subtype (Wizard spells cost {1} less)") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, wizardDiscount.name)
        val calculator = CostCalculator(d.cardRegistry, PredicateEvaluator(cardRegistry = d.cardRegistry))
        val pricier = card("CDA Probe Pricier Wizard") {
            manaCost = "{1}{G}"
            typeLine = "Creature — Elf"
            power = 1
            toughness = 1
            staticAbility { ability = GrantSubtype("Wizard") }
        }
        calculator.calculateEffectiveCost(d.state, pricier, d.player1) shouldBe ManaCost.parse("{G}")
        calculator.calculateEffectiveCost(d.state, conditionalWizard, d.player1) shouldBe ManaCost.parse("{1}{G}")
    }

    test("a conditional self subtype grant is not folded into the card off the battlefield") {
        val d = driver()
        d.cardSubtypes(d.putCardInHand(d.player1, conditionalWizard.name)) shouldNotContain "Wizard"
    }

    test("CR 604.3: the subtype is on the card in hand and in the graveyard") {
        val d = driver()
        d.cardSubtypes(d.putCardInHand(d.player1, wizardElf.name)) shouldContain "Wizard"
        d.cardSubtypes(d.putCardInGraveyard(d.player1, wizardElf.name)) shouldContain "Wizard"
        d.cardSubtypes(d.putCardInHand(d.player1, knightLord.name)) shouldNotContain "Knight"
    }

    test("on the battlefield the CDA type projects alongside the printed one") {
        val d = driver()
        val probe = d.putCreatureOnBattlefield(d.player1, wizardElf.name)
        d.state.projectedState.getSubtypes(probe) shouldBe setOf("Elf", "Wizard")
    }

    test("CR 613.3: an earlier type-setting effect still overwrites the CDA type") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, frogify.name)
        val probe = d.putCreatureOnBattlefield(d.player1, wizardElf.name)
        d.state.projectedState.getSubtypes(probe) shouldBe setOf("Frog")
    }

    test("a grant to other creatures is still a battlefield layer-4 effect") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, knightLord.name)
        val probe = d.putCreatureOnBattlefield(d.player1, wizardElf.name)
        d.state.projectedState.getSubtypes(probe) shouldBe setOf("Elf", "Wizard", "Knight")
    }
})
