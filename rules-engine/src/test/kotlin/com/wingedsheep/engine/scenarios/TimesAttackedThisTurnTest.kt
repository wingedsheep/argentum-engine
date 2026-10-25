package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.components.combat.TimesAttackedThisTurnComponent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.state.components.player.QueuedPhase
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * `EntityNumericProperty.TimesAttackedThisTurn` — "for each time it has attacked this turn".
 *
 * A creature has attacked once for every combat phase it was declared as an attacker in
 * (CR 508.1). The count survives the end of that combat, is per object (a zone change makes a new
 * object that hasn't attacked, CR 400.7) and resets with the turn. As a per-affected-entity amount
 * in a layer-7 static it pumps each creature by its own count.
 */
class TimesAttackedThisTurnTest : FunSpec({

    val AttackAnthem = card("Attack Count Anthem") {
        manaCost = "{R}"
        typeLine = "Enchantment"
        staticAbility {
            ability = GrantDynamicStats(
                filter = GroupFilter(GameObjectFilter.Creature.youControl()),
                powerBonus = DynamicAmounts.propertyOf(
                    EffectTarget.AffectedEntity,
                    EntityNumericProperty.TimesAttackedThisTurn
                ),
                toughnessBonus = DynamicAmounts.fixed(0)
            )
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(AttackAnthem)
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.attacks(creature: EntityId): Int = PredicateEvaluator(cardRegistry = null).amounts.evaluate(
        state,
        DynamicAmount.EntityProperty(EffectTarget.Self, EntityNumericProperty.TimesAttackedThisTurn),
        EffectContext(sourceId = creature, controllerId = player1)
    )

    /** Queue one added combat phase after the precombat main phase, so the turn has two combats. */
    fun GameTestDriver.queueExtraCombat() {
        replaceState(state.updateEntity(player1) {
            it.with(AdditionalPhasesComponent(listOf(QueuedPhase(ExtraPhaseKind.COMBAT))))
        })
    }

    /** From anywhere before a declare attackers step, declare [attackers] in the next one. */
    fun GameTestDriver.attackInNextCombat(attacker: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(attacker), player2).error shouldBe null
        passPriorityUntil(Step.END_COMBAT)
    }

    test("each combat phase a creature is declared in counts once, and the count outlives the combat") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val idle = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(courser)
        d.removeSummoningSickness(idle)
        d.queueExtraCombat()

        d.attacks(courser) shouldBe 0
        d.attackInNextCombat(courser)
        withClue("Declared in the added combat") { d.attacks(courser) shouldBe 1 }
        d.attacks(idle) shouldBe 0

        d.bothPass() // leave the added combat's end step for the natural combat
        d.untapPermanent(courser)
        d.attackInNextCombat(courser)
        withClue("Declared again in the natural combat: it has attacked twice this turn") {
            d.attacks(courser) shouldBe 2
        }
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("Combat has ended; the creature has still attacked twice") { d.attacks(courser) shouldBe 2 }

        val json = Json { serializersModule = engineSerializersModule }
        val tracker: Component = d.state.getEntity(courser)!!.get<TimesAttackedThisTurnComponent>()!!
        json.decodeFromString<Component>(json.encodeToString(tracker)) shouldBe tracker
    }

    test("the count resets at end of turn") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(courser)
        d.attackInNextCombat(courser)
        d.attacks(courser) shouldBe 1

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.player2)
        d.attacks(courser) shouldBe 0
    }

    test("a creature that changes zones is a new object that hasn't attacked") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(courser)
        d.attackInNextCombat(courser)
        d.attacks(courser) shouldBe 1

        d.moveToGraveyard(courser)
        d.attacks(courser) shouldBe 0
    }

    test("a layer-7 static fed the amount pumps each creature by its own attacks") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Attack Count Anthem")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val idle = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(courser)
        d.queueExtraCombat()

        d.state.projectedState.getPower(courser) shouldBe 3
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(courser), d.player2).error shouldBe null
        withClue("The bonus applies from the declaration, so the first combat's damage already gets it") {
            d.state.projectedState.getPower(courser) shouldBe 4
        }
        d.passPriorityUntil(Step.END_COMBAT)
        d.getLifeTotal(d.player2) shouldBe 16

        d.bothPass()
        d.untapPermanent(courser)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(courser), d.player2).error shouldBe null
        d.state.projectedState.getPower(courser) shouldBe 5
        d.passPriorityUntil(Step.END_COMBAT)
        d.getLifeTotal(d.player2) shouldBe 11
        withClue("A creature that never attacked gets nothing") {
            d.state.projectedState.getPower(idle) shouldBe 3
        }
    }
})
