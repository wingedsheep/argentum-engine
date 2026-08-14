package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stitcher's Graft — Eldritch Moon #200
 * {1} · Artifact — Equipment
 *
 * Equipped creature gets +3/+3.
 * Whenever equipped creature attacks, it doesn't untap during its controller's next untap step.
 * Whenever this Equipment becomes unattached from a permanent, sacrifice that permanent.
 * Equip {2}
 *
 * The attack rider is the same shape as Crippling Chill's: [AbilityFlag.DOESNT_UNTAP] granted for
 * [Duration.UntilAfterAffectedControllersNextUntap], which the untap step already honours. Stacking
 * two Grafts on one attacker grants the flag twice over the *same* untap step rather than two, per
 * the 2016-07-13 ruling.
 *
 * The drawback rides [Triggers.becomesUnattached] — the mirror of "becomes attached", which fires on
 * every way an Equipment can come off (CR 701.3d): equipping it to a new creature, the Graft leaving
 * the battlefield, the host leaving the battlefield, and the CR 704.5n state-based unattach when the
 * host stops being a creature or the Graft stops being an Equipment. "That permanent" is
 * [EffectTarget.AttachedToTriggeringPermanent], the former host carried on the trigger. When that
 * host is the thing that left the battlefield it resolves to nothing and the sacrifice is a no-op,
 * which is precisely what the ruling calls for; likewise it can't sacrifice a permanent whose
 * control you no longer have.
 */
val StitchersGraft = card("Stitcher's Graft") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+3.\n" +
        "Whenever equipped creature attacks, it doesn't untap during its controller's next untap step.\n" +
        "Whenever this Equipment becomes unattached from a permanent, sacrifice that permanent.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(+3, +3, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attacks(binding = TriggerBinding.ATTACHED)
        effect = Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            EffectTarget.EquippedCreature,
            Duration.UntilAfterAffectedControllersNextUntap
        )
        description = "Whenever equipped creature attacks, it doesn't untap during its " +
            "controller's next untap step."
    }

    triggeredAbility {
        trigger = Triggers.becomesUnattached()
        effect = Effects.SacrificeTarget(EffectTarget.AttachedToTriggeringPermanent)
        description = "Whenever this Equipment becomes unattached from a permanent, sacrifice that permanent."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Josh Hass"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb6d700e-0514-41d7-9255-cedd859316fc.jpg?1783937422"

        ruling("2016-07-13", "If multiple effects say that a creature doesn't untap during your next untap step, those effects all apply during one untap step. For example, a creature that attacks while equipped with two Stitcher's Grafts will only spend one untap step without untapping.")
        ruling("2016-07-13", "Stitcher's Graft becomes unattached from the creature it's equipping if you equip it to a new creature, if Stitcher's Graft leaves the battlefield, if the equipped creature ceases to be a creature, or if Stitcher's Graft ceases to be an Equipment. (It also becomes unattached if the equipped creature leaves the battlefield, but the triggered ability won't do anything in that case.)")
        ruling("2016-07-13", "If Stitcher's Graft's last triggered ability triggers, but you don't control the permanent it became unattached from at the time that ability resolves (perhaps because another player has somehow gained control of it), you won't be able to sacrifice it.")
    }
}
