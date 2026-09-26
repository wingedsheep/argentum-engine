package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.ControlChangeDirection
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.EventPattern.*
import com.wingedsheep.sdk.scripting.ExploreReveal
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TapReason
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.events.DamagePredicate
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The trigger vocabulary: a trigger is written the way its Oracle text reads — a **subject**, then
 * a **verb**.
 *
 * ```kotlin
 * Triggers.self.enters()                                    // When this creature enters
 * Triggers.another(Creature.youControl()).dies()            // Whenever another creature you control dies
 * Triggers.a(Land.youControl()).enters()                    // Whenever a land you control enters
 * Triggers.attached.attacks()                               // Whenever equipped creature attacks
 * Triggers.oneOrMore(Creature.youControl()).die()           // Whenever one or more creatures you control die
 * Triggers.you.casts(GameObjectFilter.Noncreature)          // Whenever you cast a noncreature spell
 * Triggers.anOpponent.draws()                               // Whenever an opponent draws a card
 * Triggers.you.beginningOf(Step.UPKEEP)                     // At the beginning of your upkeep
 * ```
 *
 * **The subject decides the [TriggerBinding]**, so a card never passes one:
 *
 * | Subject | Printed wording | Binding |
 * |---|---|---|
 * | [self] | "this creature", "~", "this card" | [TriggerBinding.SELF] |
 * | [attached] | "enchanted creature", "equipped creature" | [TriggerBinding.ATTACHED] |
 * | [a] | "a creature you control", "a player's creature" | [TriggerBinding.ANY] |
 * | [another] | "another creature you control" | [TriggerBinding.OTHER] |
 * | [oneOrMore] / [oneOrMoreOther] | "one or more creatures …" (a batch, CR 603.2c) | [TriggerBinding.ANY] |
 * | [you] / [anOpponent] / [anyPlayer] / [chosenOpponent] | a player does something | [TriggerBinding.ANY] |
 *
 * "A" vs "another" is load-bearing: a landfall trigger on a *land* only sees itself enter under
 * [a]. Pick the subject the Oracle text prints.
 *
 * Every verb lowers to the same [TriggerSpec] (an [EventPattern] plus a binding) the engine reads;
 * this object is authoring vocabulary only. A verb whose event cannot carry the subject's filter
 * rejects a filtered subject instead of dropping the filter.
 */
object Triggers {

    /** "this creature / this permanent / this card" — [TriggerBinding.SELF]. */
    val self: ObjectTriggerSubject = ObjectTriggerSubject(null, TriggerBinding.SELF)

    /** "enchanted / equipped creature" (the permanent this Aura or Equipment is attached to) — [TriggerBinding.ATTACHED]. */
    val attached: ObjectTriggerSubject = ObjectTriggerSubject(null, TriggerBinding.ATTACHED)

    /**
     * "a [filter]" — any matching object, the source included — [TriggerBinding.ANY]. The filter's
     * "you control" resolves against the controller of the permanent bearing the trigger. Leave
     * [filter] out for an unrestricted subject ("whenever a creature attacks" on an event that
     * already means creatures).
     */
    fun a(filter: GameObjectFilter? = null): ObjectTriggerSubject = ObjectTriggerSubject(filter, TriggerBinding.ANY)

    /** "another [filter]" — any matching object except the source — [TriggerBinding.OTHER]. */
    fun another(filter: GameObjectFilter? = null): ObjectTriggerSubject =
        ObjectTriggerSubject(filter, TriggerBinding.OTHER)

    /**
     * "one or more [filter]" — a batching trigger (CR 603.2c) that fires once per simultaneous
     * batch, not once per object.
     */
    fun oneOrMore(filter: GameObjectFilter): BatchTriggerSubject = BatchTriggerSubject(filter, excludeSource = false)

    /** "one or more other [filter]" — [oneOrMore] with the source's own membership in the batch excluded. */
    fun oneOrMoreOther(filter: GameObjectFilter): BatchTriggerSubject = BatchTriggerSubject(filter, excludeSource = true)

    /** "you" — the controller of the permanent bearing the trigger. */
    val you: PlayerTriggerSubject = PlayerTriggerSubject(Player.You)

    /** "an opponent" — fires for each opponent that does it; "that player" is `Player.TriggeringPlayer`. */
    val anOpponent: PlayerTriggerSubject = PlayerTriggerSubject(Player.EachOpponent)

    /** "a player" / "each player" — any player, you included. */
    val anyPlayer: PlayerTriggerSubject = PlayerTriggerSubject(Player.Each)

    /** "the chosen opponent" — the player recorded under `ChoiceSlot.OPPONENT` as the source entered (The Rack). */
    val chosenOpponent: PlayerTriggerSubject = PlayerTriggerSubject(Player.ChosenOpponent)

    /** Any other player reference as the subject. Prefer the named subjects above. */
    fun player(player: Player): PlayerTriggerSubject = PlayerTriggerSubject(player)

    /** Combines trigger atoms that use the same binding into one disjunctive trigger. */
    fun or(first: TriggerSpec, second: TriggerSpec, vararg others: TriggerSpec): TriggerSpec {
        val triggers = listOf(first, second) + others
        require(triggers.all { it.binding == first.binding }) {
            "Triggers.or requires every trigger to use the same binding"
        }
        return TriggerSpec(
            event = AnyOf(triggers.flatMap {
                val event = it.event
                if (event is AnyOf) event.events else listOf(event)
            }),
            binding = first.binding
        )
    }
}

/**
 * An object as the subject of a trigger — see [Triggers.self], [Triggers.attached], [Triggers.a],
 * [Triggers.another]. Its verbs describe what happens *to* or is done *by* that object.
 */
