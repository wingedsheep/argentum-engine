package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.CardType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a spending restriction on mana produced by an ability.
 * When mana has a restriction, it can only be spent to cast spells that satisfy the restriction.
 * Restricted mana is spent preferentially when the spell is eligible.
 */
@Serializable
sealed interface ManaRestriction {
    val description: String

    /**
     * No restriction — this mana satisfies any spend context. Used as a marker for
     * unrestricted mana that still carries [ManaSpellRider]s (e.g., Path of Ancestry,
     * whose mana is spendable anywhere but triggers a side-effect when consumed on a
     * matching spell). Plain unrestricted mana with no riders goes into the colored
     * counters on [com.wingedsheep.engine.mechanics.mana.ManaPool] directly — only
     * use this when a rider is attached.
     */
    @SerialName("AnySpend")
    @Serializable
    data object AnySpend : ManaRestriction {
        override val description: String = ""
    }

    /**
     * "Spend this mana only to cast instant or sorcery spells."
     */
    @SerialName("InstantOrSorceryOnly")
    @Serializable
    data object InstantOrSorceryOnly : ManaRestriction {
        override val description: String = "Spend this mana only to cast instant or sorcery spells"
    }

    /**
     * "Spend this mana only to cast kicked spells."
     */
    @SerialName("KickedSpellsOnly")
    @Serializable
    data object KickedSpellsOnly : ManaRestriction {
        override val description: String = "Spend this mana only to cast kicked spells"
    }

    /**
     * "Spend this mana only to cast spells with mana value [minManaValue] or greater
     * [or spells with {X} in their mana costs]."
     *
     * Parameterized over the threshold and the two optional clauses printed alongside it, because
     * the mana-value gate shows up at several thresholds and with different qualifiers:
     *  - `SpellsWithManaValueAtLeast(4)` — Ashling, Rimebound
     *  - `SpellsWithManaValueAtLeast(4, orXInCost = true, creatureOnly = true)` — Helga, Skittish Seer
     *  - `SpellsWithManaValueAtLeast(5, orXInCost = true)` — Troyan, Gutsy Explorer
     *
     * [orXInCost] adds the "or spells with {X} in their mana costs" clause — a spell with {X} in its
     * cost qualifies regardless of its mana value, and the mana may pay any part of the cost, not
     * just the {X} part. [creatureOnly] narrows both clauses to creature spells. Ability activations
     * never satisfy this restriction.
     */
    @SerialName("SpellsWithManaValueAtLeast")
    @Serializable
    data class SpellsWithManaValueAtLeast(
        val minManaValue: Int,
        val orXInCost: Boolean = false,
        val creatureOnly: Boolean = false,
    ) : ManaRestriction {
        override val description: String = buildString {
            val noun = if (creatureOnly) "creature spells" else "spells"
            append("Spend this mana only to cast $noun with mana value $minManaValue or greater")
            if (orXInCost) append(" or $noun with {X} in their mana costs")
        }
    }

    /**
     * "Spend this mana only to cast creature spells."
     */
    @SerialName("CreatureSpellsOnly")
    @Serializable
    data object CreatureSpellsOnly : ManaRestriction {
        override val description: String = "Spend this mana only to cast creature spells"
    }

    /**
     * "Spend this mana only to cast legendary spells." Matches spells with the Legendary supertype
     * (Great Hall of the Citadel, Delighted Halfling).
     */
    @SerialName("LegendarySpellsOnly")
    @Serializable
    data object LegendarySpellsOnly : ManaRestriction {
        override val description: String = "Spend this mana only to cast legendary spells"
    }

    /**
     * "Spend this mana only to cast a spell of the specified subtype
     * (or, when [creatureOnly] is false, also to activate an ability of a source of that subtype)."
     *
     * The [subtype] is baked at the moment the mana is added to the pool
     * (e.g., read from the source's CastChoicesComponent), so the
     * restriction becomes self-contained and serializable.
     *
     * When [creatureOnly] is true, only creature spells of that subtype satisfy the
     * restriction — abilities of objects of the subtype don't (Cavern of Souls shape).
     * When false, the restriction also allows activated abilities of sources of the
     * subtype (Unclaimed Territory shape).
     *
     * Any rider side-effect of spending this mana (e.g. "the spell can't be countered")
     * is carried on the [com.wingedsheep.sdk.scripting.effects.ManaSpellRider] set
     * attached to the produced mana entry, not on this type.
     */
    @SerialName("SubtypeSpellsOrAbilitiesOnly")
    @Serializable
    data class SubtypeSpellsOrAbilitiesOnly(
        val subtype: String,
        val creatureOnly: Boolean = false
    ) : ManaRestriction {
        override val description: String = if (creatureOnly) {
            "Spend this mana only to cast a creature spell of the chosen type"
        } else {
            "Spend this mana only to cast a spell of the chosen type or activate an ability of a source of the chosen type"
        }
    }

