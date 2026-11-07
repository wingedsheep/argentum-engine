package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Depala, Pilot Exemplar
 * {1}{R}{W}
 * Legendary Creature — Dwarf Pilot
 * 3/3
 * Other Dwarves you control get +1/+1.
 * Each Vehicle you control gets +1/+1 as long as it's a creature.
 * Whenever Depala becomes tapped, you may pay {X}. If you do, reveal the top X cards of your
 * library, put all Dwarf and Vehicle cards from among them into your hand, then put the rest on the
 * bottom of your library in a random order.
 *
 * The Vehicle lord's filter is `Creature` + `Vehicle`, so it reads projected state: a crewed Vehicle
 * (an artifact creature from layer 4) is matched, an uncrewed one is not. The tap trigger is
 * `Triggers.self.becomesTapped()` — the engine emits the tap event only on an untapped → tapped change,
 * matching the ruling. The payment is `Effects.MayPayX` (X = 0 is a payment that reveals nothing);
 * X then feeds [Patterns.Library.revealTopPutAllMatchingToHand], whose defaults are "the rest on the
 * bottom of your library in a random order".
 */
val DepalaPilotExemplar = card("Depala, Pilot Exemplar") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Dwarf Pilot"
    oracleText = "Other Dwarves you control get +1/+1.\n" +
        "Each Vehicle you control gets +1/+1 as long as it's a creature.\n" +
        "Whenever Depala becomes tapped, you may pay {X}. If you do, reveal the top X cards of your " +
        "library, put all Dwarf and Vehicle cards from among them into your hand, then put the rest on " +
        "the bottom of your library in a random order."
    power = 3
    toughness = 3

    // Other Dwarves you control get +1/+1.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.DWARF).youControl(),
                excludeSelf = true
            )
        )
    }

    // Each Vehicle you control gets +1/+1 as long as it's a creature.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.VEHICLE).youControl())
        )
    }

    // Whenever Depala becomes tapped, you may pay {X}. If you do, reveal the top X cards ...
    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.MayPayX(
            descriptionOverride = "You may pay {X}. If you do, reveal the top X cards of your library, " +
                "put all Dwarf and Vehicle cards from among them into your hand, then put the rest on " +
                "the bottom of your library in a random order.",
            then = Patterns.Library.revealTopPutAllMatchingToHand(
                count = DynamicAmounts.xValue(),
                filter = GameObjectFilter.Any.withAnySubtype("Dwarf", "Vehicle"),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "178"
        artist = "Greg Opalinski"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/592f3a92-787e-4180-9d0b-06c973ce8975.jpg?1783937169"
        ruling(
            "2016-09-20",
            "Depala, Pilot Exemplar's last ability is a triggered ability, not an activated ability. " +
                "It doesn't allow you to tap Depala whenever you want; rather, you need some other way " +
                "of tapping it, such as by attacking."
        )
        ruling(
            "2016-09-20",
            "For the ability to trigger, Depala has to actually change from untapped to tapped. If an " +
                "effect attempts to tap Depala, but it was already tapped at the time, this ability " +
                "won't trigger."
        )
    }
}