class ObjectTriggerSubject internal constructor(
    private val filter: GameObjectFilter?,
    private val binding: TriggerBinding,
) {
    private val filterOrAny: GameObjectFilter get() = filter ?: GameObjectFilter.Any

    private fun spec(event: EventPattern) = TriggerSpec(event = event, binding = binding)

    private fun unfiltered(verb: String) {
        require(filter == null) { "Triggers.<subject>.$verb() can't carry a subject filter — its event has no filter axis" }
    }

    private fun only(verb: String, vararg allowed: TriggerBinding) {
        require(binding in allowed) { "Triggers.<subject>.$verb() supports ${allowed.joinToString()} subjects, not $binding" }
    }

    /**
     * Narrows [Triggers.self] or [Triggers.attached] by a characteristic the object must have when
     * the event happens ("when this creature, if it's a Spider, …"). The filtered subjects [Triggers.a]
     * and [Triggers.another] take their filter directly.
     */
    fun matching(filter: GameObjectFilter): ObjectTriggerSubject {
        require(this.filter == null) { "Triggers.<subject>.matching() narrows an unfiltered subject; pass the filter to a()/another() instead" }
        return ObjectTriggerSubject(filter, binding)
    }

    // ---- Zone changes ------------------------------------------------------------------------

    /** "enters" (the battlefield). [from] restricts the zone it came from — "enters from exile". */
    fun enters(from: Zone? = null): TriggerSpec =
        spec(ZoneChangeEvent(filter = filterOrAny, from = from, to = Zone.BATTLEFIELD))

    /**
     * "leaves the battlefield", to [to] (any zone when null), or anywhere but [excludeTo] ("leaves
     * without dying" is `excludeTo = Zone.GRAVEYARD`). [excludeSacrifice] skips a leave caused by a
     * sacrifice. [asCraftMaterial] is "is exiled from the battlefield while you're activating a craft
     * ability" (CR 702.167, Market Gnome) — only a Craft-cost exile, never a removal-style one.
     *
     * "Dies" (to a graveyard) is [dies].
     */
    fun leaves(
        to: Zone? = null,
        excludeTo: Zone? = null,
        excludeSacrifice: Boolean = false,
        asCraftMaterial: Boolean = false,
    ): TriggerSpec {
        require(to != Zone.GRAVEYARD || excludeTo != null || excludeSacrifice || asCraftMaterial) {
            "leaves(to = Zone.GRAVEYARD) is dies()"
        }
        return spec(
            ZoneChangeEvent(
                filter = filterOrAny,
                from = Zone.BATTLEFIELD,
                to = to,
                excludeTo = excludeTo,
                excludeSacrifice = excludeSacrifice,
                requireCraftMaterial = asCraftMaterial,
            )
        )
    }

    /**
     * "dies" — put into a graveyard from the battlefield (CR 700.4). Also the wording for a
     * noncreature permanent "put into a graveyard from the battlefield": same event.
     */
    fun dies(): TriggerSpec =
        spec(ZoneChangeEvent(filter = filterOrAny, from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD))

    /**
     * A zone change that doesn't touch the battlefield — "is put into a graveyard from anywhere"
     * (`changesZone(to = Zone.GRAVEYARD)`), "is put into your hand from your library". Battlefield
     * moves are [enters] / [leaves] / [dies].
     */
    fun changesZone(from: Zone? = null, to: Zone? = null, excludeTo: Zone? = null): TriggerSpec {
        require(from != Zone.BATTLEFIELD && to != Zone.BATTLEFIELD) {
            "a battlefield zone change is enters()/leaves()/dies()"
        }
        require(from != null || to != null) { "changesZone() needs a from or a to zone" }
        return spec(ZoneChangeEvent(filter = filterOrAny, from = from, to = to, excludeTo = excludeTo))
    }

    // ---- Combat ------------------------------------------------------------------------------

    /**
     * "attacks". [requires] is a conjunctive set of [AttackPredicate]s — "attacks alone"
     * (`AttackPredicate.Alone`), "attacks a player" (`DefenderIsPlayer`, which never fires on a
     * planeswalker or battle, CR 508.1), "attacks for the first time each turn"
     * (`FirstTimeEachTurn`), battalion (`AttackerCountAtLeast(3)`).
     */
    fun attacks(requires: Set<AttackPredicate> = emptySet()): TriggerSpec =
        spec(AttackEvent(filter = filter, requires = requires))

    /**
     * "blocks". [attackerFilter] constrains the blocked attacker ("blocks a creature with flying");
     * [minBlockedAttackers] is "blocks two or more creatures" (Lairwatch Giant), a single trigger
     * per combat. Both are [Triggers.self]-only — the detector's shared branch can't honour them.
     */
    fun blocks(attackerFilter: GameObjectFilter? = null, minBlockedAttackers: Int = 1): TriggerSpec {
        require(attackerFilter == null || binding == TriggerBinding.SELF) {
            "attackerFilter is only supported with TriggerBinding.SELF"
        }
        require(minBlockedAttackers >= 1) { "minBlockedAttackers must be at least 1" }
        require(minBlockedAttackers == 1 || binding == TriggerBinding.SELF) {
            "minBlockedAttackers is only supported with TriggerBinding.SELF"
        }
        require(minBlockedAttackers == 1 || attackerFilter == null) {
            "minBlockedAttackers counts every blocked attacker, so it can't be combined with attackerFilter"
        }
        return spec(BlockEvent(filter = filter, attackerFilter = attackerFilter, minBlockedAttackers = minBlockedAttackers))
    }

    /** "becomes blocked". */
    fun becomesBlocked(): TriggerSpec = spec(BecomesBlockedEvent(filter = filter))

    /**
     * "blocks or becomes blocked [by a creature matching [by]]" — the combat partner is the
     * triggering entity. The partner-less wording (Spitting Slug) and "by **one or more** Orcs"
     * (Dwarven Soldier) are a single trigger per combat; "by **a** creature" (Corrosive Ooze) fires
     * once per matching partner, which is why [oncePerCombat] defaults on only without [by].
     */
    fun blocksOrBecomesBlocked(by: GameObjectFilter? = null, oncePerCombat: Boolean = by == null): TriggerSpec {
        unfiltered("blocksOrBecomesBlocked")
        return spec(BlocksOrBecomesBlockedByEvent(partnerFilter = by, oncePerCombat = oncePerCombat))
    }

    /** "attacks and isn't blocked" — fires as declare blockers ends with no blocker assigned (CR 509.3g). */
    fun attacksAndIsntBlocked(): TriggerSpec {
        unfiltered("attacksAndIsntBlocked")
        return spec(BecomesUnblockedEvent)
    }

    // ---- Damage ------------------------------------------------------------------------------

    /**
     * "deals damage [to [to]]". [batch] is the "one or more … are dealt damage" template (CR
     * 603.2c, [TriggerBinding.ANY] only); [requireExcess] is "excess damage". Combat damage is
     * [dealsCombatDamage].
     */
    fun dealsDamage(
        to: Recipient = Recipient.Any,
        damageType: DamageType = DamageType.Any,
        requireExcess: Boolean = false,
        batch: Boolean = false,
        requires: Set<DamagePredicate> = emptySet(),
    ): TriggerSpec = spec(
        DealsDamageEvent(
            damageType = damageType,
            recipient = to,
            sourceFilter = filter,
            requires = requires,
            requireExcess = requireExcess,
            batch = batch,
        )
    )

    /** "deals combat damage [to [to]]" — [dealsDamage] with [DamageType.Combat]. */
    fun dealsCombatDamage(
        to: Recipient = Recipient.Any,
        requireExcess: Boolean = false,
        batch: Boolean = false,
        requires: Set<DamagePredicate> = emptySet(),
    ): TriggerSpec = dealsDamage(to, DamageType.Combat, requireExcess, batch, requires)

    /** "is dealt damage [by a source matching [by]]". */
    fun isDealtDamage(by: GameObjectFilter = GameObjectFilter.Any): TriggerSpec {
        unfiltered("isDealtDamage")
        return spec(DamageReceivedEvent(source = by))
    }

    /**
     * "a creature [matching [dying]] dealt damage by <subject> this turn dies" (Soul Collector,
     * Trophy Hunter). [dying] is matched against the dead creature's last-known information
     * (CR 608.2h). Under [Triggers.attached] the damage is read off the attachment target when the
     * creature dies, so the Equipment can move in between (Scythe of the Wretched). Under
     * [Triggers.a] the subject filter is the damaging source, matched by last-known information from
     * when it dealt the damage (Shelob, "by a Spider you controlled").
     */
    fun damagedCreatureDies(dying: GameObjectFilter? = null): TriggerSpec =
        spec(CreatureDealtDamageBySourceDiesEvent(sourceFilter = filter, dyingFilter = dying))

    // ---- Tap / untap / face / transform ------------------------------------------------------

    /**
     * "becomes tapped". [reason] restricts why ([TapReason.TEAMWORK] — "to pay a teamwork cost",
     * CR 702.194a); [firstTimeEachTurn] is per *permanent* ("if it's the first time that creature has
     * become tapped this turn"), not per ability — that is `oncePerTurn`. The "one or more … become
     * tapped" batch is [BatchTriggerSubject.becomeTapped].
     */
    fun becomesTapped(reason: TapReason? = null, firstTimeEachTurn: Boolean = false): TriggerSpec =
        spec(TapEvent(filter = filter, reason = reason, firstTimeEachTurn = firstTimeEachTurn))

    /** "becomes untapped". */
    fun becomesUntapped(): TriggerSpec = spec(UntapEvent(filter = filter))

    /**
     * "is turned face up" — this, the attached, or (under an unfiltered [Triggers.a]) any permanent
     * (Aphetto Runecaster). "Whenever a [filter] you control is turned face up", matched on its
     * face-up characteristics, is [PlayerTriggerSubject.permanentTurnedFaceUp].
     */
    fun turnedFaceUp(): TriggerSpec {
        unfiltered("turnedFaceUp")
        return spec(TurnFaceUpEvent)
    }

    /** "transforms" — [intoBackFace] `true` to the back face, `false` to the front, `null` either way. */
    fun transforms(intoBackFace: Boolean? = null): TriggerSpec {
        unfiltered("transforms")
        return spec(TransformEvent(intoBackFace = intoBackFace))
    }

    /** "phases in" (CR 702.26). */
    fun phasesIn(): TriggerSpec = spec(PhasesInEvent(filter = filter))

    // ---- Targeting ---------------------------------------------------------------------------

    /**
     * "becomes the target of a spell or ability". [of] narrows the *targeting* object ("an Aura
     * spell" — Brine Comber). [byYou] / [byOpponent] are "you control" / "an opponent controls";
     * [spellsOnly] / [abilitiesOnly] drop the other half. [firstTimeEachTurn] with [byYou] is
     * valiant. [includeSpellTargets] also fires when a matching *spell* on the stack is targeted
     * (Surrak, Elusive Hunter); [includePlayerTargets] widens to targeted players and needs an
     * unfiltered subject (Loki, God of Mischief).
     */
    fun becomesTarget(
        of: GameObjectFilter? = null,
        byYou: Boolean = false,
        byOpponent: Boolean = false,
        spellsOnly: Boolean = false,
        abilitiesOnly: Boolean = false,
        firstTimeEachTurn: Boolean = false,
        includeSpellTargets: Boolean = false,
        includePlayerTargets: Boolean = false,
    ): TriggerSpec = spec(
        BecomesTargetEvent(
            targetFilter = filterOrAny,
            byYou = byYou,
            byOpponent = byOpponent,
            firstTimeEachTurn = firstTimeEachTurn,
            includeSpellTargets = includeSpellTargets,
            spellsOnly = spellsOnly,
            includePlayerTargets = includePlayerTargets,
            abilitiesOnly = abilitiesOnly,
            sourceFilter = of,
        )
    )

    // ---- Counters ----------------------------------------------------------------------------

    /**
     * "one or more [type] counters are put on" it ([type] null = any kind). [by] is "you put"
     * ([Player.You]); [firstTimeEachTurn] is "for the first time this turn" (Stalwart Successor);
     * [batch] is the "on one or more <permanents>" template (CR 603.2c) — see
     * [CountersPlacedEvent.batch].
     */
    fun getsCounters(
        type: CounterType? = null,
        by: Player? = null,
        firstTimeEachTurn: Boolean = false,
        batch: Boolean = false,
    ): TriggerSpec = spec(
        CountersPlacedEvent(
            counterType = type,
            filter = filterOrAny,
            firstTimeEachTurn = firstTimeEachTurn,
            placedBy = by,
            batch = batch,
        )
    )

    /**
     * "one or more [type] counters are removed from" it. [lastRemoved] is "when the last [type]
     * counter is removed" (CR 310.12b and the other countdown mechanics); [byDamagePrevention] is a
     * removal made to prevent damage.
     */
    fun losesCounters(
        type: CounterType? = null,
        lastRemoved: Boolean = false,
        byDamagePrevention: Boolean = false,
    ): TriggerSpec = spec(
        CountersRemovedEvent(
            counterType = type,
            filter = filterOrAny,
            lastRemoved = lastRemoved,
            byDamagePrevention = byDamagePrevention,
        )
    )

    // ---- Keyword actions done by / to the object ---------------------------------------------

    /** "trains" (CR 702.149c) — only when a resolving training ability's counter actually lands. */
    fun trains(): TriggerSpec {
        unfiltered("trains")
        return spec(TrainedEvent)
    }

    /**
     * "a [quality] is championed with this creature" (CR 702.72) — the champion clause already
     * restricts what can be exiled, so the quality isn't restated.
     */
    fun champions(): TriggerSpec {
        unfiltered("champions")
        return spec(ChampionedEvent)
    }

    /** "crews a Vehicle" — the Vehicle is `EffectTarget.TriggeringEntity`. */
    fun crews(): TriggerSpec {
        unfiltered("crews")
        return spec(CrewsEvent)
    }

    /** "saddles a Mount". */
    fun saddles(): TriggerSpec {
        unfiltered("saddles")
        return spec(SaddlesEvent)
    }

    /** "becomes saddled [for the first time each turn]" (CR 702.171b). */
    fun becomesSaddled(firstTimeEachTurn: Boolean = false): TriggerSpec =
        spec(BecameSaddledEvent(filter = filterOrAny, firstTimeEachTurn = firstTimeEachTurn))

    /** "becomes renowned" (CR 702.112b). */
    fun becomesRenowned(): TriggerSpec = spec(BecameRenownedEvent(filter = filterOrAny))

    /** "becomes plotted" (CR 718) — fires for the plotted card itself, face up in exile. */
    fun becomesPlotted(): TriggerSpec {
        unfiltered("becomesPlotted")
        only("becomesPlotted", TriggerBinding.SELF)
        return spec(BecomesPlottedEvent)
    }

    /** "explores" (CR 701.44), optionally gated on the revealed card ([ExploreReveal.LAND] / `NONLAND`). */
    fun explores(revealed: ExploreReveal = ExploreReveal.ANY): TriggerSpec =
        spec(ExploredEvent(filter = filter, revealedType = revealed))

    /** "connives" (CR 701.50) — a real connive only, not the Teo looting shape. */
    fun connives(): TriggerSpec = spec(ConnivedEvent(filter = filter))

    /**
     * The subject (an Aura or Equipment) "becomes attached to" a permanent matching [to] (CR 603.2f).
     * [controller] is who must control the attachment ("an Aura you control"). The host is
     * `EffectTarget.AttachedToTriggeringPermanent`.
     */
    fun becomesAttached(to: GameObjectFilter = GameObjectFilter.Any, controller: Player = Player.Any): TriggerSpec = spec(
        BecomesAttachedEvent(attachmentFilter = filterOrAny, attachmentController = controller, attachedToFilter = to)
    )

    /**
     * The subject "becomes unattached from" a permanent matching [from] (CR 701.3d) — every
     * unattach path, including either side leaving and the SBA unattach.
     */
    fun becomesUnattached(from: GameObjectFilter = GameObjectFilter.Any, controller: Player = Player.Any): TriggerSpec = spec(
        BecomesUnattachedEvent(attachmentFilter = filterOrAny, attachmentController = controller, unattachedFromFilter = from)
    )

    /**
     * Control of the object changes. [ControlChangeDirection.GAINED] on [Triggers.self] is "when you
     * gain control of this"; `LOST` is "when you lose control of" it (Stolen Uniform's watched
     * permanent). [toOpponent] with `LOST` under [Triggers.a] is "whenever an opponent gains control
     * of a permanent from you" — owned by the *old* controller via look-back-in-time (CR 603.10).
     */
    fun controlChanges(direction: ControlChangeDirection, toOpponent: Boolean = false): TriggerSpec {
        unfiltered("controlChanges")
        return spec(ControlChangeEvent(direction, requireOpponent = toOpponent))
    }

    // ---- Things that happen to a card rather than a permanent --------------------------------

    /** "when you cast this spell" — a cast trigger, on the stack (pair with `interveningIf`, CR 603.4). */
    fun isCast(): TriggerSpec {
        unfiltered("isCast")
        only("isCast", TriggerBinding.SELF)
        return spec(CastThisSpellEvent)
    }

    /** "when you cycle this card". */
    fun isCycled(): TriggerSpec {
        unfiltered("isCycled")
        only("isCycled", TriggerBinding.SELF)
        return spec(CycleEvent(Player.You))
    }

    /** "when you discard this card" — fires as it's discarded, wherever the discard sends it (madness). */
    fun isDiscarded(): TriggerSpec {
        unfiltered("isDiscarded")
        only("isDiscarded", TriggerBinding.SELF)
        return spec(DiscardEvent(player = Player.You))
    }

    /** "when you sacrifice this" — only a sacrifice; [dies] also fires on destruction. */
    fun isSacrificed(): TriggerSpec {
        unfiltered("isSacrificed")
        only("isSacrificed", TriggerBinding.SELF)
        return spec(PermanentsSacrificedEvent())
    }

    /**
     * "when you unlock this door" (CR 709.5h) — authored on a Room face; fires only for the face that
     * owns it, whether unlocked by being cast or by the unlock special action.
     */
    fun doorUnlocked(): TriggerSpec {
        unfiltered("doorUnlocked")
        only("doorUnlocked", TriggerBinding.SELF)
        return spec(DoorUnlockedEvent(Player.You))
    }

    /**
     * "at the beginning of enchanted creature's controller's [step]" (Lingering Death) — the step
     * belongs to the attached permanent's controller.
     */
    fun beginningOf(step: Step): TriggerSpec {
        unfiltered("beginningOf")
        only("beginningOf", TriggerBinding.ATTACHED)
        return spec(StepEvent(step, Player.You))
    }

    /**
     * "whenever [by] activates an ability of <subject> without {T} in its activation cost" — the
     * attached form of [PlayerTriggerSubject.activatesAbility] with `withoutTapInCost` (Artifact
     * Possession). The activated permanent is the triggering entity.
     */
    fun hasAbilityActivatedWithoutTap(by: Player = Player.Each): TriggerSpec {
        unfiltered("hasAbilityActivatedWithoutTap")
        only("hasAbilityActivatedWithoutTap", TriggerBinding.ATTACHED)
        return spec(AbilityActivatedEvent(player = by, requireNoTapInCost = true))
    }
}

