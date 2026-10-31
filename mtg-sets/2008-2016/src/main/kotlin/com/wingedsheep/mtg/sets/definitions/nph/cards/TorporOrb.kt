package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressZoneChangeTriggers

/**
 * Torpor Orb
 * {2}
 * Artifact
 * Creatures entering don't cause abilities to trigger.
 *
 * The [SuppressZoneChangeTriggers] static (default `filter = Creature`): when a creature enters the
 * battlefield, no triggered ability fires from that entry — neither the creature's own
 * enters-the-battlefield triggers nor any other permanent's "whenever a [...] enters" trigger that
 * the creature would cause. Replacement effects (enters with counters / tapped) and "as it enters"
 * choices are unaffected, matching the printed rulings.
 */
val TorporOrb = card("Torpor Orb") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Creatures entering don't cause abilities to trigger."

    staticAbility {
        ability = SuppressZoneChangeTriggers()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Svetlin Velinov"
        flavorText = "\"Phyrexia is certainly dangerous, but I have to admire some of its innovations.\"\n—Tezzeret"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/953610f6-ea96-4e71-969f-50ecac09c091.jpg?1722108794"
    }
}
