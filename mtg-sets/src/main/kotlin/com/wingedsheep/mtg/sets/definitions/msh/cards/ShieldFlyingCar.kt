package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * S.H.I.E.L.D. Flying Car — Marvel Super Heroes #74 (rare)
 * {2}{U} · Artifact — Vehicle · 3/3
 *
 * Flash
 * Flying
 * When this Vehicle enters, exile up to one target creature you control. Return that card to the
 * battlefield under its owner's control at the beginning of the next end step.
 * Crew 1
 *
 * Implementation notes:
 * - Flash plus the ETB blink is the combat trick: cast it in response to removal or to re-trigger
 *   an enters ability. The blink is the Abuelo, Ancestral Echo shape — [Effects.Exile] on the
 *   bound target plus a [CreateDelayedTriggerEffect] at [Step.END] moving that card back to the
 *   battlefield. `CreateDelayedTriggerExecutor` bakes the bound target into a concrete entity
 *   reference when the trigger is created, so the return still happens if this Vehicle itself
 *   leaves the battlefield in the meantime (CR 603.7a — the delayed ability is independent of its
 *   source once created).
 * - [Effects.Move] to [Zone.BATTLEFIELD] with no `controllerOverride` returns the card under its
 *   owner's control, matching the printed wording.
 * - "Up to one target" is an optional single target, so it can be cast with no legal (or no
 *   desired) creature: the exile and the delayed return then both no-op.
 * - Crew 1 is [KeywordAbility.crew]; until it is crewed the Vehicle is a noncreature artifact, so
 *   flying only matters once it attacks or blocks as a creature.
 */
val ShieldFlyingCar = card("S.H.I.E.L.D. Flying Car") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 3
    oracleText = "Flash\n" +
        "Flying\n" +
        "When this Vehicle enters, exile up to one target creature you control. Return that card " +
        "to the battlefield under its owner's control at the beginning of the next end step.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "up to one target creature you control",
            TargetCreature(
                count = 1,
                optional = true,
                filter = TargetFilter.CreatureYouControl
            )
        )
        effect = Effects.Composite(
            Effects.Exile(creature),
            CreateDelayedTriggerEffect(
                step = Step.END,
                effect = Effects.Move(creature, Zone.BATTLEFIELD)
            )
        )
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Paulius Daščioras"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e651410-b829-4468-b866-a9dd15f909ad.jpg?1783902952"
    }
}