/**
 * "one or more [filter] …" as the subject — batching triggers (CR 603.2c) that fire once per
 * simultaneous batch however many objects are in it. See [Triggers.oneOrMore] /
 * [Triggers.oneOrMoreOther].
 *
 * For [enter] and [die], a filter with no controller predicate means "you control" — say it with
 * `.youControl()` anyway; `.opponentControls()` / `.anyController()` widen it.
 */
class BatchTriggerSubject internal constructor(
    private val filter: GameObjectFilter,
    private val excludeSource: Boolean,
) {
    private fun spec(event: EventPattern) = TriggerSpec(event = event, binding = TriggerBinding.ANY)

    private fun noOther(verb: String) {
        require(!excludeSource) { "Triggers.oneOrMoreOther(…).$verb() isn't supported — its event has no source exclusion" }
    }

    /** "enter" (the battlefield). The matching members are the payoff's captured collection. */
    fun enter(): TriggerSpec = spec(PermanentsEnteredEvent(filter = filter, excludeSource = excludeSource))

    /** "die" — once per death batch (CR 603.3b), so a board wipe fires it once. */
    fun die(): TriggerSpec = spec(CreaturesYouControlDiedEvent(filter = filter, excludeSelf = excludeSource))

    /** "leave the battlefield without dying" — to any zone but a graveyard. */
    fun leaveWithoutDying(): TriggerSpec =
        spec(LeaveBattlefieldWithoutDyingEvent(filter = filter, excludeSelf = excludeSource))

    /**
     * "block" — once per block declaration however many matching creatures block, and not at all
     * when none does (Tide of War: `oneOrMore(GameObjectFilter.Creature).block()`). The filter
     * reads the blockers with no implicit controller.
     */
    fun block(): TriggerSpec {
        noOther("block")
        return spec(BlockEvent(filter = filter, batch = true))
    }

    /** "deal combat damage to a player". */
    fun dealCombatDamageToAPlayer(): TriggerSpec {
        noOther("dealCombatDamageToAPlayer")
        return spec(OneOrMoreDealCombatDamageToPlayerEvent(sourceFilter = filter))
    }

    /** "deal combat damage to you" — defensive; fires once per combat-damage batch. */
    fun dealCombatDamageToYou(): TriggerSpec {
        noOther("dealCombatDamageToYou")
        return spec(OneOrMoreDealCombatDamageToYouEvent(sourceFilter = filter))
    }

    /**
     * "become tapped". A batch mixing causes is narrowed to the taps matching [reason], not
     * discarded. There is no first-time-each-turn rider: it's per-permanent and has no batch reading.
     */
    fun becomeTapped(reason: TapReason? = null): TriggerSpec {
        noOther("becomeTapped")
        return spec(TapEvent(filter = filter, batch = true, reason = reason))
    }

    /**
     * "you untap one or more … during your untap step" — the detector fires it only for the active
     * player's untap-step untaps, so no `triggerRestriction` is needed (The Millennium Calendar).
     */
    fun becomeUntapped(): TriggerSpec {
        noOther("becomeUntapped")
        return spec(UntapEvent(filter = filter, batch = true))
    }

    /**
     * "are put into your graveyard [from your library]" — from anywhere by default. The matching
     * cards are the captured collection (Hedge Shredder's "put them onto the battlefield").
     */
    fun putIntoYourGraveyard(fromLibrary: Boolean = false): TriggerSpec {
        noOther("putIntoYourGraveyard")
        return spec(
            if (fromLibrary) CardsPutIntoGraveyardFromLibraryEvent(filter = filter)
            else CardsPutIntoYourGraveyardEvent(filter = filter)
        )
    }

    /**
     * "leave your graveyard" — however they leave. Pair with `triggerRestriction =
     * Conditions.IsYourTurn` for "during your turn".
     */
    fun leaveYourGraveyard(): TriggerSpec {
        noOther("leaveYourGraveyard")
        return spec(CardsLeftYourGraveyardEvent(filter = filter))
    }

    /**
     * "are put into exile from [from]". Not scoped to one player's zones unless the filter says so
     * (Kaya, Spirits' Justice). [includeTokens] is the *permanents* wording — see
     * [CardsPutIntoExileEvent.includeTokens].
     */
    fun putIntoExile(
        from: Set<Zone> = setOf(Zone.GRAVEYARD, Zone.BATTLEFIELD),
        includeTokens: Boolean = false,
    ): TriggerSpec {
        noOther("putIntoExile")
        return spec(CardsPutIntoExileEvent(fromZones = from, filter = filter, includeTokens = includeTokens))
    }
}

