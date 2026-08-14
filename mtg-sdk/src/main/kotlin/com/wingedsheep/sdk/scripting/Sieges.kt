package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect

/**
 * The Siege defeat trigger (CR 310.11b) as pure data — the intrinsic ability *every* Siege has,
 * printed on none of them.
 *
 * A Siege's card text is only its reminder line ("As a Siege enters, choose an opponent to protect
 * it…"); the actual ability comes from the rules, exactly like the suspend countdown
 * ([Suspend.countdownAbility]). So it lives here as one synthesized [TriggeredAbility] that the
 * engine grants to any Siege on the battlefield, rather than being repeated on every battle card —
 * a new Siege is then a plain `cardDef` with a `startingDefense` and a back face.
 *
 * The ability is a three-step pipeline over primitives that already existed:
 *
 *  1. **[CardSource.Self] → collection.** The battle feeds itself into the pipeline.
 *  2. **Move that collection to exile.** Mandatory — "exile it" has no "may" (CR 310.11b), and the
 *     exile is what makes the *cast* legal: the card must be in exile for the free-cast grant to
 *     have a zone to cast from. [MoveCollectionEffect.storeMovedAs] republishes the ids that
 *     actually moved, so a battle that somehow left the battlefield first casts nothing.
 *  3. **May-cast it transformed for free.** [CastFromCollectionWithoutPayingCostEffect] casts
 *     during this ability's resolution (CR 608.2 — timing restrictions based on card type are
 *     ignored, and a card left uncast simply stays in exile), and `castTransformed` puts the back
 *     face on the stack (CR 712.8c) the same way disturb does.
 *
 * The window this depends on is CR 704.5v's carve-out: a battle with defense 0 is *not* put into
 * its owner's graveyard while it is the source of a trigger still on the stack, which is precisely
 * how the battle survives long enough for step 2 to exile it.
 */
object Sieges {

    /** Pipeline collection key the defeat trigger uses to hand the battle to the exile step. */
    const val DEFEAT_COLLECTION: String = "siege_defeat"

    /** Pipeline collection key holding the ids that actually reached exile. */
    const val DEFEAT_EXILED_COLLECTION: String = "siege_defeat_exiled"

    /**
     * The synthesized triggered ability every Siege on the battlefield has (CR 310.11b).
     *
     * [TriggerBinding.SELF] scopes it to defense counters leaving *this* permanent, and
     * `lastRemoved` fires it only for the removal that empties them — chipping a Siege from 5
     * defense to 2 is silent, and the hit that takes it to 0 defeats it exactly once.
     */
    val defeatAbility: TriggeredAbility = TriggeredAbility(
        id = AbilityId("siege_defeat"),
        trigger = EventPattern.CountersRemovedEvent(
            counterType = Counters.DEFENSE,
            lastRemoved = true,
        ),
        binding = TriggerBinding.SELF,
        activeZones = setOf(Zone.BATTLEFIELD),
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(CardSource.Self, storeAs = DEFEAT_COLLECTION),
                MoveCollectionEffect(
                    from = DEFEAT_COLLECTION,
                    destination = CardDestination.ToZone(Zone.EXILE),
                    storeMovedAs = DEFEAT_EXILED_COLLECTION,
                ),
                MayEffect(
                    CastFromCollectionWithoutPayingCostEffect(
                        from = DEFEAT_EXILED_COLLECTION,
                        castTransformed = true,
                    ),
                    descriptionOverride = "cast it transformed without paying its mana cost",
                ),
            )
        ),
        descriptionOverride = "When the last defense counter is removed from this permanent, " +
            "exile it, then you may cast it transformed without paying its mana cost.",
    )
}
