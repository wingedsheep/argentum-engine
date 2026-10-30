package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Temple Thief
 * {1}{B}
 * Creature — Human Rogue
 * 2/2
 * This creature can't be blocked by enchanted creatures or enchantment creatures.
 *
 * Bog Rats' [CantBeBlockedBy] over a union blocker filter: creatures with an Aura attached
 * (`enchanted()`) or creatures that are also enchantments. The restriction is checked as blockers
 * are declared, so a blocker becoming enchanted afterwards doesn't unblock the Thief (ruling).
 */
val TempleThief = card("Temple Thief") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 2
    oracleText = "This creature can't be blocked by enchanted creatures or enchantment creatures."

    staticAbility {
        ability = CantBeBlockedBy(
            GameObjectFilter.Creature.enchanted() or (GameObjectFilter.Creature and GameObjectFilter.Enchantment)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Evyn Fong"
        flavorText = "\"I don't know if the gods watch over their temples, but I figure it's best to be " +
            "stealthy just in case.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc4a61aa-75d2-46bc-b13f-b863f5f49d32.jpg?1783931559"
        ruling(
            "2020-01-24",
            "Once a creature has blocked Temple Thief, causing that creature to become enchanted (or, in some " +
                "rare cases, causing it to become an enchantment creature) won't cause Temple Thief to become " +
                "unblocked."
        )
    }
}