/**
 * A player as the subject of a trigger — see [Triggers.you], [Triggers.anOpponent],
 * [Triggers.anyPlayer], [Triggers.chosenOpponent]. Inside the effect, the acting player is
 * `Player.TriggeringPlayer`.
 */
class PlayerTriggerSubject internal constructor(private val player: Player) {

    private fun spec(event: EventPattern, binding: TriggerBinding = TriggerBinding.ANY) =
        TriggerSpec(event = event, binding = binding)

    private fun only(verb: String, vararg allowed: Player) {
        require(player in allowed) { "Triggers.<player>.$verb() supports ${allowed.joinToString()}, not $player" }
    }

    // ---- Turn structure ----------------------------------------------------------------------

    /**
     * "at the beginning of <player>'s [step]" — `Triggers.you.beginningOf(Step.UPKEEP)`,
     * `Triggers.anyPlayer.beginningOf(Step.END)` ("each end step"). [Step.BEGIN_COMBAT] is "the
     * beginning of combat", [Step.END_COMBAT] "end of combat" (CR 511.1).
     */
    fun beginningOf(step: Step): TriggerSpec = spec(StepEvent(step, player))

    // ---- Spells and abilities ----------------------------------------------------------------

    /**
     * "casts a [spell] spell". [requires] is a conjunctive set of [SpellCastPredicate]s — from a
     * zone, kicked, paid with Treasure mana, targets this / a matching object.
     */
    fun casts(
        spell: GameObjectFilter = GameObjectFilter.Any,
        requires: Set<SpellCastPredicate> = emptySet(),
    ): TriggerSpec = spec(SpellCastEvent(spellFilter = spell, player = player, requires = requires))

