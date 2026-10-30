package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.GrantHexproofFromToGroup
import com.wingedsheep.sdk.scripting.GrantProtectionFromToGroup
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The SDK's [GrantHexproofFromToGroup.isSupported] and the engine's [HexproofFromRules.keywordsFor]
 * say the same thing twice — which scopes have a projected `HEXPROOF_FROM_*` keyword. If they drift,
 * a grant the SDK accepts projects nothing and the hexproof silently does nothing. Pin them together
 * for every [ProtectionScope] kind. For the protection twin, every scope
 * [GrantProtectionFromToGroup.isSupported] accepts must have a [ProtectionKeywords.keywordsFor] keyword.
 */
class HexproofFromScopeCoverageTest : FunSpec({

    val samples: List<ProtectionScope> = listOf(
        ProtectionScope.Color(Color.WHITE),
        ProtectionScope.Colors(setOf(Color.BLUE, Color.BLACK)),
        ProtectionScope.NonColor(Color.GREEN),
        ProtectionScope.Multicolored,
        ProtectionScope.Monocolored,
        ProtectionScope.CardType("INSTANT"),
        ProtectionScope.Subtype("Goblin"),
        ProtectionScope.Supertype("LEGENDARY"),
        ProtectionScope.Everything,
        ProtectionScope.EachOpponent,
        ProtectionScope.Spells,
        ProtectionScope.PermanentsCastThisTurn,
        ProtectionScope.RingBearers,
        ProtectionScope.ActivatedAbilities,
        ProtectionScope.TriggeredAbilities,
        ProtectionScope.ChosenCardType(),
        ProtectionScope.ChosenCardName(),
        ProtectionScope.ManaValueParity(com.wingedsheep.sdk.scripting.Parity.ODD),
        ProtectionScope.ManaValueOtherThan(3),
    )

    // A granting permanent that chose "Instant" as it entered — what a source-bound scope reads.
    val chooser = ComponentContainer().withCastChoice(ChoiceSlot.CARD_TYPE, ChoiceValue.TextChoice("Instant"))

    test("the samples cover every ProtectionScope kind") {
        samples.map { it::class }.toSet() shouldBe ProtectionScope::class.sealedSubclasses.toSet()
    }

    test("a scope the SDK accepts for a hexproof grant is exactly one the engine projects") {
        samples.forEach { scope ->
            withClue(scope) {
                GrantHexproofFromToGroup.isSupported(scope) shouldBe HexproofFromRules.keywordsFor(scope).isNotEmpty()
            }
        }
    }

    test("a scope the SDK accepts for a protection grant is one the engine projects") {
        samples.filter { GrantProtectionFromToGroup.isSupported(it) }.forEach { scope ->
            withClue(scope) {
                val bound = ProtectionKeywords.resolveChosen(scope, chooser)
                (bound != null && ProtectionKeywords.keywordsFor(bound).isNotEmpty()) shouldBe true
            }
        }
    }

    test("a single color or card type keeps its own protection-grant spelling") {
        GrantProtectionFromToGroup.isSupported(ProtectionScope.Color(Color.WHITE)) shouldBe false
        GrantProtectionFromToGroup.isSupported(ProtectionScope.CardType("INSTANT")) shouldBe false
    }
})
