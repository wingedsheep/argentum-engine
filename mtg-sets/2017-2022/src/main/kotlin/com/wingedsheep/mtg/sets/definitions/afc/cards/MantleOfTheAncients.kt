package com.wingedsheep.mtg.sets.definitions.afc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.AttachmentKind

/**
 * Mantle of the Ancients — Forgotten Realms Commander #8
 * {3}{W}{W} · Enchantment — Aura
 *
 * Enchant creature you control
 * When this Aura enters, return any number of target Aura and/or Equipment cards from your
 * graveyard to the battlefield attached to enchanted creature.
 * Enchanted creature gets +1/+1 for each Aura and Equipment attached to it.
 *
 * The return is one `move(…, attachTo = EnchantedCreature)` of the still-legal targets: the host is
 * whatever the Mantle is attached to as the ability resolves, or the creature it was last attached
 * to if the Mantle has left. Per the first ruling, a target that couldn't be legally attached stays
 * in the graveyard — the `couldAttachTo(EnchantedCreature)` filter keeps it out of the move, where
 * `attachTo` alone would let an unattachable Equipment enter unattached.
 */
val MantleOfTheAncients = card("Mantle of the Ancients") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "When this Aura enters, return any number of target Aura and/or Equipment cards from your " +
        "graveyard to the battlefield attached to enchanted creature.\n" +
        "Enchanted creature gets +1/+1 for each Aura and Equipment attached to it."

    auraTarget = TargetObject(filter = TargetFilter.CreatureYouControl)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter(GameObjectFilter.Any.withAnySubtype("Aura", "Equipment").ownedByYou(), zone = Zone.GRAVEYARD),
            unlimited = true
        )
        effect = Effects.Pipeline {
            val returning = gather(CardSource.ChosenTargets)
            move(
                returning,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                attachTo = EffectTarget.EnchantedCreature,
                filter = GameObjectFilter.Any.couldAttachTo(EffectTarget.EnchantedCreature)
            )
        }
    }

    // "Aura and Equipment" — not every attachment: a Fortification on an animated land isn't counted.
    val attached = DynamicAmounts.attachmentsOnEnchantedCreature(AttachmentKind.AURA) +
        DynamicAmounts.attachmentsOnEnchantedCreature(AttachmentKind.EQUIPMENT)
    staticAbility {
        ability = GrantDynamicStats(
            filter = Filters.EnchantedCreature,
            powerBonus = attached,
            toughnessBonus = attached
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Denman Rooke"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/867124fc-c8e4-4e0e-be41-b74a6bb34e86.jpg?1783926258"
        ruling(
            "2021-07-23",
            "As the enters-the-battlefield ability is resolving, any targets that couldn't be legally " +
                "attached to the creature enchanted by Mantle of the Ancients stay in your graveyard."
        )
        ruling(
            "2021-07-23",
            "If Mantle of the Ancients becomes attached to a different creature in response to its " +
                "enters-the-battlefield ability, the creature it's attached to as that ability is resolving " +
                "is the \"enchanted creature,\" even though you used the original enchanted creature to " +
                "choose targets. This means some of those targets may become illegal."
        )
        ruling(
            "2021-07-23",
            "If Mantle of the Ancients leaves the battlefield before its enters-the-battlefield ability " +
                "resolves, the last permanent it was attached to before leaving the battlefield is the " +
                "\"enchanted creature\" (even if that permanent is no longer a creature)."
        )
    }
}
