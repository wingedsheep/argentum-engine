package com.wingedsheep.ai.audit

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * Any effect that exiles cards an opponent owns stocks what a processor spends
 * ([SynergyAxis.OPPONENT_EXILE]), whoever does the exiling: "that player exiles the top two cards of
 * their library" off an opponent's land (Sire of Stagnation), "target opponent exiles the top four"
 * (Oblivion Sower), "defending player exiles two permanents they control" (Bane of Bala Ged). Our
 * own library exiled stocks nothing of theirs, and a processor spends the axis rather than feeding it.
 */
class OpponentExileFeederSynergyTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun named(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("exiling an opponent's library top or their permanents feeds opponent exile") {
        for (name in listOf("Sire of Stagnation", "Oblivion Sower", "Bane of Bala Ged")) {
            SynergyAxes.of(named(name)).feeds shouldContain SynergyAxis.OPPONENT_EXILE
        }
    }

    test("exiling our own library top feeds nothing of theirs") {
        val ownTop = card("Own Library Exile Probe") {
            manaCost = "{1}"
            typeLine = "Sorcery"
            spell { effect = Patterns.Library.exileTop(2) }
        }
        SynergyAxes.of(ownTop).feeds shouldNotContain SynergyAxis.OPPONENT_EXILE
    }

    test("a processor still rewards opponent exile without feeding it") {
        val nullifier = SynergyAxes.of(named("Ulamog's Nullifier"))
        nullifier.rewards shouldContain SynergyAxis.OPPONENT_EXILE
        nullifier.feeds shouldNotContain SynergyAxis.OPPONENT_EXILE
    }
})
