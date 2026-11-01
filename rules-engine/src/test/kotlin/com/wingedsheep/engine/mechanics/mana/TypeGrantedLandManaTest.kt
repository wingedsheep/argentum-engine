package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * A permanent that is a land only through a type-changing effect has the intrinsic mana ability of
 * its basic land types, exactly like a printed land: the "is it a land?" gate reads the projected
 * type line, never the printed one. Card-level coverage: `AshayaSoulOfTheWildScenarioTest`.
 */
class TypeGrantedLandManaTest : FunSpec({

    val islandMaker = card("Creatures Are Islands Test") {
        manaCost = "{2}"
        typeLine = "Artifact"
        val creatures = GroupFilter(GameObjectFilter.Creature.youControl())
        staticAbility { ability = GrantCardType(cardType = "LAND", filter = creatures) }
        staticAbility { ability = GrantSubtype("Island", creatures) }
    }
    val islandSubtypeOnly = card("Creatures Are Island-Typed Test") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility { ability = GrantSubtype("Island", GroupFilter(GameObjectFilter.Creature.youControl())) }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + islandMaker + islandSubtypeOnly)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("a creature made an Island land has the intrinsic {U} ability and counts toward colors lands could produce") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.putPermanentOnBattlefield(p1, "Creatures Are Islands Test")
        val bears = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val projected = d.state.projectedState

        IntrinsicManaAbilities.forEntity(d.state, projected, bears).map { it.id } shouldBe
            listOf(AbilityId.intrinsicMana('U'))
        LandManaColorInspector.colorsLandsCouldProduce(d.state, projected, listOf(bears), d.cardRegistry, PredicateEvaluator(d.cardRegistry)) shouldContain
            Color.BLUE
    }

    test("a basic land subtype without the land card type grants no mana ability") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.putPermanentOnBattlefield(p1, "Creatures Are Island-Typed Test")
        val bears = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val projected = d.state.projectedState

        IntrinsicManaAbilities.forEntity(d.state, projected, bears).shouldBeEmpty()
        LandManaColorInspector.colorsLandsCouldProduce(d.state, projected, listOf(bears), d.cardRegistry, PredicateEvaluator(d.cardRegistry)) shouldNotContain
            Color.BLUE
    }
})
