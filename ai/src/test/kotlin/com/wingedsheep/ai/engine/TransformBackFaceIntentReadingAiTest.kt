package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A transform card's back face is read as its own card once it has flipped (the registry holds it
 * by name), so its statics are placed by the back face's intent, not the front's. Kruin Outlaw's
 * front is a first striker with a transform trigger — nothing to tag — while its back, Terror of
 * Kruin Pass, grants its Werewolves menace: evasion the AI reads, so the card is not "abilities
 * not recognised". Geier Reach Bandit's back, Vildin-Pack Alpha, transforms a Werewolf you control
 * as it enters: our own permanent flipping, the self-transform spelled through the trigger.
 */
class TransformBackFaceIntentReadingAiTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }

    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("a back face's lord static is placed by the back face's own reading") {
        val kruin = card("Kruin Outlaw")
        val back = kruin.backFace ?: error("Kruin Outlaw has no back face")
        withClue("Terror of Kruin Pass's Werewolf menace grant reads as evasion") {
            CardIntentAnalyzer.analyze(back).tags shouldContain IntentTag.EVASION_GRANT
        }
        withClue("Kruin Outlaw is not flagged 'abilities not recognised'") {
            AiCardRating.of(kruin, listOf("ISD")).intent.unrecognised shouldBe false
        }
    }

    test("transforming an entering permanent you control needs no intent") {
        val bandit = card("Geier Reach Bandit")
        withClue("Vildin-Pack Alpha's 'you may transform it' reads fully") {
            AiReadingAudit.read(bandit).abilities.filter { it.checked }.all { it.readFully } shouldBe true
        }
        withClue("Geier Reach Bandit is not flagged 'abilities not recognised'") {
            AiCardRating.of(bandit, listOf("SOI")).intent.unrecognised shouldBe false
        }
    }
})
