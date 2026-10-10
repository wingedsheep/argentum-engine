package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * The projected `PROTECTION_FROM_*` keywords that grant "protection from [ProtectionScope]" — the
 * same spellings a printed [com.wingedsheep.sdk.scripting.KeywordAbility.Protection] projects (see
 * `StateProjector`), so a granted protection is enforced by exactly the read sites that enforce a
 * printed one. The protection twin of [HexproofFromRules.keywordsFor].
 */
object ProtectionKeywords {

    /**
     * Empty for a scope with no creature-protection keyword (non-color, monocolored, everything);
     * `GrantProtectionFromToGroup` rejects those at construction.
     */
    fun keywordsFor(scope: ProtectionScope): Set<String> = when (scope) {
        is ProtectionScope.Color -> setOf("PROTECTION_FROM_${scope.color.name}")
        is ProtectionScope.Colors -> scope.colors.mapTo(linkedSetOf()) { "PROTECTION_FROM_${it.name}" }
        ProtectionScope.Multicolored -> setOf(ColorProtection.PROTECTION_FROM_MULTICOLORED)
        is ProtectionScope.CardType -> setOf("PROTECTION_FROM_CARDTYPE_${scope.cardType.uppercase()}")
        is ProtectionScope.Subtype -> setOf("PROTECTION_FROM_SUBTYPE_${scope.subtype.uppercase()}")
        is ProtectionScope.Supertype -> setOf("PROTECTION_FROM_SUPERTYPE_${scope.supertype.uppercase()}")
        ProtectionScope.EachOpponent -> setOf(Keyword.PROTECTION_FROM_EACH_OPPONENT.name)
        ProtectionScope.Spells,
        ProtectionScope.PermanentsCastThisTurn,
        ProtectionScope.RingBearers,
        ProtectionScope.ActivatedAbilities,
        ProtectionScope.TriggeredAbilities ->
            setOf(SourceKindProtection.protectionKeyword(checkNotNull(SourceKind.of(scope))))
        is ProtectionScope.NonColor,
        ProtectionScope.Monocolored,
        ProtectionScope.Everything -> emptySet()
    }
}
