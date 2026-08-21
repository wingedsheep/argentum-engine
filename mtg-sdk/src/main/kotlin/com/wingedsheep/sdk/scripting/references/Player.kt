package com.wingedsheep.sdk.scripting.references

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Unified player reference type.
 *
 * Used for:
 * - Effect targets: "target player", "each opponent"
 * - Zone scoping: "your graveyard", "opponent's hand"
 * - Counting: "creatures you control"
 */
@Serializable
sealed interface Player {
    val description: String

    // =============================================================================
    // Static Player References
    // =============================================================================

    /** The controller of the ability/effect */
    @SerialName("You")
    @Serializable
    data object You : Player {
        override val description: String = "you"
    }

    /**
     * A genuinely non-targeted "an opponent" — used only where the printed text has a
     * single opponent act without targeting them (a chooser: "an opponent chooses a
     * creature type", "choose ... an opponent"). The engine currently resolves this to
     * the controller's first opponent in turn order; the proper multiplayer flow (the
     * controller picks which opponent) is tracked in `backlog/multiplayer.md`.
     *
     * Do NOT use this for:
     * - "target opponent" → [TargetOpponent]
     * - "each opponent" / "your opponents" / "an opponent controls" (exists/aggregation) → [EachOpponent]
     * - "defending player" / combat-damage-to-a-player triggers → [DefendingPlayer]
     * - "that player" in a per-opponent trigger ("at the beginning of each opponent's
     *   upkeep, that player ...") → [TriggeringPlayer]
     */
    @SerialName("AnOpponent")
    @Serializable
    data object AnOpponent : Player {
        override val description: String = "an opponent"
    }

    /**
     * The defending player, per CR 802.2a: the specific player the ability's source is
     * attacking, determined per attacking creature — never "the opponent" via turn order.
     * Resolves through the source's attack assignment (a creature attacking a planeswalker
     * defends against that planeswalker's controller); for "deals combat damage to a
     * player" triggers whose source has left combat before resolution, the damaged player
     * is read from the trigger context as last-known information.
     */
    @SerialName("DefendingPlayer")
    @Serializable
    data object DefendingPlayer : Player {
        override val description: String = "defending player"
    }

    /** All opponents */
    @SerialName("EachOpponent")
    @Serializable
    data object EachOpponent : Player {
        override val description: String = "each opponent"
    }

    /** All players */
    @SerialName("Each")
    @Serializable
    data object Each : Player {
        override val description: String = "each player"
    }

    /** All players in APNAP order (active player first, then turn order) */
    @SerialName("ActivePlayerFirst")
    @Serializable
    data object ActivePlayerFirst : Player {
        override val description: String = "each player"
    }

    /** Any player (for matching/filtering) */
    @SerialName("Any")
    @Serializable
    data object Any : Player {
        override val description: String = "a player"
    }

    // =============================================================================
    // Target-Bound Player References
    // =============================================================================

    /** A targeted player (resolved at effect execution) */
    @SerialName("TargetPlayer")
    @Serializable
    data object TargetPlayer : Player {
        override val description: String = "target player"
    }

    /**
     * **Every** player among the spell or ability's chosen targets — "those players" after
     * "choose any number of target players" (Officious Interrogation). The plural sibling of
     * [TargetPlayer], which resolves to a single targeted player and so silently reads only the
     * first when a spell targets several.
     *
     * Counting primitives sum over the resolved list, which is what makes "the total number of
     * creatures those players control" one [DynamicAmount] instead of a per-target loop. A target
     * that has become illegal by resolution is already gone from the context's target list, so it
     * contributes nothing — exactly what Officious Interrogation's 2024-02-02 ruling requires.
     */
    @SerialName("EachTargetedPlayer")
    @Serializable
    data object EachTargetedPlayer : Player {
        override val description: String = "those players"
    }

    /** A targeted opponent (resolved at effect execution) */
    @SerialName("TargetOpponent")
    @Serializable
    data object TargetOpponent : Player {
        override val description: String = "target opponent"
    }

