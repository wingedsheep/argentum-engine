package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Grants a triggered ability to a filtered set of permanents.
 *
 * Use [GroupFilter.attachedCreature] for "enchanted/equipped creature has ..." auras and
 * equipment, [GroupFilter.source] for "this creature has ..." abilities, or any
 * battlefield-scoped filter for lord/sliver-style "all X creatures have ..." effects.
 *
 * Both `TriggerDetector` (for battlefield scope) and `TriggerAbilityResolver` (for
 * Self/AttachedTo scope) consult this static ability when computing triggered
 * abilities to fire.
 *
 * @property ability The triggered ability to grant.
 * @property filter The permanents that gain the ability.
 */
@SerialName("GrantTriggeredAbility")
@Serializable
data class GrantTriggeredAbility(
    val ability: TriggeredAbility,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "${filter.description} have ${ability.trigger}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAbility !== ability) copy(filter = newFilter, ability = newAbility) else this
    }
}

/**
 * Grants an activated ability to a filtered set of permanents.
 *
 * Use [GroupFilter.attachedCreature] for "enchanted/equipped creature has ..." auras
 * and equipment, [GroupFilter.source] for "this creature has ..." abilities, or any
 * battlefield-scoped filter for lord/sliver-style "all X creatures have ..." effects.
 *
 * `LegalActionsCalculator` and `ActivateAbilityHandler` consult this static ability
 * when computing legal activated abilities for each permanent.
 *
 * @property ability The activated ability to grant.
 * @property filter The permanents that gain the ability.
 */
@SerialName("GrantActivatedAbility")
@Serializable
data class GrantActivatedAbility(
    val ability: ActivatedAbility,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "${filter.description} have ${ability.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAbility !== ability) copy(filter = newFilter, ability = newAbility) else this
    }
}

/**
 * Which pool of cards a [HasAllActivatedAbilitiesOfCards] static reads to find the cards whose
 * activated abilities it donates.
 */
@Serializable
enum class DonorCards {
    /** Cards exiled *with* the source — its `LinkedExileComponent` (Territory Forge, Agatha's Soul Cauldron). */
    @SerialName("LinkedExile") LINKED_EXILE,

    /** Cards exiled *to craft* the source — its `CraftedFromExiledComponent` (Locus of Enlightenment, CR 702.167c). */
    @SerialName("CraftMaterials") CRAFT_MATERIALS,

    /**
     * Cards in the graveyard of the source's *controller* (Thranduil, the Elvenking). Unlike the two
     * exile pools — which are anchored to the source permanent by a component — this pool is anchored
     * to the player, so it re-reads live as cards enter and leave that graveyard.
     */
    @SerialName("YourGraveyard") YOUR_GRAVEYARD,

    /**
     * Cards in *every* player's graveyard (Mirran Safehouse — "all land cards in all graveyards").
     * Anchored to no one: it re-reads each graveyard live, whoever controls the source.
     */
    @SerialName("AllGraveyards") ALL_GRAVEYARDS,
}

/**
 * Grants the permanents matching [receivedBy] **all activated abilities of the cards in the
 * [donors] pool that match [cardFilter]** — the single primitive behind "exiled with this"
 * (linked-exile) grants, "exiled to craft this" (craft-material) grants, and "cards in your
 * graveyard" grants. Shapes:
 *  - `receivedBy = GroupFilter.source()` (the default) → "This permanent has all activated abilities
 *    of the donor cards" (Territory Forge / Locus of Enlightenment / Thranduil, the Elvenking — the
 *    source grants to *itself*).
 *  - any battlefield filter → "Creatures you control with +1/+1 counters on them have all activated
 *    abilities of all creature cards exiled with this" (Agatha's Soul Cauldron — grants to *other*
 *    matching permanents).
 *
 * [cardFilter] narrows the donor pool by the *card's own* characteristics — `Filters.Creature` for
 * Agatha's "all **creature** cards exiled with", `Filters.WithSubtype("Elf")` for Thranduil's "all
 * **Elf** cards in your graveyard". It matches against base card data (donor cards are never on the
 * battlefield, so there is no projection entry to consult).
 *
 * Resolution is dynamic: the engine reads the donor pool at activation-legality time, pulls every
 * activated ability off each donor card's definition, and surfaces them as activatable on each
 * *matching* permanent — with that permanent as the ability's source, so self-references and `{T}`
 * resolve against the permanent that gained the ability (CR 113.7 — a granted ability's source is the
 * object that has it; faithful to the rulings that the donor card's "this card" references become
 * references to the permanent that has the ability).
 *
 * It grants only *activated* abilities — not triggered, static, or replacement abilities.
 *
 * @property donors Which pool of cards donates its abilities — see [DonorCards].
 * @property cardFilter Restricts the donor pool to cards matching it (default: every card in the pool).
 * @property receivedBy The permanents that gain the donor cards' abilities (default: the source itself).
 * @property oncePerTurnEach When true (Locus of Enlightenment's "only once each turn"), each granted
 *   ability additionally carries a once-each-turn cap tracked *per donor card* — two copies of one
 *   card each get their own budget, not a shared one. The engine implements this by re-stamping each
 *   granted ability with a donor-derived [AbilityId] (`donor_<entity>_<printedId>`), which also stops
 *   duplicate donors collapsing under the granter-dedup. When false (Territory Forge, Agatha,
 *   Thranduil), abilities are granted unmodified and duplicates dedup as before.
 */
