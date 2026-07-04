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
 * Grants the permanents matching [filter] **all activated abilities of the card(s) in this
 * source's linked-exile pile**. Two shapes:
 *  - `filter = GroupFilter.source()` (the default) → "This permanent has all activated abilities of
 *    the exiled card" (Territory Forge — the source grants the abilities to *itself*).
 *  - any battlefield filter → "Creatures you control with +1/+1 counters on them have all activated
 *    abilities of all creature cards exiled with this" (Agatha's Soul Cauldron — the source grants
 *    the abilities to *other* matching permanents). Pair with [creatureCardsOnly] when the printed
 *    text restricts the source pile to creature cards.
 *
 * Resolution is dynamic: the engine reads the source's linked-exile pile at activation-legality
 * time, pulls every activated ability off each exiled card's definition, and surfaces them as
 * activatable on each *matching* permanent — with that permanent as the ability's source, so
 * self-references and `{T}` resolve against the creature that gained the ability (CR-faithful to
 * the rulings that the exiled card's "this card" references become references to the permanent
 * that has the ability).
 *
 * It grants only *activated* abilities — not triggered, static, or replacement abilities.
 *
 * @property filter The permanents that gain the exiled cards' abilities (default: the source itself).
 * @property creatureCardsOnly When true, only *creature* cards in the linked-exile pile contribute
 *   their abilities (Agatha's "all **creature** cards exiled with"). When false, every exiled card
 *   contributes (Territory Forge).
 */
@SerialName("HasAllActivatedAbilitiesOfLinkedExiledCard")
@Serializable
data class HasAllActivatedAbilitiesOfLinkedExiledCard(
    val filter: GroupFilter = GroupFilter.source(),
    val creatureCardsOnly: Boolean = false,
) : StaticAbility {
    override val description: String =
        "${filter.description} have all activated abilities of the${if (creatureCardsOnly) " creature" else ""} cards exiled with this"

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants the source permanent the **activated and/or triggered abilities of the single card it most
 * recently *chose*** from its linked-exile pile — the "last chosen card" of a
 * choose-from-your-exile mechanic (Koh, the Face Stealer: "Pay 1 life: Choose a creature card exiled
 * with Koh. Koh has all activated and triggered abilities of the last chosen card").
 *
 * Unlike [HasAllActivatedAbilitiesOfLinkedExiledCard] — which surfaces the abilities of *every* card
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
