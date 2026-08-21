package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scythe of the Wretched — Mirrodin #239
 * {2} · Artifact — Equipment · Rare
 *
 * Equipped creature gets +2/+2.
 * Whenever a creature dealt damage by equipped creature this turn dies, return that card to the
 * battlefield under your control. Attach this Equipment to that creature.
 * Equip {4}
 *
 * Modelling notes:
 * - The trigger is the Soul Collector shape read **one object further out**. The engine already tracks
 *   "creatures this permanent damaged this turn" on the damaging source, and already had the
 *   `sourceFilter == null` reading of it; what it ignored was the *binding*. `TriggerBinding.ATTACHED`
 *   now means "the damaging source is the permanent this is attached to", so
 *   [Triggers.CreatureDealtDamageByAttachedDies] needed no new event and no new tracker — only the
 *   detector reading the tracker off the attachment target.
 * - The attachment is resolved when the creature *dies*, which is what the card's own ruling requires:
 *   the Scythe must be equipped at that moment, but it need not have been equipped when the damage was
 *   dealt. An unattached Scythe therefore never triggers, and a Scythe that moved between the damage
 *   and the death still does.
 * - "Return that card" reads the dying creature through `EffectTarget.TriggeringEntity`, guarded on the
 *   card still being in the graveyard (`PutOntoBattlefieldFromGraveyard`). That guard is what makes a
 *   token correct for free: a token that died is gone by resolution, so nothing returns and the attach
 *   finds no host.
 * - The attach names the same `TriggeringEntity` — the card keeps its id across the zone change, so the
 *   returned permanent is the thing the Equipment moves onto. If it comes back as something that isn't a
 *   creature (the third ruling), no card-level check is needed: the "attached to an illegal permanent"
 *   state-based action unattaches the Equipment on the next check.
 * - Both halves sit in one `Composite` because the card is one sentence pair with no "may" — the return
 *   is mandatory and the attach follows it unconditionally.
 */
val ScytheOfTheWretched = card("Scythe of the Wretched") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2.\n" +
        "Whenever a creature dealt damage by equipped creature this turn dies, return that card to " +
        "the battlefield under your control. Attach this Equipment to that creature.\n" +
        "Equip {4}"

    // "Equipped creature gets +2/+2."
    staticAbility {
        ability = ModifyStats(2, 2)
    }

    // "Whenever a creature dealt damage by equipped creature this turn dies, return that card to the
    // battlefield under your control. Attach this Equipment to that creature."
    triggeredAbility {
        trigger = Triggers.CreatureDealtDamageByAttachedDies
        effect = Effects.Composite(
            Effects.PutOntoBattlefieldFromGraveyard(
                target = EffectTarget.TriggeringEntity,
                underYourControl = true
            ),
            Effects.AttachEquipment(EffectTarget.TriggeringEntity)
        )
        description = "Whenever a creature dealt damage by equipped creature this turn dies, return " +
            "that card to the battlefield under your control. Attach this Equipment to that creature."
    }

    equipAbility("{4}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "239"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/143e7a81-fb3d-4bad-854a-b24138ca7415.jpg?1783944505"
        ruling(
            "2004-12-01",
            "The Scythe's ability triggers even if something other than the damage dealt by the " +
                "equipped creature causes the creature to be put into a graveyard that turn."
        )
        ruling(
            "2004-12-01",
            "Scythe of the Wretched needs to equip your creature when the other creature goes to the " +
                "graveyard for the ability to trigger, but it doesn't matter whether the Scythe " +
                "equipped your creature when the damage was actually dealt."
        )
        ruling(
            "2004-12-01",
            "If the card isn't a creature when it comes back onto the battlefield, then Scythe of the " +
                "Wretched won't move onto it."
        )
    }
}