    /**
     * "casts their [n]th [spell] spell each turn" — counts casts, not resolutions (The Queen of Dale:
     * `anOpponent.castsNth(1, GameObjectFilter.Noncreature)`).
     */
    fun castsNth(n: Int, spell: GameObjectFilter? = null): TriggerSpec =
        spec(NthSpellCastEvent(nthSpell = n, player = player, spellFilter = spell))

    /**
     * "chooses one or more targets" — casting a spell, activating an ability, or putting a triggered
     * ability on the stack with a target. The triggering entity is that spell or ability (Psychic
     * Battle).
     */
    fun choosesTargets(): TriggerSpec = spec(TargetsChosenEvent(player = player))

    /**
     * "a spell or ability is put onto the stack" (Grip of Chaos) — by any player, so only
     * [Triggers.anyPlayer].
     */
    fun putsSpellOrAbilityOnStack(): TriggerSpec {
        only("putsSpellOrAbilityOnStack", Player.Each)
        return spec(SpellOrAbilityOnStackEvent)
    }

    /**
     * "activates an ability". Non-mana abilities only by default (CR 605 / 606) —
     * [includeManaAbilities] is the unqualified "an ability of a creature" wording (Elrond,
     * Moon-Reader), [excludeManaAbilities] the explicit exhaust "isn't a mana ability" one.
     * [of] scopes the ability's source permanent; [targeting] is "an ability that targets a creature
     * or player"; [loyalty] / [minLoyaltyRemoved] are loyalty abilities (CR 606.4, a −X counts X);
     * [exhaust] is an exhaust ability (CR 702.177); [withoutTapInCost] is the Antiquities "without {T}
     * in its activation cost" wording, which ignores the mana-ability split entirely.
     */
    fun activatesAbility(
        of: GameObjectFilter? = null,
        targeting: Recipient? = null,
        loyalty: Boolean = false,
        minLoyaltyRemoved: Int = 0,
        exhaust: Boolean = false,
        includeManaAbilities: Boolean = false,
        excludeManaAbilities: Boolean = false,
        withoutTapInCost: Boolean = false,
    ): TriggerSpec = spec(
        AbilityActivatedEvent(
            player = player,
            targetMatch = targeting,
            sourceFilter = of,
            requireNoTapInCost = withoutTapInCost,
            requireExhaust = exhaust,
            excludeManaAbilities = excludeManaAbilities,
            includeManaAbilities = includeManaAbilities,
            requireLoyalty = loyalty || minLoyaltyRemoved > 0,
            minLoyaltyRemoved = minLoyaltyRemoved,
        )
    )

