package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Morcant's Loyalist
 * {1}{B}{G}
 * Creature — Elf Warrior
 * 3/2
 *
 * Other Elves you control get +1/+1.
 * When this creature dies, return another target Elf card from your graveyard to your hand.
 */
val MorcantsLoyalist = card("Morcant's Loyalist") {
    manaCost = "{1}{B}{G}"
    typeLine = "Creature — Elf Warrior"
    power = 3
    toughness = 2
    oracleText = "Other Elves you control get +1/+1.\n" +
        "When this creature dies, return another target Elf card from your graveyard to your hand."

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype("Elf").youControl(),
                excludeSelf = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.Dies
        val elfCard = target(
            "another target Elf card from your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Any.withSubtype("Elf").ownedByYou(),
                    zone = Zone.GRAVEYARD,
                    excludeSelf = true
                )
            )
        )
        effect = Effects.ReturnToHand(elfCard)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Evyn Fong"
        flavorText = "\"Despite recent alliances, we must never forget the ideals of perfection which define us.\"\n" +
            "—High Perfect's decree"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/175c6b1c-8790-41a2-ae15-5031206c410f.jpg?1767952302"
    }
}
