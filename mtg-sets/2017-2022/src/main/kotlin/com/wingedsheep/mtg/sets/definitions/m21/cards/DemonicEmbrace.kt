package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Demonic Embrace
 * {1}{B}{B}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +3/+1, has flying, and is a Demon in addition to its other types.
 * You may cast this card from your graveyard by paying 3 life and discarding a card in addition
 * to paying its other costs.
 *
 * The graveyard clause is Wickerfolk Indomitable's shape: a self cast-from-graveyard permission
 * carrying a composite additional cost (pay 3 life, then discard a card). Normal Aura timing and
 * the {1}{B}{B} mana cost still apply (ruling).
 */
val DemonicEmbrace = card("Demonic Embrace") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +3/+1, has flying, and is a Demon in addition to its other types.\n" +
        "You may cast this card from your graveyard by paying 3 life and discarding a card in addition to " +
        "paying its other costs."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(3, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    staticAbility {
        // GrantSubtype defaults its filter to the source — an Aura names the enchanted creature.
        ability = GrantSubtype("Demon", Filters.EnchantedCreature)
    }

    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            additionalCost = Costs.additional.Composite(
                listOf(
                    Costs.additional.PayLife(3),
                    Costs.additional.DiscardCards(1)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "95"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56149192-ddc1-45a4-b7aa-be311da5fe8e.jpg?1783930711"
        ruling("2020-06-23", "Casting Demonic Embrace with the permission granted by its last ability doesn't change when you can cast it.")
        ruling("2020-06-23", "If another effect gives you permission to cast Demonic Embrace from your graveyard, you don't pay the additional cost imposed by its last ability if you use that permission.")
    }
}