    /**
     * "a creature you control attacking causes a triggered ability of that creature to trigger"
     * (Firebender Ascension) — the triggering ability is `EffectTarget.TriggeringEntity`.
     */
    fun attackTriggersAbility(): TriggerSpec = spec(AbilityTriggeredEvent(player = player, requireAttackCause = true))

    // ---- Combat ------------------------------------------------------------------------------

    /**
     * "attacks [with one or more [with]]" — you declare attackers, once per combat. Only
     * [Triggers.you]; a single creature attacking is [ObjectTriggerSubject.attacks].
     */
    fun attacks(with: GameObjectFilter? = null, minAttackers: Int = 1): TriggerSpec {
        only("attacks", Player.You)
        return spec(YouAttackEvent(minAttackers = minAttackers, attackerFilter = with))
    }

    /**
     * "is attacked" — "whenever one or more creatures attack you" ([Triggers.you]; with
     * [includePlaneswalkers], "you and/or planeswalkers you control") or "whenever one or more of
     * your opponents are attacked" ([Triggers.anOpponent]). Once per combat.
     */
    fun isAttacked(minAttackers: Int = 1, includePlaneswalkers: Boolean = false): TriggerSpec = when (player) {
        Player.You -> spec(CreaturesAttackYouEvent(minAttackers = minAttackers, includePlaneswalkersYouControl = includePlaneswalkers))
        Player.EachOpponent -> {
            require(!includePlaneswalkers) { "includePlaneswalkers is only supported for Triggers.you" }
            spec(CreaturesAttackYourOpponentEvent(minAttackers = minAttackers))
        }
        else -> throw IllegalArgumentException("Triggers.<player>.isAttacked() supports you and anOpponent, not $player")
    }

