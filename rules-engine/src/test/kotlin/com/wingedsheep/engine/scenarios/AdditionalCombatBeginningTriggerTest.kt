package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.InAdditionalCombatPhaseComponent
import com.wingedsheep.engine.state.components.player.SkipCombatPhasesComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The "At the beginning of that combat, …" rider on an added combat phase
 * (`Effects.AddCombatPhaseWithBeginningTrigger`, Moraug, Fury of Akoum).
 *
 * The rider is a delayed triggered ability (CR 603.7) linked to the one combat phase its effect
 * created (CR 500.8 adds that phase directly after the current one): it triggers as that phase's
 * beginning of combat step begins, never at the natural combat phase or at another added one, and
 * the added phase is combat only — no main phase follows it.
 */
class AdditionalCombatBeginningTriggerTest : FunSpec({

    val RallyingCombat = card("Rallying Combat") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.AddCombatPhaseWithBeginningTrigger(
                Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.youControl()),
                    Effects.Untap(EffectTarget.IterationEntity)
                )
            )
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(RallyingCombat)
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castRallyingCombat() {
        val spell = putCardInHand(player1, "Rallying Combat")
        giveMana(player1, Color.RED, 1)
        castSpell(player1, spell).error shouldBe null
        bothPass()
        state.stack.shouldBeEmpty()
    }

    fun GameTestDriver.inAddedCombat(): Boolean =
        state.getEntity(player1)?.has<InAdditionalCombatPhaseComponent>() == true

    /** Pass to the next beginning of combat step, then resolve whatever triggered there. */
    fun GameTestDriver.enterNextCombatResolvingTriggers(): Int {
        passPriorityUntil(Step.BEGIN_COMBAT)
        val triggered = state.stack.size
        while (state.stack.isNotEmpty()) bothPass()
        return triggered
    }

    fun GameTestDriver.attackWith(creature: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(creature), player2).error shouldBe null
        passPriorityUntil(Step.END_COMBAT)
    }

    test("cast precombat: the added combat comes first and only it untaps at its beginning") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(courser)
        d.tapPermanent(courser)

        d.castRallyingCombat()
        withClue("The rider rides on the queued phase, not in the global delayed-trigger list") {
            d.state.delayedTriggers.shouldBeEmpty()
            val queued: Component = d.state.getEntity(d.player1)!!.get<AdditionalPhasesComponent>()!!
            val json = Json { serializersModule = engineSerializersModule }
            json.decodeFromString<Component>(json.encodeToString(queued)) shouldBe queued
        }

        withClue("The added combat comes straight after the precombat main phase and its rider triggers") {
            d.enterNextCombatResolvingTriggers() shouldBe 1
            d.inAddedCombat() shouldBe true
            d.isTapped(courser) shouldBe false
        }
        d.attackWith(courser)
        d.isTapped(courser) shouldBe true

        withClue("The natural combat follows, and nothing untaps at its beginning") {
            d.enterNextCombatResolvingTriggers() shouldBe 0
            d.inAddedCombat() shouldBe false
            d.isTapped(courser) shouldBe true
        }
        d.state.delayedTriggers.shouldBeEmpty()
    }

    test("two resolutions give two added combats that each untap at their own beginning") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(courser)
        d.castRallyingCombat()
        d.castRallyingCombat()

        d.enterNextCombatResolvingTriggers() shouldBe 1
        d.inAddedCombat() shouldBe true
        d.attackWith(courser)
        d.isTapped(courser) shouldBe true

        withClue("The second added combat's own rider untaps the attacker") {
            d.enterNextCombatResolvingTriggers() shouldBe 1
            d.inAddedCombat() shouldBe true
            d.isTapped(courser) shouldBe false
        }
        d.attackWith(courser)

        withClue("Then the natural combat, with no untap") {
            d.enterNextCombatResolvingTriggers() shouldBe 0
            d.inAddedCombat() shouldBe false
            d.isTapped(courser) shouldBe true
        }
        d.getLifeTotal(d.player2) shouldBe 14
    }

    test("cast postcombat: the added combat follows the main phase and no main phase follows it") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(courser)
        d.attackWith(courser)
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.isTapped(courser) shouldBe true

        d.castRallyingCombat()
        d.enterNextCombatResolvingTriggers() shouldBe 1
        d.inAddedCombat() shouldBe true
        d.isTapped(courser) shouldBe false
        d.attackWith(courser)

        d.bothPass()
        withClue("The turn moves on to its end step, not another main phase") {
            d.state.step shouldBe Step.END
        }
    }

    test("a skipped added combat never fires its rider") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(courser)
        d.tapPermanent(courser)
        d.castRallyingCombat()
        d.addComponent(d.player1, SkipCombatPhasesComponent)

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.isTapped(courser) shouldBe true
        d.state.delayedTriggers.shouldBeEmpty()
        d.state.getEntity(d.player1)?.has<AdditionalPhasesComponent>() shouldBe false
    }
})
