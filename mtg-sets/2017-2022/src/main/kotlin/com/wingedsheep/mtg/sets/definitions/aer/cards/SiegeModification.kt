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
 * Siege Modification — Aether Revolt #99
 * {1}{R}{R} · Enchantment — Aura
 *
 * Enchant creature or Vehicle
 * As long as enchanted permanent is a Vehicle, it's a creature in addition to its other types.
 * Enchanted creature gets +3/+0 and has first strike.
 *
 * Aerial Modification's shape: a Layer 4 [GrantCardType] on the attached permanent, gated by
 * [Conditions.EnchantedPermanentMatches] on the Vehicle subtype, then the usual Aura stat and
 * keyword grants once the host is a creature.
 */
val SiegeModification = card("Siege Modification") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature or Vehicle\n" +
        "As long as enchanted permanent is a Vehicle, it's a creature in addition to its other types.\n" +
        "Enchanted creature gets +3/+0 and has first strike."

    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle))

    staticAbility {
        ability = GrantCardType("CREATURE", filter = GroupFilter.attachedCreature())
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE))
    }

    staticAbility {
        ability = ModifyStats(3, 0)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Sung Choi"
        flavorText = "Perfect for getting through to the consuls."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1a4e33f-53f0-4919-98a1-c832dcd32efb.jpg?1783936748"
        ruling(
            "2017-02-09",
            "If Siege Modification becomes unattached from a Vehicle that's attacking or blocking, that " +
                "Vehicle will be removed from combat unless another effect (such as its crew ability) is " +
                "also making it a creature."
        )
    }
}