    /**
     * "is dealt damage [by a [by] source]" — [Triggers.you] only: any source, creature, burn spell
     * or artifact (Sun Droplet); the damage source is the triggering entity and the amount is
     * `TRIGGER_DAMAGE_AMOUNT`.
     */
    fun isDealtDamage(by: GameObjectFilter? = null, damageType: DamageType = DamageType.Any): TriggerSpec {
        only("isDealtDamage", Player.You)
        return spec(DealsDamageEvent(damageType = damageType, recipient = Recipient.You, sourceFilter = by))
    }

    /**
     * "one or more of your opponents are dealt combat damage", by any source — once per
     * combat-damage batch (Fblthp, Impossibly Lost). [Triggers.anOpponent] only; "one or more
     * creatures deal combat damage to you" is [BatchTriggerSubject.dealCombatDamageToYou].
     */
    fun isDealtCombatDamage(): TriggerSpec {
        only("isDealtCombatDamage", Player.EachOpponent)
        return spec(OpponentsDealtCombatDamageEvent)
    }

    // ---- Cards and lands ---------------------------------------------------------------------

    /**
     * "draws a card" — once per card (CR 121.2). [exceptFirstInDrawStep] skips the turn-based draw
     * (CR 504.1, Orcish Bowmasters).
     */
    fun draws(exceptFirstInDrawStep: Boolean = false): TriggerSpec =
        spec(DrawEvent(player, exceptFirstInDrawStep = exceptFirstInDrawStep))

    /** "draws their [n]th card each turn" — once, even when one multi-card draw crosses it (CR 121.5: no "draw", no count). */
    fun drawsNth(n: Int): TriggerSpec = spec(NthCardDrawnEvent(nthCard = n, player = player))

    /** "reveals a [card] card from the first draw of a turn" — with `RevealFirstDrawEachTurn` (Primitive Etchings). */
    fun revealsFirstDraw(card: GameObjectFilter? = null): TriggerSpec {
        only("revealsFirstDraw", Player.You)
        return spec(CardRevealedFromDrawEvent(cardFilter = card))
    }

    /**
     * "discards a [card] card" — once per card; [batch] is "one or more cards", once per discard
     * event (CR 603.2c). Your own card discarding itself is [ObjectTriggerSubject.isDiscarded].
     */
    fun discards(card: GameObjectFilter? = null, batch: Boolean = false): TriggerSpec =
        spec(DiscardEvent(player = player, cardFilter = card, batch = batch))

    /** "cycles a card". This card being cycled is [ObjectTriggerSubject.isCycled]. */
    fun cycles(): TriggerSpec = spec(CycleEvent(player))

    /**
     * "plays a land [from a zone other than [fromZoneOtherThan]]" (CR 305.1) — the land-play action,
     * not a land an effect puts onto the battlefield.
     */
    fun playsLand(fromZoneOtherThan: Zone? = null): TriggerSpec =
        spec(LandPlayedEvent(fromZoneOtherThan = fromZoneOtherThan, player = player))

    /** "is turned face up" for "a [filter] you control" — matched on its face-up characteristics (Perimeter Enforcer). */
    fun permanentTurnedFaceUp(filter: GameObjectFilter = GameObjectFilter.Any): TriggerSpec =
        spec(CreatureTurnedFaceUpEvent(player, filter))

    /** "searches their library" (CR 701.23) — fires even if nothing was found. */
    fun searchesLibrary(): TriggerSpec = spec(SearchLibraryEvent(player))

    /**
     * "a spell or ability causes <player> to shuffle their library" (CR 701.24) — never the game-rule
     * shuffles of setup (CR 103.2) and mulligans (CR 103.5).
     */
    fun shufflesLibrary(): TriggerSpec = spec(ShuffleLibraryEvent(player))

