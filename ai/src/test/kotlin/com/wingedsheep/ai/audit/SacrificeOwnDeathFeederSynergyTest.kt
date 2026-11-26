package com.wingedsheep.ai.audit

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * "Sacrifice another creature" as an effect the resolving player carries out on their own board
 * (Disciple of Bolas's enters trigger) is a creature of ours dying: it feeds [SynergyAxis.DEATH]
 * like a sacrifice cost does. Sacrificing a land is no death.
 */
class SacrificeOwnDeathFeederSynergyTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }

    test("an enters trigger that sacrifices another creature feeds death") {
        val disciple = byName["Disciple of Bolas"] ?: error("Disciple of Bolas is not in the corpus")
        SynergyAxes.of(disciple).feeds shouldContain SynergyAxis.DEATH
    }

    test("sacrificing one of our lands feeds no death") {
        val landSac = card("Own Land Sacrifice Probe") {
            manaCost = "{1}"
            typeLine = "Enchantment"
            triggeredAbility {
                trigger = Triggers.self.enters()
                effect = Effects.SacrificeOwn(GameObjectFilter.Land)
            }
        }
        SynergyAxes.of(landSac).feeds shouldNotContain SynergyAxis.DEATH
    }
})
