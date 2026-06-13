package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CopyCardIntoCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Paradigm (Secrets of Strixhaven) as a composable, content-agnostic primitive.
 *
 * Paradigm is an **exile-zone** recurrence mechanic on Lesson spells. The printed text reads:
 * "[main effect] Then exile this spell. After you first resolve a spell with this name, you may
 * cast a copy of it from exile without paying its mana cost at the beginning of each of your first
 * main phases."
 *
 * Like Suspend (CR 702.62), it lives entirely off a **marker component** rather than a printed
 * ability, so the very same machinery would work for any card a future effect "grants paradigm" to.
 * Two halves make up the mechanic:
 *  - **Entering exile** — a Paradigm spell carries `CardScript.paradigm`. When it resolves it is
 *    routed to exile instead of the graveyard (reusing the existing self-exile-on-resolve path) and
 *    tagged with the paradigm marker as it lands. The marker is what the engine keys on.
 *  - **Recurring free recast** — [recastAbility] below, the single triggered ability the engine
 *    synthesizes for every exiled card carrying the paradigm marker. It fires at the beginning of
 *    the owner's precombat (first) main phase — the trigger detector already scans exile for
 *    `activeZone == EXILE` triggers and treats exiled cards as owner-controlled — and lets the owner
 *    cast a **copy** of the exiled card for free through the ordinary
 *    [CopyCardIntoCollectionEffect] → [CastFromCollectionWithoutPayingCostEffect] pipeline (which
 *    handles target / X selection). The original never leaves exile, so the trigger recurs every
 *    turn; each cast copy is a phantom (CR 707.10a) that ceases to exist when it leaves the stack.
 *
 * Because it casts a *copy* — not the card itself — Paradigm differs from Suspend (which plays the
 * exiled card directly and so consumes it). That single difference is the [CopyCardIntoCollectionEffect]
 * step; everything else mirrors the proven Suspend wiring.
 */
object Paradigm {

    /** Pipeline collection key the recast uses to hand the fresh copy to the free-cast step. */
    const val COPY_COLLECTION: String = "paradigm_copy"

    /**
     * The synthesized triggered ability granted (by the engine) to any exiled card that carries
     * the paradigm marker. Functions only in exile (`activeZone == EXILE`), so it is inert anywhere
     * else and harmless to return universally.
     */
    val recastAbility: TriggeredAbility = TriggeredAbility(
        id = AbilityId("paradigm_recast"),
        // "your first main phase" = the precombat main phase (CR 505 — the first main phase).
        trigger = EventPattern.StepEvent(Step.PRECOMBAT_MAIN, Player.You),
        binding = TriggerBinding.SELF,
        activeZone = Zone.EXILE,
        effect = MayEffect(
            CompositeEffect(
                listOf(
                    CopyCardIntoCollectionEffect(source = EffectTarget.Self, storeAs = COPY_COLLECTION),
                    CastFromCollectionWithoutPayingCostEffect(from = COPY_COLLECTION),
                )
            ),
            descriptionOverride = "cast a copy of it without paying its mana cost",
        ),
        descriptionOverride = "At the beginning of each of your first main phases, you may cast a " +
            "copy of this card from exile without paying its mana cost.",
    )
}
