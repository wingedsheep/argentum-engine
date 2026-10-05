package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Aerial Modification — Aether Revolt #1
 * {4}{W} · Enchantment — Aura
 *
 * Enchant creature or Vehicle
 * As long as enchanted permanent is a Vehicle, it's a creature in addition to its other types.
 * Enchanted creature gets +2/+2 and has flying.
 *
 * The enchant clause is [GameObjectFilter.CreatureOrVehicle] (a Vehicle matched by its subtype), as
 * on Silken Strength. The animation is a Layer 4 [GrantCardType] on the attached permanent, gated by
 * [Conditions.EnchantedPermanentMatches] on the Vehicle subtype — `Scope.AttachedTo` ignores the
 * group's base filter, so the "is a Vehicle" test has to live in the condition. Once the host is a
 * creature, the +2/+2 and flying apply through the usual Aura auto-targeting.
 */
val AerialModification = card("Aerial Modification") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature or Vehicle\n" +
        "As long as enchanted permanent is a Vehicle, it's a creature in addition to its other types.\n" +
        "Enchanted creature gets +2/+2 and has flying."

    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle))

    staticAbility {
        ability = GrantCardType("CREATURE", filter = GroupFilter.attachedCreature())
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE))
    }

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Jung Park"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b89cab47-25fb-49ea-bb43-90a0089b6b20.jpg?1783936786"
        ruling(
            "2017-02-09",
            "If Aerial Modification becomes unattached from a Vehicle that's attacking or blocking, that " +
                "Vehicle will be removed from combat unless another effect (such as its crew ability) is " +
                "also making it a creature."
        )
    }
}