    /** A player from the context (for multi-target spells) */
    @SerialName("ContextPlayer")
    @Serializable
    data class ContextPlayer(val index: Int) : Player {
        override val description: String = "that player"
    }

    /**
     * The player currently being considered as a target (CR 115). Bound by the engine's
     * target enumerator/validator to each candidate player in turn while evaluating a
     * [com.wingedsheep.sdk.scripting.targets.TargetPlayer.restriction] /
     * [com.wingedsheep.sdk.scripting.targets.TargetOpponent.restriction]. It only resolves
     * inside that restriction-evaluation context (where `EffectContext.candidatePlayerId`
     * is set) — at effect-execution time there is no candidate, so it resolves to nothing.
     * Reach it through the `Conditions.candidate*` facade rather than constructing it by hand.
     */
    @SerialName("Candidate")
    @Serializable
    data object Candidate : Player {
        override val description: String = "that player"
    }

    /** The player from the trigger context (e.g., player dealt combat damage) */
    @SerialName("TriggeringPlayer")
    @Serializable
    data object TriggeringPlayer : Player {
        override val description: String = "that player"
    }

    // =============================================================================
    // Relational Player References
    // =============================================================================

    /**
     * The opponent locked into the source's [com.wingedsheep.sdk.scripting.ChoiceSlot.OPPONENT]
     * slot (set by an `EntersWithChoice(ChoiceType.OPPONENT, …)` replacement effect).
     * Resolves to that stored player entity id; null if no opponent has been chosen on
     * the source.
     *
     * Used by cards like Jihad ("White creatures get +2/+1 as long as the chosen player
     * controls a nontoken permanent of the chosen color") — `Exists(Player.ChosenOpponent,
     * Zone.BATTLEFIELD, …)` reads the chosen opponent from the source's
     * [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent].
     */
    @SerialName("ChosenOpponent")
    @Serializable
    data object ChosenOpponent : Player {
        override val description: String = "the chosen player"
    }

    /**
     * The player enchanted by the source Aura — read from the source's
     * [com.wingedsheep.engine.state.components.battlefield.AttachedToComponent] target id when it
     * is a player. Used by "enchant player" Auras (Grievous Wound) for both the payoff target
     * ("they lose half their life") and the [com.wingedsheep.sdk.scripting.PreventLifeGain] scope
     * ("enchanted player can't gain life"). Resolves to nothing when the source isn't an Aura
     * attached to a player.
     */
    @SerialName("EnchantedPlayer")
    @Serializable
    data object EnchantedPlayer : Player {
        override val description: String = "enchanted player"
    }

    /** Controller of a permanent (used with EffectTarget) */
    @SerialName("ControllerOf")
    @Serializable
    data class ControllerOf(val targetDescription: String) : Player {
        override val description: String = "its controller"
    }

    /** Owner of a permanent (used with EffectTarget) */
    @SerialName("OwnerOf")
    @Serializable
    data class OwnerOf(val targetDescription: String) : Player {
        override val description: String = "its owner"
    }

    /**
     * The owner of the effect's **source** — the card the ability is printed on, not whoever
     * currently controls it.
     *
     * [OwnerOf] reads the owner of the effect's first *chosen target*, so it is unusable for an
     * ability that names its own source without targeting it. This is that missing case:
     * *"[This creature]'s owner shuffles it into their library and draws three cards"*
     * (Gandalf, Wandering Wizard). It matters exactly when control and ownership diverge — a stolen
     * permanent's ability still acts on the *owner*, per the printed text.
     */
    @SerialName("OwnerOfSource")
    @Serializable
    data object OwnerOfSource : Player {
        override val description: String = "its owner"
    }

