package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pirate Hat (LCI #70) — {1}{U} Artifact — Equipment
 *
 * Equipped creature gets +1/+1 and has "Whenever this creature attacks, draw a card,
 * then discard a card."
 * Equip Pirate {1}
 * Equip {2}
 *
 * Implementation notes:
 * - The +1/+1 pump is a [ModifyStats] static ability scoped to [Filters.EquippedCreature].
 * - The attack-triggered loot (draw a card, then discard a card) is granted to the equipped
 *   creature via [GrantTriggeredAbility] with [Triggers.attacks()] (SELF binding) so the ability
 *   lives on the creature and fires when that creature attacks. The loot resolves for that
 *   creature's controller via [Patterns.Hand.loot] (draw uses [EffectTarget.Controller]).
 * - "Equip Pirate {1}" is a variant equip keyword ("{1}: Attach to target Pirate creature you
 *   control. Equip only as a sorcery.", CR 702.6c) — authored as `equipAbility(quality = "Pirate")`
 *   so it stays a real equip ability, exactly like the unrestricted "Equip {2}".
 */
val PirateHat = card("Pirate Hat") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 and has \"Whenever this creature attacks, draw a card, " +
        "then discard a card.\"\n" +
        "Equip Pirate {1}\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    // Equipped creature gets +1/+1.
    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    // Equipped creature has "Whenever this creature attacks, draw a card, then discard a card."
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.attacks().event,
                binding = Triggers.attacks().binding,
                effect = Patterns.Hand.loot()
            ),
            filter = Filters.EquippedCreature
        )
    }

    // Equip Pirate {1}: attach only to a Pirate creature you control, sorcery speed (CR 702.6c).
    equipAbility(
        "{1}",
        quality = "Pirate",
        targetFilter = TargetFilter.CreatureYouControl.withSubtype(Subtype.PIRATE),
    )

    // Equip {2}: attach to any creature you control, sorcery speed.
    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Domenico Cava"
        flavorText = "\"Landlubbers just don't understand the value of a proper hat.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d11e3d4-769d-47aa-8d3a-ce1ac60b68b8.jpg?1782694554"
    }
}
