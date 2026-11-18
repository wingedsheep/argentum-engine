package com.wingedsheep.ai.audit

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * "Sliver creatures you control have 'When this creature enters, you may exile target creature an
 * opponent controls until this creature leaves the battlefield'" (Constricting Sliver): a lord's
 * helpful trigger handed to each of our own creatures is the same ability printed on each, read as
 * an Aura's granted trigger is — here exile removal. The same grant to every Sliver on the
 * battlefield ("all Slivers have …") reaches the opponent's too, and stays unread.
 */
class OwnGroupGrantedTriggerReadingTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun named(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    fun sliverLord(name: String, filter: GameObjectFilter) = card(name) {
        manaCost = "{5}{W}"
        typeLine = "Creature — Sliver"
        power = 3
        toughness = 3
        staticAbility {
            ability = GrantTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.self.enters()
                    val creature = target(TargetFilter.CreatureOpponentControls)
                    effect = Effects.May(Effects.MoveUntilSourceLeaves(creature, Zone.EXILE))
                },
                filter = GroupFilter(filter),
            )
        }
    }

    test("a helpful trigger granted to the creatures we control reads as if printed") {
        val tags = CardIntentAnalyzer.analyze(named("Constricting Sliver")).tags
        tags shouldContain IntentTag.REMOVAL
        tags shouldContain IntentTag.EXILE_REMOVAL
        CardIntentAnalyzer.analyze(named("Ingenious Infiltrator")).tags shouldContain IntentTag.DRAW
    }

    test("the same grant to every Sliver on the battlefield is symmetric and reads nothing") {
        val ours = sliverLord("Own Sliver Probe", GameObjectFilter.Creature.withSubtype(Subtype.SLIVER).youControl())
        val everyone = sliverLord("All Slivers Probe", GameObjectFilter.Creature.withSubtype(Subtype.SLIVER))
        CardIntentAnalyzer.analyze(ours).tags shouldContain IntentTag.REMOVAL
        CardIntentAnalyzer.analyze(everyone).tags shouldNotContain IntentTag.REMOVAL
    }
})
