package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Chained to the Rocks
 * {W}
 * Enchantment — Aura
 * Enchant Mountain you control
 * When this Aura enters, exile target creature an opponent controls until this Aura leaves the
 * battlefield. (That creature returns under its owner's control.)
 */
val ChainedToTheRocks = card("Chained to the Rocks") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant Mountain you control\n" +
        "When this Aura enters, exile target creature an opponent controls until this Aura leaves the battlefield. " +
        "(That creature returns under its owner's control.)"

    auraTarget = TargetObject(
        filter = TargetFilter(GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN).youControl())
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ExileUntilLeaves(t)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5af083f-5820-4185-ac04-4c4368f7703c.jpg?1783939823"
    }
}
