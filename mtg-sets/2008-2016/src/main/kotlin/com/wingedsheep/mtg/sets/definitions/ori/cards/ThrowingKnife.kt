package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Throwing Knife (ORI #241)
 * {2}
 * Artifact — Equipment
 *
 * Equipped creature gets +2/+0.
 * Whenever equipped creature attacks, you may sacrifice this Equipment. If you do, this Equipment
 * deals 2 damage to any target.
 * Equip {2}
 *
 * The attack trigger lives on the Equipment itself (`Triggers.attached.attacks()` = "equipped
 * creature attacks"), so its source is the Knife. "Any target" is chosen as the trigger goes on the
 * stack; on resolution [Effects.MayPay] over [SacrificeSelfEffect] asks whether to sacrifice the
 * Knife (only offered while it is still on the battlefield) and deals the 2 damage only if it was
 * sacrificed. The damage source defaults to the ability's source — the now-sacrificed Knife, read
 * by last-known information — not the equipped creature (see ruling).
 */
val ThrowingKnife = card("Throwing Knife") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0.\n" +
        "Whenever equipped creature attacks, you may sacrifice this Equipment. If you do, this " +
        "Equipment deals 2 damage to any target.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(+2, +0, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        val anyTarget = target(Targets.Any)
        effect = Effects.MayPay(
            cost = SacrificeSelfEffect,
            then = Effects.DealDamage(2, anyTarget)
        )
        description = "Whenever equipped creature attacks, you may sacrifice this Equipment. " +
            "If you do, this Equipment deals 2 damage to any target."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dfce9be-67a2-4e8a-a71f-e88f64067691.jpg?1783938307"

        ruling("2015-06-22", "Throwing Knife's triggered ability triggers before blockers are declared. If you use the ability and destroy the targeted creature with lethal damage, it won't be on the battlefield to block.")
        ruling("2015-06-22", "Throwing Knife is the source of the damage dealt because of its triggered ability. For example, you could sacrifice Throwing Knife to deal 2 damage to a creature with protection from red, even if Throwing Knife is equipped to a red creature.")
    }
}