    // ---- Life and the game -------------------------------------------------------------------

    /** "gains life [for the first time each turn]". */
    fun gainsLife(firstTimeEachTurn: Boolean = false): TriggerSpec =
        spec(LifeGainEvent(player, firstTimeEachTurn = firstTimeEachTurn))

    /** "loses life" — the amount is `TRIGGER_LIFE_LOST`. */
    fun losesLife(): TriggerSpec = spec(LifeLossEvent(player))

    /** "loses the game" (CR 104.3). */
    fun losesGame(): TriggerSpec = spec(PlayerLostGameEvent(player))

    // ---- Permanents the player acts on -------------------------------------------------------

    /**
     * "sacrifices a [filter]" — once per permanent, the source included (CR 603.2c); [batch] is
     * "sacrifices one or more", once per batch. "Another" is [sacrificesAnother].
     */
    fun sacrifices(filter: GameObjectFilter = GameObjectFilter.Any, batch: Boolean = false): TriggerSpec =
        spec(PermanentsSacrificedEvent(filter = filter, sacrificedBy = player, perPermanent = !batch))

    /** "sacrifices another [filter]" — once per permanent; the source sacrificing itself doesn't count. */
    fun sacrificesAnother(filter: GameObjectFilter = GameObjectFilter.Any): TriggerSpec =
        spec(PermanentsSacrificedEvent(filter = filter, sacrificedBy = player, perPermanent = true), TriggerBinding.OTHER)

    /**
     * "taps an untapped [filter]" — a tap the player *caused* (an effect instructed them to tap),
     * never an opponent tapping their own creature; [batch] is "one or more".
     */
    fun taps(filter: GameObjectFilter, batch: Boolean = false): TriggerSpec =
        spec(TapEvent(filter = filter, batch = batch, tapper = player))

    /**
     * "taps a [land] for mana". **Not wired in the engine yet** — a mana-adding version (Mana Flare)
     * is a triggered mana ability; author it as `AdditionalManaOnSourceTap`.
     */
    fun tapsLandForMana(land: GameObjectFilter? = null): TriggerSpec =
        spec(LandTappedForMana(player = player, landFilter = land))

    /** "creates a [token] token". */
    fun createsToken(token: GameObjectFilter? = null): TriggerSpec =
        spec(TokenCreationEvent(controller = player, tokenFilter = token))

    /** "exploits a creature" — [nontoken] is "exploits a nontoken creature". */
    fun exploits(nontoken: Boolean = false): TriggerSpec =
        spec(ExploitedEvent(player = player, requireNontokenExploited = nontoken))

    // ---- Keyword actions ---------------------------------------------------------------------

    /** "commits a crime". */
    fun commitsCrime(): TriggerSpec = spec(CommitCrimeEvent(player))

    /** "gives a gift". */
    fun givesAGift(): TriggerSpec = spec(GiftGivenEvent(player))

    /** "scries" — `TRIGGER_SCRY_COUNT` is the number of cards looked at. */
    fun scries(): TriggerSpec = spec(ScriedEvent(player))

    /** "surveils" (CR 701.25). */
    fun surveils(): TriggerSpec = spec(SurveiledEvent(player))

    /** "scries or surveils" (Matoya, Archon Elder). */
    fun scriesOrSurveils(): TriggerSpec = spec(ScriedOrSurveiledEvent(player))

    /** "discovers" (CR 701.57) — `TRIGGER_DISCOVER_VALUE` is the discover value. */
    fun discovers(): TriggerSpec = spec(DiscoveredEvent(player))

    /** "collects evidence" — never for a declined or impossible collection. */
    fun collectsEvidence(): TriggerSpec = spec(EvidenceCollectedEvent(player))

    /** "forages" (CR 701.59a) — never for a declined forage. */
    fun forages(): TriggerSpec = spec(ForagedEvent(player))

    /** "solves a Case" (CR 719.3a) — once per Case, ever. */
    fun solvesACase(): TriggerSpec = spec(CaseSolvedEvent(player))

    /**
     * "clashes [and wins]" (CR 701.30) — also for a clash an opponent started. A payoff that only
     * *differs* on a win branches in its effect instead.
     */
    fun clashes(andWins: Boolean = false): TriggerSpec = spec(ClashedEvent(player, requireWin = andWins))

    /** "the Ring tempts <player>" (CR 701.54d); [bearerChosen] is "chooses a creature as your Ring-bearer". */
    fun isTemptedByTheRing(bearerChosen: Boolean = false): TriggerSpec =
        spec(RingTemptedEvent(player, requireBearerChosen = bearerChosen))

    /** "waterbends, earthbends, firebends, or airbends" — once per bend whose type is in [types]. */
    fun bends(types: Set<BendType> = BendType.ALL): TriggerSpec = spec(BendPerformedEvent(player = player, types = types))

    /** "manifests dread" (CR 701.60) — the milled card is the captured collection. */
    fun manifestsDread(): TriggerSpec = spec(ManifestedDreadEvent(player))

    /** "expends [threshold]" — spends their [threshold]th total mana on spells this turn; once per turn. */
    fun expends(threshold: Int): TriggerSpec = spec(ExpendEvent(threshold, player))

    /** "fully unlocks a Room" (Eerie, DSK). */
    fun fullyUnlocksARoom(): TriggerSpec = spec(RoomFullyUnlockedEvent(player))

    /** "a chapter ability of a Saga <player> controls resolves"; [finalOnly] is the final chapter (Tom Bombadil). */
    fun sagaChapterResolves(finalOnly: Boolean = false): TriggerSpec =
        spec(SagaChapterResolvedEvent(player, finalChapterOnly = finalOnly))
}
