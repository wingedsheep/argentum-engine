package com.wingedsheep.ai.audit

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Defending player exiles two permanents they control" (Bane of Bala Ged) — a pipeline gathering
 * an opponent's battlefield as a zone and exiling the ones they pick — is the exile edict: removal
 * that leaves nothing to recur, read off the shape rather than the card. The same pipeline over our
 * own permanents is a price, never an answer.
 */
class OpponentChosenExileReadingTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun named(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("an opponent exiling permanents of their choosing reads as exile removal") {
        val bane = named("Bane of Bala Ged")
        AiReadingAudit.read(bane).readFully shouldBe true
        val tags = CardIntentAnalyzer.analyze(bane).tags
        tags shouldContain IntentTag.REMOVAL
        tags shouldContain IntentTag.EXILE_REMOVAL
    }

    test("exiling permanents we control is no removal") {
        val ownExile = card("Own Exile Probe") {
            manaCost = "{2}"
            typeLine = "Sorcery"
            spell {
                effect = Effects.Pipeline {
                    val permanents = gather(CardSource.FromZone(Zone.BATTLEFIELD, Player.You, GameObjectFilter.Permanent))
                    val chosen = chooseExactly(2, from = permanents, prompt = "Choose two permanents to exile")
                    exile(chosen)
                }
            }
        }
        val tags = CardIntentAnalyzer.analyze(ownExile).tags
        tags shouldNotContain IntentTag.REMOVAL
        tags shouldNotContain IntentTag.EXILE_REMOVAL
    }
})
