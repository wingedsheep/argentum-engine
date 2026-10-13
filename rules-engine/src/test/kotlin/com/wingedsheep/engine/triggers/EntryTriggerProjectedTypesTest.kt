package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A permanent entering the battlefield is matched against "whenever a [type] enters" triggers by
 * its projected card types (layer 4), not its printed type line: a noncreature artifact that an
 * animation static makes a creature enters *as* a creature.
 */
class EntryTriggerProjectedTypesTest : FunSpec({
    val animator = card("Entry Test Animator") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        staticAbility { ability = GrantCardType("CREATURE", GroupFilter(GameObjectFilter.Artifact.notCreature())) }
    }
    val creatureWatcher = card("Entry Test Creature Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
            effect = Effects.GainLife(1)
        }
    }
    val noncreatureWatcher = card("Entry Test Noncreature Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Artifact.notCreature().youControl()).enters()
            effect = Effects.GainLife(10)
        }
    }
    val trinket = card("Entry Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
        // Printed P/T (as on a Vehicle) so it survives being animated.
        power = 1
        toughness = 1
    }

    fun driver(animated: Boolean) = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(animator, creatureWatcher, noncreatureWatcher, trinket))
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        putPermanentOnBattlefield(player1, creatureWatcher.name)
        putPermanentOnBattlefield(player1, noncreatureWatcher.name)
        if (animated) putPermanentOnBattlefield(player1, animator.name)
        giveMana(player1, Color.WHITE, 1)
        castSpell(player1, putCardInHand(player1, trinket.name)).error shouldBe null
        bothPass()
        while (stackSize > 0) bothPass()
    }

    test("an artifact animated as it enters fires creature-entry triggers, not noncreature ones") {
        val d = driver(animated = true)
        d.state.projectedState.isCreature(d.findPermanent(d.player1, trinket.name)!!) shouldBe true
        d.getLifeTotal(d.player1) shouldBe 21
    }

    test("without the animation it enters as a noncreature artifact") {
        val d = driver(animated = false)
        d.getLifeTotal(d.player1) shouldBe 30
    }
})
