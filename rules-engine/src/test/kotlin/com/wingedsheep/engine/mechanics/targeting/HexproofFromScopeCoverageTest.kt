package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.GrantHexproofFromToGroup
import com.wingedsheep.sdk.scripting.ProtectionScope
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The SDK's [GrantHexproofFromToGroup.isSupported] and the engine's [HexproofFromRules.keywordsFor]
 * say the same thing twice — which scopes have a projected `HEXPROOF_FROM_*` keyword. If they drift,
 * a grant the SDK accepts projects nothing and the hexproof silently does nothing. Pin them together
 * for every [ProtectionScope] kind.
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
        ProtectionScope.ActivatedAbilities,
        ProtectionScope.TriggeredAbilities,
    )

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
})
