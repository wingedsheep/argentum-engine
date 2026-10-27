package com.wingedsheep.mtg.sets.definitions.dmc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * The Reaver Cleaver
 * {2}{R}
 * Legendary Artifact — Equipment
 *
 * Equipped creature gets +1/+1 and has trample and "Whenever this creature deals combat damage to
 * a player or planeswalker, create that many Treasure tokens."
 * Equip {3}
 *
 * The quoted trigger is granted to the equipped creature (SELF-bound inside the granted ability),
 * so the creature's controller creates the Treasures — the Ceremonial Knife shape.
 */
val TheReaverCleaver = card("The Reaver Cleaver") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 and has trample and \"Whenever this creature deals " +
        "combat damage to a player or planeswalker, create that many Treasure tokens.\"\n" +
        "Equip {3}"

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrPlaneswalker),
                effect = Effects.CreateTreasure(DynamicAmounts.triggerDamageAmount()),
                descriptionOverride = "Whenever this creature deals combat damage to a player or " +
                    "planeswalker, create that many Treasure tokens."
            )
        )
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Yigit Koroglu"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bcd1591-b5b9-49fc-9f2a-45f31ed1871e.jpg?1783921476"
    }
}
