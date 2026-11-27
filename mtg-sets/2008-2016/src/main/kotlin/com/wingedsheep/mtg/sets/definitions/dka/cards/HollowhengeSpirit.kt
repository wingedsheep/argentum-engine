package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hollowhenge Spirit
 * {3}{W}
 * Creature — Spirit
 * 2/2
 * Flash
 * Flying
 * When this creature enters, remove target attacking or blocking creature from combat.
 *
 * [Effects.RemoveFromCombat] keeps its default `unblockSoleBlockedAttackers = false`, so an attacker
 * whose blocker is removed stays blocked, and the removed creature's tapped status is untouched.
 */
val HollowhengeSpirit = card("Hollowhenge Spirit") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Flying\n" +
        "When this creature enters, remove target attacking or blocking creature from combat."
    power = 2
    toughness = 2

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.AttackingOrBlockingCreature)
        effect = Effects.RemoveFromCombat(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Lars Grant-West"
        flavorText = "Some spirits mourn. Others terrorize."
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b1be91a-cdec-4933-b834-0a7838abb9b8.jpg?1783940853"
        ruling("2011-01-22", "Removing an attacking creature from combat doesn't untap that creature.")
        ruling(
            "2011-01-22",
            "Removing a blocking creature from combat doesn't cause the creature it was blocking to become " +
                "unblocked. If no other creatures are blocking that attacking creature, it won't assign combat " +
                "damage (unless it has trample)."
        )
        ruling(
            "2011-01-22",
            "If there are multiple combat phases in a turn, a creature that's been removed from combat can " +
                "still attack or block in future combat phases that turn."
        )
    }
}