@SerialName("HasAllActivatedAbilitiesOfCards")
@Serializable
data class HasAllActivatedAbilitiesOfCards(
    val donors: DonorCards,
    val cardFilter: GameObjectFilter = GameObjectFilter.Any,
    val receivedBy: GroupFilter = GroupFilter.source(),
    val oncePerTurnEach: Boolean = false,
) : StaticAbility {
    override val description: String = buildString {
        append(receivedBy.description)
        append(" have all activated abilities of the")
        if (cardFilter != GameObjectFilter.Any) append(" ${cardFilter.description}")
        append(
            when (donors) {
                DonorCards.LINKED_EXILE -> " cards exiled with this"
                DonorCards.CRAFT_MATERIALS -> " cards exiled to craft this"
                DonorCards.YOUR_GRAVEYARD -> " cards in your graveyard"
                DonorCards.ALL_GRAVEYARDS -> " cards in all graveyards"
            }
        )
        if (oncePerTurnEach) append(" (each only once each turn)")
    }

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newReceivedBy = receivedBy.applyTextReplacement(replacer)
        val newCardFilter = cardFilter.applyTextReplacement(replacer)
        return if (newReceivedBy !== receivedBy || newCardFilter !== cardFilter)
            copy(receivedBy = newReceivedBy, cardFilter = newCardFilter) else this
    }
}

/**
 * Grants the source permanent the **activated and/or triggered abilities of the single card it most
 * recently *chose*** from its linked-exile pile — the "last chosen card" of a
 * choose-from-your-exile mechanic (Koh, the Face Stealer: "Pay 1 life: Choose a creature card exiled
 * with Koh. Koh has all activated and triggered abilities of the last chosen card").
 *
 * Unlike [HasAllActivatedAbilitiesOfCards] — which surfaces the abilities of *every* card
 * in the pile — this reads the source's `ChosenLinkedExileComponent` (stamped by
 * [com.wingedsheep.sdk.scripting.effects.RecordChosenLinkedExileEffect]) and contributes only the
 * abilities of that one chosen card. It is always self-scoped ("this permanent has …"); use the two
 * flags to grant activated abilities, triggered abilities, or both.
 *
 * Resolution is dynamic and re-reads the chosen card on every query, so re-choosing a different
 * exiled card live-swaps which abilities the source has. The granted abilities use the source as
 * their own source, so `{T}`/self-references bind to the granting permanent (CR-faithful to the
 * "gains abilities of another object" rulings). It grants only *activated* and *triggered*
 * abilities — never static, keyword, or replacement abilities.
 *
 * @property grantActivated When true, the chosen card's activated abilities are surfaced on the source.
 * @property grantTriggered When true, the chosen card's triggered abilities fire from the source.
 */
@SerialName("HasAbilitiesOfChosenLinkedExiledCard")
@Serializable
data class HasAbilitiesOfChosenLinkedExiledCard(
    val grantActivated: Boolean = true,
    val grantTriggered: Boolean = true,
) : StaticAbility {
    override val description: String = buildString {
        append("This permanent has all ")
        append(
            when {
                grantActivated && grantTriggered -> "activated and triggered"
                grantActivated -> "activated"
                else -> "triggered"
            }
        )
        append(" abilities of the last chosen card exiled with it")
    }

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}
