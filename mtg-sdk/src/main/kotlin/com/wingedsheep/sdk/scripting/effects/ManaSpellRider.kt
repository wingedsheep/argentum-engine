package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A side-effect that attaches to a spell when mana carrying this rider is spent on it.
 *
 * Riders are orthogonal to [ManaRestriction]:
 *  - A [ManaRestriction] controls *where* mana may be spent.
 *  - A [ManaSpellRider] controls *what happens to the spell* when the mana is spent.
 *
 * The set of riders consumed during payment is collected by the cast pipeline and
 * applied to the spell as it goes on the stack.
 */
@Serializable
sealed interface ManaSpellRider {
    val description: String

    /**
     * "That spell can't be countered." (Cavern of Souls, with the default [spellFilter].)
     * "If that mana is spent on an instant or sorcery spell, that spell can't be countered."
     * (Boseiju, Who Shelters All, with `spellFilter = GameObjectFilter.InstantOrSorcery`.)
     *
     * On consumption the cast pipeline matches the spell against [spellFilter] using its cast
     * characteristics and, on a match, stamps `CantBeCounteredComponent` on it. A non-matching
     * spell (Boseiju's {C} spent on a creature spell) is left counterable — the rider is a no-op.
     *
     * @property spellFilter Which cast spells the rider makes uncounterable. [GameObjectFilter.Any]
     *   when the mana's restriction already fences where it can be spent (Cavern of Souls).
     */
    @SerialName("MakesSpellUncounterable")
    @Serializable
    data class MakesSpellUncounterable(
        val spellFilter: GameObjectFilter = GameObjectFilter.Any
    ) : ManaSpellRider {
        override val description: String =
            if (spellFilter == GameObjectFilter.Any) "That spell can't be countered"
            else "If that mana is spent on ${article(spellFilter.description)} spell, that spell can't be countered"
    }

    /**
     * "When that mana is spent to cast a creature spell that shares a creature type
     * with your commander, scry [amount]." (Path of Ancestry)
     *
     * On consumption the cast pipeline checks the spell's projected creature subtypes
     * against the spell controller's commander(s). If the spell is a creature spell and
     * shares at least one creature subtype with any of the controller's commanders, a
     * triggered ability is placed on the stack above the spell that, on resolution,
     * scries [amount].
     *
     * If the rider is consumed but the spell doesn't satisfy the condition (non-creature
     * spell, no shared creature type, controller has no commander, etc.) the rider is
     * a no-op. Per CR / Scryfall ruling, creature types are checked at the moment of
     * casting, not at trigger-resolution.
     */
    @SerialName("ScryOnSharedTypeWithCommander")
    @Serializable
    data class ScryOnSharedTypeWithCommander(val amount: Int = 1) : ManaSpellRider {
        override val description: String =
            "When that mana is spent to cast a creature spell that shares a creature type with your commander, scry $amount"
    }

    /**
     * "When you spend this mana to cast a [spellFilter] spell, [effect]." — the delayed triggered
     * ability a mana ability sets up for the spell its mana pays for. Pyromancer's Goggles
     * (`spellFilter = GameObjectFilter.InstantOrSorcery.withColor(Color.RED)`,
     * `effect = CopyTargetSpellEffect(EffectTarget.TriggeringEntity)`): "When that mana is spent to
     * cast a red instant or sorcery spell, copy that spell …". Scaled Nurturer
     * (`GameObjectFilter.Creature.withSubtype("Dragon")`, `Effects.GainLife(2)`): "… you gain 2 life".
     *
     * On consumption the cast pipeline matches the spell against [spellFilter] using its cast
     * characteristics, at payment time. On a match, a triggered ability running [effect] is put on
     * the stack **above** the spell, so it resolves first. [EffectTarget.TriggeringEntity] names the
     * spell ("that spell" / "it"); the mana's controller controls the trigger. The trigger is built
     * with the spell as its source, so it fires whether or not the mana's producer is still on the
     * battlefield, and it still resolves if the spell has left the stack in the meantime.
     *
     * The rider is a no-op when the spell doesn't match (the {G} paid for a non-Dragon spell).
     * Consuming two riders fires [effect] twice, one trigger per rider.
     *
     * The trigger chooses no targets of its own, so [effect] must be untargeted: it may name the
     * spell ([EffectTarget.TriggeringEntity]) and the controller, never a `ContextTarget`.
     *
     * @property spellFilter Which cast spells set the trigger off.
     * @property effect What the trigger does when it resolves.
     */
    @SerialName("TriggersWhenSpent")
    @Serializable
    data class TriggersWhenSpent(
        val spellFilter: GameObjectFilter,
        val effect: Effect,
    ) : ManaSpellRider {
        override val description: String =
            "When you spend this mana to cast ${article(spellFilter.description)} spell, " +
                effect.description.replaceFirstChar { it.lowercase() }
    }

