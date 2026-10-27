package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.ChoiceSlot
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
     * `GrantProtectionFromToGroup` rejects those at construction. A source-bound
     * [ProtectionScope.ChosenCardType] is also empty here — [resolveChosen] it against the granting
     * permanent first.
     */
    fun keywordsFor(scope: ProtectionScope): Set<String> = when (scope) {
        is ProtectionScope.Color -> setOf("PROTECTION_FROM_${scope.color.name}")
        is ProtectionScope.Colors -> scope.colors.mapTo(linkedSetOf()) { "PROTECTION_FROM_${it.name}" }
        ProtectionScope.Multicolored -> setOf(ColorProtection.PROTECTION_FROM_MULTICOLORED)
        is ProtectionScope.CardType -> setOf("PROTECTION_FROM_CARDTYPE_${scope.cardType.uppercase()}")
        is ProtectionScope.Subtype -> setOf(TypeQualityProtection.subtypeKeyword(scope.subtype, scope.cardType))
        is ProtectionScope.Supertype -> setOf(TypeQualityProtection.supertypeKeyword(scope.supertype, scope.cardType))
        ProtectionScope.EachOpponent -> setOf(Keyword.PROTECTION_FROM_EACH_OPPONENT.name)
        ProtectionScope.Spells,
        ProtectionScope.PermanentsCastThisTurn,
        ProtectionScope.RingBearers,
        ProtectionScope.ActivatedAbilities,
        ProtectionScope.TriggeredAbilities ->
            setOf(SourceKindProtection.protectionKeyword(checkNotNull(SourceKind.of(scope))))
        is ProtectionScope.NonColor,
        is ProtectionScope.ChosenCardType,
        ProtectionScope.Monocolored,
        ProtectionScope.Everything -> emptySet()
    }

    /**
     * Binds a source-bound scope to the choice its granting permanent [source] made as it entered:
     * [ProtectionScope.ChosenCardType] becomes the [ProtectionScope.CardType] stored under its slot,
     * or null while nothing is chosen (protection from nothing). Every other scope is returned as is.
     */
    fun resolveChosen(scope: ProtectionScope, source: ComponentContainer?): ProtectionScope? = when (scope) {
        is ProtectionScope.ChosenCardType -> chosenCardType(source, scope.slot)?.let { ProtectionScope.CardType(it) }
        else -> scope
    }

    /** The `PROTECTION_FROM_CARDTYPE_<TYPE>` keyword for the card type [source] chose under [slot], or null. */
    fun chosenCardTypeKeyword(source: ComponentContainer?, slot: ChoiceSlot): String? =
        chosenCardType(source, slot)?.let { "PROTECTION_FROM_CARDTYPE_${it.uppercase()}" }

    private fun chosenCardType(source: ComponentContainer?, slot: ChoiceSlot): String? =
        (source?.get<CastChoicesComponent>()?.chosen?.get(slot) as? ChoiceValue.TextChoice)?.text
}
