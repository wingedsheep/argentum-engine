package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Stone Haven Outfitter — Oath of the Gatewatch #37 (canonical printing)
 * {1}{W} · Creature — Kor Artificer Ally · 2/2
 *
 * Equipped creatures you control get +1/+1.
 * Whenever an equipped creature you control dies, draw a card.
 *
 * Modeling notes:
 *  - The lord is a static `ModifyStats` over equipped creatures you control (Kemba, Kha Enduring's
 *    shape) — +1/+1 once per creature, however many Equipment it carries.
 *  - The dies trigger's `equipped()` gate reads last-known information (`wasEquipped` on the
 *    zone-change snapshot), so it still sees the Equipment the creature had as it died. The ANY
 *    binding includes the Outfitter itself if it was equipped.
 */
private val equippedCreatureYouControl = GameObjectFilter.Creature.youControl().equipped()

val StoneHavenOutfitter = card("Stone Haven Outfitter") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Artificer Ally"
    power = 2
    toughness = 2
    oracleText = "Equipped creatures you control get +1/+1.\n" +
        "Whenever an equipped creature you control dies, draw a card."

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter(equippedCreatureYouControl))
    }

    triggeredAbility {
        trigger = Triggers.a(equippedCreatureYouControl).dies()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Jason Rainville"
        flavorText = "The makers and menders are the first called to battle and the last to leave."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1b98e63-f23d-432a-86ae-f88bdad2f648.jpg?1783937922"
        ruling(
            "2016-01-22",
            "Equipped creatures will get just +1/+1 from Stone Haven Outfitter, no matter how many Equipment are attached to them. Similarly, the last ability will trigger just once per equipped creature."
        )
        ruling(
            "2016-01-22",
            "In this context, \"equipped creatures you control\" and \"equipped creature you control\" refer to any creatures you control with Equipment attached to them. In the highly unusual case that Stone Haven Outfitter becomes an Equipment, the meaning of its abilities doesn't change. The bonuses continue to refer to any equipped creature you control. They aren't bonuses applied solely to the creature Stone Haven Outfitter is attached to."
        )
    }
}