    @SerialName("CastFromExileOnly")
    @Serializable
    data object CastFromExileOnly : ManaRestriction {
        override val description: String = "Spend this mana only to cast spells from exile"
    }

    /**
     * "Spend this mana only to turn permanents face up."
     *
     * Satisfied by the turn-face-up special action (disguise/morph face-up, CR 707.9 / 702.37e),
     * not by spell casts or ability activations. Used by Overgrown Zealot and Creeping Peeper.
     */
    @SerialName("TurnPermanentsFaceUpOnly")
    @Serializable
    data object TurnPermanentsFaceUpOnly : ManaRestriction {
        override val description: String = "Spend this mana only to turn permanents face up"
    }

    /**
     * "Spend this mana only to cast face-down spells."
     *
     * Satisfied by casting a card face down for its morph (CR 702.37a) or disguise (CR 702.168a)
     * cost — a spell with no name and no characteristics but "2/2 creature" (CR 708.2). Used
     * inside [AnyOf] by Tin Street Gossip.
     *
     * Deliberately *not* satisfied by effects that merely put cards onto the battlefield face
     * down: per the printed ruling on Tin Street Gossip, this mana can't pay for a spell that
     * instructs you to cloak or manifest, because nothing there is cast face down.
     */
    @SerialName("FaceDownSpellsOnly")
    @Serializable
    data object FaceDownSpellsOnly : ManaRestriction {
        override val description: String = "Spend this mana only to cast face-down spells"
    }

    /**
     * "Spend this mana only to unlock a door."
     *
     * Satisfied by the unlock-a-door special action (CR 709.5e), not by spell casts or ability
     * activations. Used inside [AnyOf] by Creeping Peeper.
     */
    @SerialName("UnlockDoorOnly")
    @Serializable
    data object UnlockDoorOnly : ManaRestriction {
        override val description: String = "Spend this mana only to unlock a door"
    }

    /**
     * "Spend this mana only to [A], [B], or [C]." Disjunction of restrictions — the mana is
     * spendable in any context that satisfies *any* of the [restrictions]. Used by Creeping
     * Peeper ("cast an enchantment spell, unlock a door, or turn a permanent face up"). Composing
     * atomic restrictions this way keeps each spend-context check in one place and lets new
     * multi-option mana reuse the existing atoms.
     */
    @SerialName("AnyOf")
    @Serializable
    data class AnyOf(
        val restrictions: List<ManaRestriction>
    ) : ManaRestriction {
        init {
            require(restrictions.isNotEmpty()) { "AnyOf must have at least one restriction" }
        }

        override val description: String = "Spend this mana only to " +
            restrictions.joinToString(", or ") {
                it.description.removePrefix("Spend this mana only to ")
            }
    }

    /**
     * "Spend this mana only to activate an ability." Any activated ability of any source
     * qualifies — unlike [CardTypeSpellsOrAbilitiesOnly], which ties abilities to a card type.
     * Compose with [AnyOf] for "... or to activate an ability" clauses (Purple Dragon Punks:
     * "Spend this mana only to cast an artifact spell or to activate an ability").
     */
    @SerialName("AbilityActivationOnly")
    @Serializable
    data object AbilityActivationOnly : ManaRestriction {
        override val description: String = "Spend this mana only to activate an ability"
    }