    /**
     * "If that mana is spent on a [spellFilter] spell, it gains [keyword] until end of turn."
     * (Carnelian Orb of Dragonkind, with `spellFilter = GameObjectFilter.Creature.withSubtype("Dragon")`
     * and `keyword = Keyword.HASTE`.)
     *
     * Unlike the trigger-queuing riders this is a continuous effect, not a triggered ability — the
     * printed card puts nothing on the stack. On consumption the cast pipeline matches the spell
     * against [spellFilter] and, on a match, floats an end-of-turn keyword grant keyed to the spell's
     * entity id. A permanent spell keeps that id as it resolves, so the keyword is live the moment it
     * becomes a permanent; on a non-permanent spell the grant simply never has a permanent to apply to.
     *
     * Matching happens at payment time against the spell's cast characteristics, per the printed
     * ruling: mana spent on a non-Dragon spell that *becomes* a Dragon later in the turn grants
     * nothing, and an instant or sorcery that makes Dragon tokens is not a Dragon creature spell.
     *
     * With `duration = Duration.Permanent` the grant has no end: "If that mana is spent on a
     * creature spell, it gains haste." (Hall of the Bandit Lord). Instead of a floating effect on
     * the stack object, the keyword is frozen onto the spell's cast record and granted to the
     * permanent it becomes as it enters, so a spell that's countered or otherwise never resolves
     * carries nothing into another zone (CR 400.7). The permanent keeps the keyword until it leaves
     * the battlefield.
     *
     * @property keyword Keyword enum name (e.g. `"HASTE"`), matching [GrantKeywordEffect.keyword].
     * @property spellFilter Which cast spells the rider grants [keyword] to.
     * @property duration [Duration.EndOfTurn] (the default) or [Duration.Permanent].
     */
    @SerialName("GrantsKeywordWhenSpent")
    @Serializable
    data class GrantsKeywordWhenSpent(
        val keyword: String,
        val spellFilter: GameObjectFilter,
        val duration: Duration = Duration.EndOfTurn,
    ) : ManaSpellRider {
        init {
            require(duration == Duration.EndOfTurn || duration == Duration.Permanent) {
                "GrantsKeywordWhenSpent supports only EndOfTurn or Permanent, got $duration"
            }
        }

        constructor(keyword: Keyword, spellFilter: GameObjectFilter, duration: Duration = Duration.EndOfTurn) :
            this(keyword.name, spellFilter, duration)

        override val description: String =
            "If that mana is spent on ${article(spellFilter.description)} spell, it gains " +
                keyword.lowercase().replace('_', ' ') +
                (if (duration == Duration.Permanent) "" else " until end of turn")
    }

    /**
     * "If that mana is spent on a [spellFilter] spell, that creature enters with [count] additional
     * [counterType] counter(s) on it." (Guildmages' Forum, with
     * `spellFilter = GameObjectFilter.Creature and GameObjectFilter.Multicolored`.)
     *
     * Like [GrantsKeywordWhenSpent] this is not a triggered ability — the printed card puts nothing on
     * the stack, so it can't be responded to. On consumption the cast pipeline matches the spell against
     * [spellFilter] (at payment time, against its cast characteristics) and, on a match, owes the
     * counters to the spell (`SpellOnStackComponent.additionalEntryCounters`); they're placed as the
     * permanent enters, so counter-placement replacements (Hardened Scales) apply then. A countered
     * spell carries none into its next zone, and a copy of the spell owes none.
     *
     * Neither neighbour takes this axis: [GrantsKeywordWhenSpent] hands over a keyword, not counters, and
     * [TriggersWhenSpent] with an `AddCounters` effect is the *triggered* reading — right for Jade Orb of
     * Dragonkind's "When you spend this mana …", wrong for an "If that mana is spent …" rider, whose
     * counters must not wait on (or be stoppable as) a trigger above the spell.
     *
     * @property spellFilter Which cast spells the rider adds counters to.
     * @property counterType The kind of counter the permanent enters with.
     * @property count How many additional counters each rider-carrying mana owes.
     */
    @SerialName("EntersWithCountersWhenSpent")
    @Serializable
    data class EntersWithCountersWhenSpent(
        val spellFilter: GameObjectFilter,
        val counterType: CounterType = CounterType.PLUS_ONE_PLUS_ONE,
        val count: Int = 1,
    ) : ManaSpellRider {
        init {
            require(count > 0) { "EntersWithCountersWhenSpent needs a positive count, got $count" }
        }

        override val description: String =
            "If that mana is spent on ${article(spellFilter.description)} spell, that creature enters with " +
                (if (count == 1) "an additional ${counterType.printed} counter" else "$count additional ${counterType.printed} counters") +
                " on it"
    }
}

/** "an instant or sorcery", "a creature" — the article a rider description puts before a filter. */
private fun article(noun: String): String =
    if (noun.firstOrNull()?.lowercaseChar() in setOf('a', 'e', 'i', 'o', 'u')) "an $noun" else "a $noun"
