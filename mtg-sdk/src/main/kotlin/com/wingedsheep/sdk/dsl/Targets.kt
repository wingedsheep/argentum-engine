package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.TargetChooser
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlaneswalker
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlayer
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.targets.TargetOpponentOrPlaneswalker
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetPermanentOrPlayer
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.targets.TargetPlayerOrPlaneswalker
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/**
 * The target shapes that are **not** a single game object: players, "any target", and the mixed
 * "X or Y" shapes that span players and permanents.
 *
 * ```kotlin
 * val player = target(Targets.Player)
 * val t = target(Targets.Any)
 * ```
 *
 * An object target — a permanent, a card in a graveyard, a spell or ability on the stack — has no
 * preset here: it is declared by its filter, `target(TargetFilter.Creature.youControl())`, so every
 * refinement composes instead of needing a name of its own (see [TargetDeclarations]).
 */
object Targets {

    // =========================================================================
    // Players
    // =========================================================================

    /** Target player. */
    val Player: TargetRequirement = TargetPlayer()

    /** Target opponent. */
    val Opponent: TargetRequirement = TargetOpponent()

    // =========================================================================
    // Any target
    // =========================================================================

    /** Any target (creature, player, planeswalker, or battle). */
    val Any: TargetRequirement = AnyTarget()

    /** Any damageable target satisfying [filter], including player candidates. */
    fun Any(filter: GameObjectFilter): TargetRequirement = AnyTarget(filter = filter)

    /**
     * "Any target of an opponent's choice" — a real target of your spell/ability that an
     * opponent selects (Cuombajj Witches). Announced and resolved like any target, with
     * legality (hexproof/protection) measured relative to you, the controller. List it after
     * the controller-chosen targets in a script. See [TargetChooser].
     */
    val AnyChosenByOpponent: TargetRequirement = AnyTarget(chooser = TargetChooser.Opponent)

    /**
     * Any target other than the creature the source is attached to — for Aura/Equipment
     * abilities worded "enchanted/equipped creature deals damage … to any other target".
     */
    val AnyOtherThanEnchantedCreature: TargetRequirement =
        TargetOther(AnyTarget(), excludeAttachedCreature = true)

    // =========================================================================
    // Mixed shapes
    // =========================================================================

    /** Target creature or player. */
    val CreatureOrPlayer: TargetRequirement = TargetCreatureOrPlayer()

    /**
     * Target permanent or player — any permanent on the battlefield, or any player
     * (Powerful Broker). For a narrower permanent half ("target artifact or player"),
     * construct [TargetPermanentOrPlayer] with a `permanentFilter`.
     */
    val PermanentOrPlayer: TargetRequirement = TargetPermanentOrPlayer()

    /** Target player or battle (Onakke Javelineer). */
    val PlayerOrBattle: TargetRequirement = TargetPermanentOrPlayer(permanentFilter = TargetFilter.Battle)

    /** Target opponent or battle (Ayara, Widow of the Realm). */
    val OpponentOrBattle: TargetRequirement =
        TargetPermanentOrPlayer(permanentFilter = TargetFilter.Battle, opponentsOnly = true)

    /** Target creature or planeswalker. */
    val CreatureOrPlaneswalker: TargetRequirement = TargetCreatureOrPlaneswalker()

    /** Target player or planeswalker. */
    val PlayerOrPlaneswalker: TargetRequirement = TargetPlayerOrPlaneswalker()

    /** Target opponent or planeswalker. */
    val OpponentOrPlaneswalker: TargetRequirement = TargetOpponentOrPlaneswalker()
}