    /**
     * "This mana can't be spent to cast a non-[cardTypes] spell" (Hydraulic Helper:
     * "{T}: Add {U}. This mana can't be spent to cast a nonartifact spell").
     *
     * The only **negative** restriction in this family, and the distinction is load-bearing.
     * Every other variant is a whitelist — "spend this mana *only* to …" — which blocks any spend
     * that isn't the named one. This clause blocks exactly one thing, casting a spell that lacks
     * one of [cardTypes], and leaves every other spend legal: activating an ability, paying a ward
     * cost, an "unless that player pays {2}" tax, a cost demanded while something resolves,
     * turning a permanent face up. Modelling it as `AnyOf(CardTypeSpellsOrAbilitiesOnly(…),
     * AbilityActivationOnly)` covers only the first of those and silently rejects the rest.
     */
    @SerialName("CannotCastSpellsOtherThan")
    @Serializable
    data class CannotCastSpellsOtherThan(
        val cardTypes: Set<com.wingedsheep.sdk.core.CardType>
    ) : ManaRestriction {
        init {
            require(cardTypes.isNotEmpty()) { "CannotCastSpellsOtherThan needs at least one card type" }
        }

        override val description: String =
            "This mana can't be spent to cast a non${
                cardTypes.joinToString("/") { it.displayName.lowercase() }
            } spell"
    }

    /**
     * "Spend this mana only to cast a spell from anywhere other than your hand."
     *
     * Used by Mm'menon, the Right Hand's granted artifact ability. Generalizes
     * [CastFromExileOnly] by accepting any non-hand origin (exile, graveyard, top of
     * library, command zone, …), not exile alone — mirroring the printed text. Rejects
     * ability activations (the oracle says "cast a spell," not "activate an ability").
     */
    @SerialName("CastFromNonHandOnly")
    @Serializable
    data object CastFromNonHandOnly : ManaRestriction {
        override val description: String =
            "Spend this mana only to cast a spell from anywhere other than your hand"
    }

    /**
     * "Spend this mana only to cast a spell that has any of the given subtypes."
     *
     * Generalizes [SubtypeSpellsOrAbilitiesOnly] to a *set* of subtypes joined by OR, for
     * cards whose mana is usable on more than one named subtype — e.g. Maelstrom of the
     * Spirit Dragon ("a Dragon spell or an Omen spell"). The [subtypes] are baked into the
     * restriction at the moment the mana is produced, so the restriction stays
     * self-contained and serializable. Unlike [SubtypeSpellsOrAbilitiesOnly] this matches
     * spells only (not activated abilities of sources of the subtype) — every printed
     * multi-subtype variant so far is spell-only.
     */
    @SerialName("SubtypeSpellsOnly")
    @Serializable
    data class SubtypeSpellsOnly(
        val subtypes: Set<String>
    ) : ManaRestriction {
        override val description: String = buildString {
            append("Spend this mana only to cast ")
            append(subtypes.joinToString(" or ") { "a $it spell" })
        }
    }

    /**
     * "Spend this mana only to cast [cardType] spells [or activate abilities of [cardType] sources]."
     *
     * Parameterized over card type so the same restriction shape covers Steelswarm Operator
     * (artifact), hypothetical land/creature/planeswalker variants, and future printings. The
     * two booleans cover the three printed shapes:
     *   - `allowSpells=true,  allowAbilities=false` — Steelswarm Operator's first ability
     *   - `allowSpells=false, allowAbilities=true`  — Steelswarm Operator's second ability
     *   - `allowSpells=true,  allowAbilities=true`  — hypothetical "spells or abilities" mana
     *
     * [negated] flips the type test: "cast non[cardType] spells [or activate abilities of
     * non[cardType] sources]" — e.g. The Emperor of Palamecia's "Spend this mana only to cast
     * a noncreature spell" is `CardTypeSpellsOrAbilitiesOnly(CardType.CREATURE, negated = true)`.
     * Only the type membership is negated; the allowSpells/allowAbilities gating is unchanged.
     *
     * Per the printed ruling, "[cardType] source" includes any object with that card type in
     * any zone (battlefield, hand, graveyard, etc.) — the engine evaluates source type from
     * the activating source's [com.wingedsheep.sdk.core.TypeLine] (plus projected types for
     * battlefield permanents) at payment time.
     */
    @SerialName("CardTypeSpellsOrAbilitiesOnly")
    @Serializable
    data class CardTypeSpellsOrAbilitiesOnly(
        val cardType: CardType,
        val allowSpells: Boolean = true,
        val allowAbilities: Boolean = false,
        val negated: Boolean = false,
    ) : ManaRestriction {
        init {
            require(allowSpells || allowAbilities) {
                "CardTypeSpellsOrAbilitiesOnly must allow at least one of spells or abilities"
            }
        }

        override val description: String = buildString {
            append("Spend this mana only to ")
            val typeName = (if (negated) "non" else "") + cardType.displayName.lowercase()
            val clauses = buildList {
                if (allowSpells) add("cast $typeName spells")
                if (allowAbilities) add("activate abilities of $typeName sources")
            }
            append(clauses.joinToString(" or "))
        }
    }
}
