package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Seal Away
 * {1}{W}
 * Enchantment
 * Flash
 * When Seal Away enters the battlefield, exile target tapped creature an opponent
 * controls until Seal Away leaves the battlefield.
 */
val SealAway = card("Seal Away") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment"
    oracleText = "Flash\nWhen Seal Away enters the battlefield, exile target tapped creature an opponent controls until Seal Away leaves the battlefield."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "tapped creature an opponent controls",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.tapped().opponentControls()))
        )
        effect = Effects.ExileUntilLeaves(creature)
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Joseph Meehan"
        flavorText = "\"An ancient nemesis rendered harmless long ago.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f8d6588-671d-4eb3-874f-f7139da2e05a.jpg?1562739448"
    }
}
