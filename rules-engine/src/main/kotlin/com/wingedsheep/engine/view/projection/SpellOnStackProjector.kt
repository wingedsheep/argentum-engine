package com.wingedsheep.engine.view.projection

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.view.CastProvenance
import com.wingedsheep.engine.view.ClientPerModeTargetGroup
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.GiftGivenEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect

/**
 * What a [ClientCard][com.wingedsheep.engine.view.ClientCard] shows about the *spell* it is while it
 * sits on the stack: how it was cast (optional cost, provenance, alternative-cost payment), what was
 * chosen for it (modes and their targets, X, gift), and its runtime text. Every field is empty for
 * a card that is not a spell on the stack.
 */
internal data class SpellOnStackView(
    val chosenModeDescriptions: List<String> = emptyList(),
    val perModeTargets: List<ClientPerModeTargetGroup> = emptyList(),
    val optionalCostLabel: String? = null,
    val castProvenanceLabel: String? = null,
    val costSacrificeLabel: String? = null,
    val manaPaidCost: String? = null,
    val wasBlightPaid: Boolean = false,
    val giftPromised: Boolean = false,
    val chosenX: Int? = null,
    val sacrificedCreatureTypes: Set<String>? = null,
    val stackText: String? = null,
)

/** Builds the [SpellOnStackView] for a card carrying a [SpellOnStackComponent]. */
internal class SpellOnStackProjector(
    private val stackText: StackTextRenderer,
) {

    fun project(
        state: GameState,
        entityId: EntityId,
        zoneKey: ZoneKey,
        spellOnStack: SpellOnStackComponent?,
        cardDef: CardDefinition?,
        cardComponent: CardComponent,
        viewingPlayerId: EntityId,
        isSpectator: Boolean
    ): SpellOnStackView {
        if (spellOnStack == null) return SpellOnStackView()
        val onStack = zoneKey.zoneType == Zone.STACK

        // Per-mode breakdown for modal spells whose modes/targets were chosen at cast time (700.2).
        // Opponents see the same data so they can respond with counterspells knowing what's coming.
        val modalEffectForStack = (cardDef?.script?.spellEffect as? ModalEffect)
        val chosenModeDescriptions: List<String> = if (
            onStack &&
            modalEffectForStack != null &&
            spellOnStack.chosenModes.isNotEmpty()
        ) {
            stackText.chosenModeDescriptions(state, entityId, spellOnStack, modalEffectForStack)
        } else emptyList()
        val perModeTargets: List<ClientPerModeTargetGroup> = if (
            onStack &&
            spellOnStack.chosenModes.isNotEmpty()
        ) {
            stackText.perModeTargetGroups(
                state,
                spellOnStack.chosenModes,
                spellOnStack.modeTargetsOrdered,
                chosenModeDescriptions,
                viewingPlayerId,
                isSpectator
            )
        } else emptyList()

        // An alternative cost replaces the printed cost, so the printed pips explain nothing about
        // what this cast actually took: name the body it ate and the mana that left the pool. Scoped
        // to alternative-cost casts on purpose — for a normal cast both are already inferable from
        // the card, and every spell would grow two badges for nothing. Emerge (CR 702.119a) is the
        // case that needs it: the sacrifice is *why* the cost shrank.
        val alternativeCostSpell = spellOnStack.takeIf { it.alternativeCost != null }

        return SpellOnStackView(
            chosenModeDescriptions = chosenModeDescriptions,
            perModeTargets = perModeTargets,
            optionalCostLabel = optionalCostLabel(spellOnStack, cardDef),
            // Name how the spell got onto the stack ("Disturb · Graveyard") so a cast from anywhere
            // but hand doesn't read as a cast out of hand. Same server-side-naming rule as the
            // optional-cost label.
            castProvenanceLabel = CastProvenance.badgeLabel(spellOnStack.alternativeCost, spellOnStack.castFromZone),
            costSacrificeLabel = alternativeCostSpell?.let {
                CastProvenance.sacrificeLabel(it.sacrificedPermanents.mapNotNull { snapshot -> snapshot.name })
            },
            manaPaidCost = alternativeCostSpell?.let {
                CastProvenance.paidManaCost(
                    white = it.manaSpentWhite,
                    blue = it.manaSpentBlue,
                    black = it.manaSpentBlack,
                    red = it.manaSpentRed,
                    green = it.manaSpentGreen,
                    colorless = it.manaSpentColorless,
                )
            },
            // Surface whether the optional Blight additional cost was paid (Lorwyn Eclipsed)
            // so opponents can see at a glance that a stronger effect is incoming on resolution.
            wasBlightPaid = spellOnStack.wasBlightPaid,
            giftPromised = giftPromised(spellOnStack, cardDef),
            // Get chosen X value for spells on the stack
            chosenX = spellOnStack.xValue,
            // Get sacrificed creature types for spells with sacrifice-as-cost (e.g., Endemic Plague)
            sacrificedCreatureTypes = spellOnStack.sacrificedPermanents
                .flatMap { it.subtypes }.toSet()
                .takeIf { it.isNotEmpty() },
            stackText = if (onStack && cardDef != null) {
                stackText(state, entityId, spellOnStack, cardDef, cardComponent)
            } else null,
        )
    }

    /**
     * Name the optional additional cost a spell on the stack declared ("Kicked", "Bargained",
     * "Offspring"), so opponents can see at a glance which branch is coming on resolution. The
     * label is derived server-side from the keyword's printed prefix — the client renders the
     * badge verbatim rather than mapping slots to words itself.
     */
    private fun optionalCostLabel(spellOnStack: SpellOnStackComponent, cardDef: CardDefinition?): String? =
        spellOnStack.declaredCostSlot?.let { slot ->
            val declared = cardDef?.keywordAbilities
                ?.filterIsInstance<KeywordAbility.OptionalAdditionalCost>()
                ?.firstOrNull { it.declaredSlot == slot }
            when {
                declared?.keyword == Keyword.OFFSPRING -> "Offspring"
                slot == ChoiceSlot.BARGAINED -> "Bargained"
                slot == ChoiceSlot.KICKED -> {
                    // A two-kicker card names the kicker(s) paid ("Kicked {G}"), CR 702.33f.
                    val kickers = cardDef?.keywordAbilities
                        ?.filterIsInstance<KeywordAbility.OptionalAdditionalCost>()
                        ?.filter { it.declaredSlot == ChoiceSlot.KICKED }.orEmpty()
                    val paid = listOfNotNull(
                        kickers.getOrNull(0)?.takeIf { ChoiceSlot.FIRST_KICKER in spellOnStack.additionalCostChoices },
                        kickers.getOrNull(1)?.takeIf { ChoiceSlot.SECOND_KICKER in spellOnStack.additionalCostChoices },
                    )
                    if (paid.isEmpty()) "Kicked"
                    else com.wingedsheep.engine.handlers.actions.spell.chosenKickersLabel(paid)
                }
                // Teamwork prints its N, so the badge is the keyword's own prefix ("Teamwork 2")
                // rather than the bare slot name (CR 702.194b — "cast using teamwork").
                slot == ChoiceSlot.TEAMWORK ->
                    declared?.displayPrefix ?: "Teamwork"
                else -> slot.name.lowercase().replaceFirstChar { it.uppercase() }
            }
        }

    /**
     * Detect whether this spell promised a gift (Bloomburrow gift mechanic).
     * Permanent spells carry the promise as the gift additional cost elected while casting
     * (CR 702.174a — `giftRecipient`); instants and sorceries model it as a modal choice whose
     * "promise" mode's effect tree contains GiftGivenEffect. Surface either to opponents so
     * they can see at a glance that a gift is coming on resolution, rather than having to parse
     * the mode description.
     */
    private fun giftPromised(spellOnStack: SpellOnStackComponent, cardDef: CardDefinition?): Boolean {
        if (spellOnStack.giftRecipient != null) return true
        if (spellOnStack.chosenModes.isEmpty()) return false
        val modal = cardDef?.script?.spellEffect as? ModalEffect ?: return false
        return spellOnStack.chosenModes.any { idx ->
            val mode = modal.modes.getOrNull(idx) ?: return@any false
            effectTreeContainsGift(mode.effect)
        }
    }

    private fun effectTreeContainsGift(effect: Effect): Boolean = when (effect) {
        is GiftGivenEffect -> true
        is CompositeEffect -> effect.effects.any { effectTreeContainsGift(it) }
        is GatedEffect -> effectTreeContainsGift(effect.then) ||
            (effect.otherwise?.let { effectTreeContainsGift(it) } ?: false)
        is ModalEffect -> effect.modes.any { effectTreeContainsGift(it.effect) }
        else -> false
    }

    /** The text shown for the spell on the stack, or null when the card itself says it all. */
    private fun stackText(
        state: GameState,
        entityId: EntityId,
        spellOnStack: SpellOnStackComponent,
        cardDef: CardDefinition,
        cardComponent: CardComponent
    ): String? = when {
        spellOnStack.castFaceDown -> "Cast as a face-down 2/2 creature"
        // Instants/sorceries always show their spell effect description
        cardDef.typeLine.cardTypes.any { it == CardType.INSTANT || it == CardType.SORCERY } ->
            stackText.runtimeStackText(state, entityId, spellOnStack, cardDef)
        // Permanents only show text when ambiguous (card has alternate cast modes like cycling or morph)
        cardDef.keywordAbilities.any { it is KeywordAbility.Cycling || it is KeywordAbility.Morph } ->
            stackText.runtimeStackText(state, entityId, spellOnStack, cardDef) ?: cardComponent.oracleText
        // Unambiguous permanent cast — no text needed
        else -> null
    }
}
