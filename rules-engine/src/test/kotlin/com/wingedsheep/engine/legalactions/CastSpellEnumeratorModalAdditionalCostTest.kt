package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests E3 from [`backlog/modal-cast-time-choices-plan.md`]: a mode whose
 * `additionalManaCost` the caster can't pay must be surfaced as unavailable
 * in the [ModalLegalEnumeration] payload (rule 700.2a — "you can't choose a
 * mode whose targets or costs you can't satisfy").
 *
 * Card built directly from [CardDefinition] + [CardScript.spell] so we can set
 * `Mode.additionalManaCost` (the DSL's `ModeBuilder` doesn't expose this field).
 *
 * Board: P1 has Forest + Plains (producing {G}/{W}). Base cost of the card
 * itself is free to cast, but mode 1 adds an unpayable {5}{U}. The enumerator
 * must flag mode 1 as unavailable while modes 0/2 remain available.
 */
class CastSpellEnumeratorModalAdditionalCostTest : FunSpec({

    // Choose-2 modal, allowRepeat=false. Mode indices:
    //   0 — "Draw a card"                       (always available)
    //   1 — "Gain 1 life" + additionalManaCost  ({5}{U}: unpayable with only {G}/{W})
    //   2 — "You lose 1 life"                   (always available)
    val CostlyModal = CardDefinition(
        name = "Test Costly Modal",
        manaCost = ManaCost.parse("{G}"),
        typeLine = TypeLine.sorcery(),
        oracleText = "Choose two —\n• Draw a card\n• {5}{U}: Gain 1 life\n• You lose 1 life",
        script = CardScript.spell(
            effect = ModalEffect(
                modes = listOf(
                    Mode.noTarget(
                        DrawCardsEffect(DynamicAmount.Fixed(1), EffectTarget.Controller),
                        "Draw a card"
                    ),
                    Mode(
                        effect = GainLifeEffect(DynamicAmount.Fixed(1), EffectTarget.Controller),
                        description = "Pay {5}{U}: Gain 1 life",
                        additionalManaCost = "{5}{U}"
                    ),
                    Mode.noTarget(
                        LoseLifeEffect(DynamicAmount.Fixed(1), EffectTarget.Controller),
                        "You lose 1 life"
                    )
                ),
                chooseCount = 2
            )
        )
    )

    test("E3 — mode with unpayable additionalManaCost is flagged unavailable; others stay available") {
        val driver = setupP1(
            hand = listOf("Test Costly Modal"),
            // Only {G} and {W} reachable — nowhere near {5}{U}.
            battlefield = listOf("Forest", "Plains"),
            extraSetCards = listOf(CostlyModal)
        )

        val casts = driver.enumerateFor(driver.player1).castActionsFor("Test Costly Modal")
        casts.size shouldBe 1

        val modal = casts.single()
        modal.actionType shouldBe "CastSpellModal"

        val enumeration = modal.modalEnumeration
        enumeration.shouldNotBeNull()
        enumeration.modes.size shouldBe 3

        // Mode 0 ("Draw a card") — always available.
        enumeration.modes[0].available shouldBe true
        enumeration.modes[0].additionalManaCost shouldBe null

        // Mode 1 ("Gain 1 life", requires {5}{U}) — flagged as unavailable because
        // the caster has no way to pay {5}{U} on top of the base cost.
        enumeration.modes[1].available shouldBe false
        enumeration.modes[1].additionalManaCost shouldBe "{5}{U}"

        // Mode 2 ("You lose 1 life") — always available.
        enumeration.modes[2].available shouldBe true
        enumeration.modes[2].additionalManaCost shouldBe null

        enumeration.unavailableIndices shouldContain 1
        enumeration.unavailableIndices shouldNotContain 0
        enumeration.unavailableIndices shouldNotContain 2
    }

    test("E3 — when enough Islands are in play, the {5}{U} mode becomes available") {
        val driver = setupP1(
            hand = listOf("Test Costly Modal"),
            // 5 Islands + any mana to cover the base {G} cost. That's enough to cover {5}{U}
            // on top of the base cost (total payable = {5}{G}{U}).
            battlefield = listOf("Forest", "Island", "Island", "Island", "Island", "Island", "Island"),
            extraSetCards = listOf(CostlyModal)
        )

        val modal = driver.enumerateFor(driver.player1)
            .castActionsFor("Test Costly Modal").single()
        val enumeration = modal.modalEnumeration.shouldNotBeNull()
        enumeration.modes[1].available shouldBe true
        enumeration.unavailableIndices shouldBe emptyList()
    }
})