    /**
     * The controller of the effect's **source** — read off the source permanent rather than off
     * the resolution context's `controllerId`.
     *
     * Ordinarily that is the same player [You] names, and [You] stays the right reference. This
     * one exists for the case where the context's controller has been **rebound to some other
     * player**, which is exactly what the per-player loops do:
     * [com.wingedsheep.sdk.scripting.values.DynamicAmount.CountPlayersWith] and
     * `ForEach`-over-players evaluate their inner condition once per candidate with
     * `controllerId` set to that candidate, so `You` inside the loop means "the player being
     * tested". A comparison against the ability's *own* controller then has no reference left to
     * reach for — the shape of "for each opponent who has more cards in hand **than you**"
     * (Wojek Investigator), which is `CountPlayersWith(EachOpponent, Compare(Count(You, HAND),
     * GT, Count(ControllerOfSource, HAND)))`.
     *
     * Outside such a loop, prefer [You]. Note the pairing with [OwnerOfSource]: that one reads the
     * source's *owner* (right when control and ownership diverge and the card says "its owner");
     * this one follows control, so a stolen permanent's ability compares against whoever controls
     * it now, as "you" always does.
     */
    @SerialName("ControllerOfSource")
    @Serializable
    data object ControllerOfSource : Player {
        override val description: String = "you"
    }

    /**
     * The distinct owners of the cards currently in the effect source's *linked-exile pile*
     * (the source's [com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent],
     * populated by [com.wingedsheep.sdk.dsl.Effects.ExileUntilLeaves]). Resolves to one entry
     * per distinct owner of a still-exiled linked card — never falling back to "all players" —
     * and to nothing when the pile is empty.
     *
     * Reach it through [com.wingedsheep.sdk.dsl.Effects.ForEachPlayer] on a leaves-the-battlefield
     * trigger to make "the exiled card's owner does X" act on the right player(s). Per the
     * Unidentified Hovership ruling (CR 701.62 manifest dread; DSK 37), when more than one card was
     * exiled, *each player who owns one or more of them* acts once — hence distinct owners, not
     * once per card. It is intentionally a list (per-player loop) reference, not a single-player one.
     */
    @SerialName("OwnersOfLinkedExile")
    @Serializable
    data object OwnersOfLinkedExile : Player {
        override val description: String = "the exiled card's owner"
    }

    /**
     * The controller of the spell or ability that **targeted** the source — the other end of a
     * "becomes the target of a spell or ability" trigger.
     *
     * [TriggeringPlayer] cannot name it: a becomes-target trigger binds the *targeted object* as
     * the triggering entity, and its trigger context deliberately leaves the triggering player
     * null unless the thing targeted was itself a player. What the context does carry is the
     * targeting stack object, and this reference reads that object's controller — the caster of a
     * spell, or the controller of an activated/triggered ability.
     *
     * Used by Fractured Loyalty: *"Whenever enchanted creature becomes the target of a spell or
     * ability, that spell or ability's controller gains control of that creature."*
     *
     * The trigger goes on the stack above the spell that caused it, so ordinarily the targeting
     * object is still on the stack when this resolves. If it left in the meantime (it was
     * countered in response), resolution falls back to that object's last-known controller and
     * finally its owner, per CR 608.2h.
     */
    @SerialName("ControllerOfTargetingSource")
    @Serializable
    data object ControllerOfTargetingSource : Player {
        override val description: String = "that spell or ability's controller"
    }

    // =============================================================================
    // Possessive Forms (for descriptions)
    // =============================================================================

    /** Get possessive form for zone descriptions like "your hand", "opponent's graveyard" */
    val possessive: String
        get() = when (this) {
            You -> "your"
            AnOpponent -> "an opponent's"
            DefendingPlayer -> "defending player's"
            TargetOpponent -> "target opponent's"
            TargetPlayer -> "target player's"
            EachTargetedPlayer -> "those players'"
            Each -> "each player's"
            ActivePlayerFirst -> "each player's"
            EachOpponent -> "each opponent's"
            Any -> "a player's"
            is ContextPlayer -> "that player's"
            Candidate -> "that player's"
            TriggeringPlayer -> "that player's"
            ChosenOpponent -> "the chosen player's"
            EnchantedPlayer -> "enchanted player's"
            is ControllerOf -> "its controller's"
            is OwnerOf -> "its owner's"
            OwnerOfSource -> "its owner's"
            // Names the same player [You] does; the difference is only where it reads from.
            ControllerOfSource -> "your"
            OwnersOfLinkedExile -> "the exiled card's owner's"
            ControllerOfTargetingSource -> "that spell or ability's controller's"
        }
}
