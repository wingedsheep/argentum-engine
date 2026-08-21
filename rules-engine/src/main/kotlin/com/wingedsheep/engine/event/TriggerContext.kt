package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.AbilityTriggeredEvent
import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.BecomesTargetEvent
import com.wingedsheep.engine.core.BlockersDeclaredEvent
import com.wingedsheep.engine.core.CardCycledEvent
import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.core.UntappedEvent
import com.wingedsheep.engine.core.PhasedInEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId

/**
 * Context information about what caused a trigger.
 */
@kotlinx.serialization.Serializable
data class TriggerContext(
    val triggeringEntityId: EntityId? = null,
    val triggeringPlayerId: EntityId? = null,
    val damageAmount: Int? = null,
    val step: Step? = null,
    val xValue: Int? = null,
    /** Last known +1/+1 counter count when the source left the battlefield */
    val counterCount: Int? = null,
    /** Last known total counter count (all types) when the source left the battlefield */
    val totalCounterCount: Int? = null,
    /** Last known -1/-1 counter count when the source left the battlefield */
    val minusOneMinusOneCounterCount: Int? = null,
    /** The spell or ability entity that targeted a permanent (for ward triggers) */
    val targetingSourceEntityId: EntityId? = null,
    /** Last known power when the triggering entity left the battlefield (for dies/leaves triggers) */
    val lastKnownPower: Int? = null,
    /** Last known toughness when the triggering entity left the battlefield (for dies/leaves triggers) */
    val lastKnownToughness: Int? = null,
    /**
     * Total last-known power of the creatures that died in the batch that fired a
     * `CreaturesYouControlDiedEvent` trigger (CR 603.2c / 603.10) — summed over the deaths that
     * match the trigger's filter, captured at detection time. Read via
     * [com.wingedsheep.sdk.scripting.values.ContextPropertyKey.DIED_BATCH_TOTAL_POWER] by "the
     * total power of those creatures" batch payoffs (The Skullspore Nexus). Null for non-batch
     * triggers. Populated by [TriggerDetector], not [fromEvent].
     */
    val diedBatchTotalPower: Int? = null,
    /**
     * Last-known **projected** subtypes when the triggering entity left the battlefield (CR 603.10),
     * so continuous-effect-granted types count and not just printed ones. Read by
     * [com.wingedsheep.sdk.scripting.conditions.TriggeringEntityHadSubtype] as an intervening-if on
     * dies/leaves triggers (Infernal Vessel's "if it wasn't a Demon" self-recursion guard). Null
     * when the trigger's source never left the battlefield.
     */
    val lastKnownSubtypes: Set<String>? = null,
    /**
     * Last-known **projected** card types when the triggering entity left the battlefield
     * (CR 603.10), so a type set by a continuous effect counts and not just the printed one. Read by
     * [com.wingedsheep.sdk.scripting.conditions.TriggeringEntityHadCardType] as an intervening-if on
     * dies/leaves triggers (Tom, Bert, and William's "if they were a creature" self-recursion
     * guard — the second death is of the artifact they came back as). The card-type sibling of
     * [lastKnownSubtypes]. Null when the trigger's source never left the battlefield.
     */
    val lastKnownCardTypes: Set<String>? = null,
    /**
     * Last-known counter map (counter-type-string → count) when the triggering source left
     * the battlefield. Used by triggers that move every counter onto another permanent
     * (e.g., Essence Channeler's "put its counters on target creature you control").
     * Null when the trigger's source never left the battlefield (or had no counters).
     */
    val lastKnownCounters: Map<String, Int>? = null,
    /**
     * Per-player damage dealt to the triggering source this turn, captured at LTB time.
     * Read by LTB effects like Grothama's "each player draws X cards where X is the damage
     * dealt to ~ this turn by sources they controlled."
     */
    val lastKnownDamageDealtByPlayers: Map<EntityId, Int>? = null,
    /**
     * Creatures that were blocking, or blocked by, the triggering source when it left the
     * battlefield (CR 509 combat pairing), captured as last-known information. Read by
     * "destroy all creatures blocking or blocked by it" (Abu Ja'far). Null when the trigger's
     * source never left combat.
     */
    val lastKnownBlockingOrBlockedByIds: List<EntityId>? = null,
    /**
     * For SpellCastEvent triggers — number of mode picks the cast spell recorded. `null`
     * when the trigger was not driven by a spell cast. Read by
     * `ContextPropertyKey.MODES_CHOSEN_ON_TRIGGERING_SPELL` so abilities like Riku of
     * Many Paths can scale by "the number of times you chose a mode for that spell."
     */
    val modesChosenCount: Int? = null,
    /**
     * For SpellCastEvent triggers — total mana spent to cast the triggering spell. `null` when
     * the trigger was not driven by a spell cast. Read by
     * `ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL` so abilities like Aberrant Manawurm
     * and Expressive Firedancer can scale by "the amount of mana spent to cast that spell."
     */
    val manaSpentOnTriggeringSpell: Int? = null,
    /**
     * For SpellCastEvent triggers — number of distinct colors of mana spent to cast the
     * triggering spell (0–5). `null` when the trigger was not driven by a spell cast. Read by
     * `ContextPropertyKey.COLORS_SPENT_ON_TRIGGERING_SPELL` so abilities like Magmablood Archaic
     * can scale by "for each color of mana spent to cast that spell."
     */
    val colorsSpentOnTriggeringSpell: Int? = null,
    /**
     * For SpellCastEvent triggers — mana value (CR 202.3) of the triggering spell. `null` when
     * the trigger was not driven by a spell cast. Read by
     * `ContextPropertyKey.TRIGGERING_SPELL_MANA_VALUE` so abilities like Kellan, the Kid can
     * gate "a permanent spell with equal or lesser mana value."
     */
    val manaValueOfTriggeringSpell: Int? = null,
    /**
     * For SpellCastEvent triggers — the value chosen for `{X}` on the triggering spell (CR 601.2b).
     * `null` when the trigger was not driven by a spell cast or the spell had no {X}. Read by
     * `ContextPropertyKey.X_VALUE_OF_TRIGGERING_SPELL` so abilities like Geometer's Arthropod can
     * scale by "the top X cards of your library."
     */
    val xValueOfTriggeringSpell: Int? = null,
    /**
     * Power of the creature the trigger's source (an Aura/Equipment) was attached to, captured
     * when the trigger fired. Carried as last-known information (CR 608.2h) so that an
     * "enchanted creature deals damage equal to its power" ability still uses the right power
     * if the creature — and the aura — leave before the ability resolves. Null for non-attached
     * sources. Populated by [TriggerDetector], not [fromEvent] (which has no game state).
     */
    val enchantedCreatureLastKnownPower: Int? = null,
    /**
     * Number of cards actually looked at by the scry that caused this trigger to fire. Read
     * by `ContextPropertyKey.TRIGGER_SCRY_COUNT` so "Whenever you scry, ... for each card
     * looked at" payoffs (Celeborn the Wise, Elrond Master of Healing) scale correctly.
     * `null` when the trigger was not driven by a scry.
     */
    val scryCount: Int? = null,
    /**
     * Number of cards discarded in the batch that caused this trigger to fire (CR 603.2c). Read
     * by `ContextPropertyKey.TRIGGER_DISCARD_COUNT` so "Whenever you discard one or more cards,
     * ... that much" payoffs (Magmakin Artillerist) scale with the batch. `null` when the trigger
     * was not driven by a discard.
     */
    val discardedCardCount: Int? = null,
    /**
     * The discover value N (mana-value threshold) of the discover that fired this trigger (CR
     * 701.57). Read by `ContextPropertyKey.TRIGGER_DISCOVER_VALUE` so "discover again for the same
     * value" payoffs (Curator of Sun's Creation) reuse it. `null` when the trigger was not driven
     * by a discover.
     */
    val discoverValue: Int? = null,
    /**
     * Damage past lethal dealt to the trigger's creature recipient (CR 120.4a). Captured
     * from `DamageDealtEvent.excessAmount` so payoffs like Fall of Cair Andros — "amass
     * Orcs X, where X is the excess damage" — can read it via
     * `ContextPropertyKey.TRIGGER_EXCESS_DAMAGE_AMOUNT`. `null` for non-damage triggers.
     */
    val excessDamageAmount: Int? = null,
    /**
     * The damage recipient creature's toughness at the instant the triggering damage was dealt
     * (CR 603.10 last-known information). Carried from [DamageDealtEvent.targetToughnessAtDamage]
     * so "damage equal to that creature's toughness" payoffs (Taii Wakeen, Perfect Shot) can read
     * it via `ContextPropertyKey.TRIGGER_RECIPIENT_TOUGHNESS` even after the creature died from the
     * same damage. `null` for non-creature recipients.
     */
    val recipientToughnessAtDamage: Int? = null,
    /**
     * The entities a batch trigger captured as "the ones that caused it to fire" — e.g. every
     * matching permanent in a [com.wingedsheep.sdk.scripting.EventPattern.PermanentsEnteredEvent]
     * batch. Seeded into the resolving ability's pipeline under
     * [com.wingedsheep.engine.handlers.PipelineState.TRIGGER_CAPTURED_COLLECTION] so a
     * `ForEachInCollectionEffect` payoff ("for each of them, create a tapped copy of it" —
     * Kambal, Profiteering Mayor) can iterate them. `null` / empty for triggers that capture a
     * single entity via [triggeringEntityId] instead.
     */
    val capturedEntityIds: List<EntityId>? = null,
    /**
     * For [com.wingedsheep.engine.core.PermanentAttachedEvent] /
     * [com.wingedsheep.engine.core.PermanentUnattachedEvent] triggers — the permanent the triggering
     * attachment (Aura/Equipment) became attached to, or came off of. Resolved by
     * [com.wingedsheep.sdk.scripting.targets.EffectTarget.AttachedToTriggeringPermanent] so a
     * "becomes (un)attached" payoff can act on the host (Eriette gains control of it; Assimilation
     * Aegis makes it a copy; Stitcher's Graft sacrifices it). `null` for non-attachment triggers.
     */
    val attachedToEntityId: EntityId? = null,
    /**
     * For [com.wingedsheep.engine.core.PermanentUnattachedEvent] triggers only — the host the
     * attachment came *off*. Deliberately separate from [attachedToEntityId]: an unattach payoff
     * must not read the live `AttachedToComponent`, because by resolution it is either gone or
     * already re-pointed at a different host (equipping the Graft away from a creature attaches it
     * elsewhere in the same action). Resolves
     * [com.wingedsheep.sdk.scripting.targets.EffectTarget.AttachedToTriggeringPermanent] in that
     * case, and leaves the attach case on its live read (CR 611.2b). `null` otherwise.
     */
    val unattachedFromEntityId: EntityId? = null
) {
    companion object {
        fun fromEvent(event: com.wingedsheep.engine.core.GameEvent): TriggerContext {
            return when (event) {
                is ZoneChangeEvent -> TriggerContext(
                    triggeringEntityId = event.entityId,
                    // The player associated with a zone change is the object's controller as it
                    // changed zones — its last-known controller when leaving the battlefield (CR
                    // 603.10/608.2h last-known information; differs from the owner for stolen
                    // permanents), falling back to the owner for non-battlefield origins. This is
                    // what "that creature's controller" / "they" mean in a dies/leaves trigger, so
                    // Player.TriggeringPlayer resolves to the dying creature's controller rather than
                    // (previously) falling through to the dead creature's entity id.
                    triggeringPlayerId = event.lastKnown?.controllerId ?: event.ownerId,
                    counterCount = event.lastKnown?.plusOnePlusOneCounters?.takeIf { it > 0 },
                    totalCounterCount = event.lastKnown?.totalCounters?.takeIf { it > 0 },
                    minusOneMinusOneCounterCount = event.lastKnown?.minusOneMinusOneCounters?.takeIf { it > 0 },
                    xValue = event.xValue,
                    lastKnownPower = event.lastKnown?.power,
                    lastKnownToughness = event.lastKnown?.toughness,
                    lastKnownSubtypes = event.lastKnown?.subtypes?.takeIf { it.isNotEmpty() },
                    lastKnownCardTypes = event.lastKnown?.typeLine?.cardTypes
                        ?.mapTo(mutableSetOf()) { it.name }?.takeIf { it.isNotEmpty() },
                    lastKnownCounters = event.lastKnown?.counters?.takeIf { it.isNotEmpty() },
                    lastKnownDamageDealtByPlayers =
                        event.lastKnown?.damageDealtByPlayers?.takeIf { it.isNotEmpty() },
                    lastKnownBlockingOrBlockedByIds =
                        event.lastKnown?.blockingOrBlockedByIds?.takeIf { it.isNotEmpty() }
                )
                is DamageDealtEvent -> TriggerContext(
                    triggeringEntityId = event.targetId,
                    damageAmount = event.amount,
                    excessDamageAmount = event.excessAmount.takeIf { it > 0 },
                    recipientToughnessAtDamage = event.targetToughnessAtDamage
                )
                is com.wingedsheep.engine.core.DamagePreventedEvent -> TriggerContext(
                    // The prevented source — so "deal that much to that source's controller" resolves
                    // via EffectTarget.ControllerOfTriggeringEntity, and damageAmount feeds PREVENTED_DAMAGE_AMOUNT.
                    triggeringEntityId = event.sourceId,
                    damageAmount = event.amount
                )
                is com.wingedsheep.engine.core.CardPlayedFromPermissionEvent -> TriggerContext(
                    // The card played this way; the player who played it. The rider's source
                    // (e.g. Fires of Mount Doom) is carried separately on the delayed trigger.
                    triggeringEntityId = event.cardId,
                    triggeringPlayerId = event.controllerId
                )
                is com.wingedsheep.engine.core.CountersAddedEvent -> TriggerContext(
                    triggeringEntityId = event.entityId,
                    counterCount = event.amount
                )
                // The permanent the counters left, and how many left it — the mirror of the
                // placement context, so a "counters removed" payoff can read "that permanent" and
                // "that many".
                is com.wingedsheep.engine.core.CountersRemovedEvent -> TriggerContext(
                    triggeringEntityId = event.entityId,
                    counterCount = event.amount
                )
                is SpellCastEvent -> TriggerContext(
                    triggeringEntityId = event.spellEntityId,
                    triggeringPlayerId = event.casterId,
                    modesChosenCount = event.chosenModesCount.takeIf { it > 0 },
                    manaSpentOnTriggeringSpell = event.totalManaSpent.takeIf { it > 0 },
                    colorsSpentOnTriggeringSpell = event.distinctColorsSpent.takeIf { it > 0 },
                    manaValueOfTriggeringSpell = event.manaValue.takeIf { it > 0 },
                    xValueOfTriggeringSpell = event.xValue
                )
                is CardsDrawnEvent -> TriggerContext(triggeringPlayerId = event.playerId)
                // A player losing the game: the loser is both the triggering player (so
                // Player.TriggeringPlayer / a TriggeringPlayerIs condition resolves to them) and
                // the triggering entity (players are entities). Shinryu, Transcendent Rival reads
                // this to gate "when the chosen player loses the game, you win the game".
                is com.wingedsheep.engine.core.PlayerLostEvent -> TriggerContext(
                    triggeringEntityId = event.playerId,
                    triggeringPlayerId = event.playerId
                )
                is com.wingedsheep.engine.core.ScriedEvent -> TriggerContext(
                    triggeringPlayerId = event.playerId,
                    scryCount = event.count
                )
                // Surveil reuses the "cards looked at" count slot (TRIGGER_SCRY_COUNT) — the
                // field is the number of cards looked at, common to scry and surveil.
                is com.wingedsheep.engine.core.SurveiledEvent -> TriggerContext(
                    triggeringPlayerId = event.playerId,
                    scryCount = event.count
                )
                // Discover (CR 701.57): carries the discover value N so "discover again for the
                // same value" (Curator of Sun's Creation) can reuse it via TRIGGER_DISCOVER_VALUE.
                is com.wingedsheep.engine.core.DiscoveredEvent -> TriggerContext(
                    triggeringPlayerId = event.playerId,
                    discoverValue = event.value
                )
                // Collect evidence (CR 701.59): the collecting player is the triggering player, so
                // "whenever you collect evidence" resolves "you" correctly for an opponent's
                // collection against a ward cost.
                is com.wingedsheep.engine.core.EvidenceCollectedEvent -> TriggerContext(
                    triggeringPlayerId = event.playerId
                )
                // Solve a Case (CR 719.3a): the solving player is the triggering player, and the
                // solved Case itself is the triggering entity — so a payoff can name either.
                is com.wingedsheep.engine.core.CaseSolvedEvent -> TriggerContext(
                    triggeringEntityId = event.entityId,
                    triggeringPlayerId = event.controllerId
                )
                // Manifest dread (CR 701.60): the cards put into the graveyard this way are
                // carried as capturedEntityIds, seeded into the resolving trigger's pipeline under
                // TRIGGER_CAPTURED_COLLECTION so "a card you put into your graveyard this way"
                // payoffs (Paranormal Analyst) can move it out. Empty when the library held fewer
                // than two cards (the trigger still fires per CR 701.60b).
                is com.wingedsheep.engine.core.ManifestedDreadEvent -> TriggerContext(
                    triggeringPlayerId = event.playerId,
                    capturedEntityIds = event.graveyardCardIds.takeIf { it.isNotEmpty() }
                )
                // The batch size feeds TRIGGER_DISCARD_COUNT ("that much") — one event per
                // discard, however many cards it contained (CR 603.2c).
                is CardsDiscardedEvent -> TriggerContext(
                    triggeringPlayerId = event.playerId,
                    discardedCardCount = event.cardIds.size
                )
                is CardRevealedFromDrawEvent -> TriggerContext(
                    triggeringEntityId = event.cardEntityId,
                    triggeringPlayerId = event.playerId
                )
                // xValue carries the X announced for an `{X}` cycling cost (CR 107.3a) so a
                // "when you cycle this card" trigger can read it as DynamicAmount.XValue —
                // Webstrike Elite's "with mana value X", Valor's Flagship's "create X tokens".
                is CardCycledEvent -> TriggerContext(
                    triggeringPlayerId = event.playerId,
                    xValue = event.xValue
                )
                is com.wingedsheep.engine.core.CrewOrSaddleContributionEvent -> TriggerContext(
                    triggeringEntityId = event.permanentId,
                    triggeringPlayerId = event.controllerId
                )
                // The attacking player is the triggering player, so "that opponent loses 3 life"
                // (Tomik, Wielder of Law) resolves off a declare-attackers trigger. Before this the
                // context was empty and `Player.TriggeringPlayer` silently evaluated to null here.
                is AttackersDeclaredEvent -> TriggerContext(
                    triggeringPlayerId = event.attackingPlayerId
                )
                is BlockersDeclaredEvent -> TriggerContext()
                is TappedEvent -> TriggerContext(triggeringEntityId = event.entityId)
                is UntappedEvent -> TriggerContext(triggeringEntityId = event.entityId)
                is PhasedInEvent -> TriggerContext(triggeringEntityId = event.entityId)
                is LifeChangedEvent -> TriggerContext(
                    triggeringEntityId = event.playerId,
                    triggeringPlayerId = event.playerId,
                    damageAmount = when {
                        event.reason == com.wingedsheep.engine.core.LifeChangeReason.LIFE_GAIN ->
                            event.newLife - event.oldLife
                        event.oldLife > event.newLife ->
                            event.oldLife - event.newLife
                        else -> null
                    }
                )
                is TurnFaceUpEvent -> TriggerContext(
                    triggeringEntityId = event.entityId,
                    triggeringPlayerId = event.controllerId,
                    xValue = event.xValue
                )
                is com.wingedsheep.engine.core.TransformedEvent -> TriggerContext(
                    triggeringEntityId = event.entityId,
                    triggeringPlayerId = event.controllerId
                )
                is ControlChangedEvent -> TriggerContext(
                    triggeringEntityId = event.permanentId,
                    triggeringPlayerId = event.newControllerId
                )
                is com.wingedsheep.engine.core.PermanentAttachedEvent -> TriggerContext(
                    // The attachment is the triggering entity; the host it attached to is carried
                    // for EffectTarget.AttachedToTriggeringPermanent.
                    triggeringEntityId = event.attachmentId,
                    triggeringPlayerId = event.controllerId,
                    attachedToEntityId = event.attachedToId
                )
                is com.wingedsheep.engine.core.PermanentUnattachedEvent -> TriggerContext(
                    // Mirror of the attach case: the attachment triggers, and the host it came off
                    // rides along as "that permanent" (Stitcher's Graft sacrifices it).
                    triggeringEntityId = event.attachmentId,
                    triggeringPlayerId = event.controllerId,
                    unattachedFromEntityId = event.attachedToId
                )
                is BecomesTargetEvent -> TriggerContext(
                    triggeringEntityId = event.targetEntityId,
                    // A targeted *player* is the triggering player as well, so "that player" is
                    // reachable from a player-target trigger (Loki, God of Mischief). Left null for
                    // object targets, exactly as before.
                    triggeringPlayerId = event.targetEntityId.takeIf { event.targetIsPlayer },
                    targetingSourceEntityId = event.sourceEntityId
                )
                is com.wingedsheep.engine.core.LibraryShuffledEvent -> TriggerContext(
                    // "…deals 2 damage to that player" — the shuffler is the triggering player.
                    triggeringPlayerId = event.playerId
                )
                is com.wingedsheep.engine.core.TargetsChosenEvent -> TriggerContext(
                    triggeringEntityId = event.stackObjectId,
                    triggeringPlayerId = event.chooserId
                )
                is AbilityActivatedEvent -> TriggerContext(
                    // The ability's stack entity — copy effects target it (Ertha Jo's
                    // CopyTargetSpellOrAbility(TriggeringEntity)). "That artifact's controller"
                    // reads resolve THROUGH it: ControllerOfTriggeringEntity falls through the
                    // ActivatedAbilityOnStackComponent to the source permanent's controller.
                    // A non-{T} mana ability never reaches the stack (abilityEntityId == null),
                    // so fall back to the source permanent directly (Haunting Wind).
                    triggeringEntityId = event.abilityEntityId ?: event.sourceId,
                    triggeringPlayerId = event.controllerId
                )
                is AbilityTriggeredEvent -> TriggerContext(
                    triggeringEntityId = event.abilityEntityId,
                    triggeringPlayerId = event.controllerId
                )
                else -> TriggerContext()
            }
        }
    }
}
