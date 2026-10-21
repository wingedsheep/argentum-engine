package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gold-Forged Thopteryx — March of the Machine: The Aftermath #31
 * {W}{U} · Artifact Creature — Dinosaur Thopter · 1/3
 *
 * Flying, lifelink
 * Each legendary permanent you control has ward {2}.
 *
 * The Thopteryx itself isn't legendary, so it never gets ward from its own grant.
 */
val GoldForgedThopteryx = card("Gold-Forged Thopteryx") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Artifact Creature — Dinosaur Thopter"
    power = 1
    toughness = 3
    oracleText = "Flying, lifelink\n" +
        "Each legendary permanent you control has ward {2}. (Whenever it becomes the target of a spell " +
        "or ability an opponent controls, counter it unless that player pays {2}.)"

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    staticAbility {
        ability = GrantWard(
            cost = WardCost.Mana("{2}"),
            filter = GroupFilter(GameObjectFilter.Permanent.legendary().youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Titus Lunter"
        flavorText = "Worlds apart, Huatli built a device Saheeli would have been proud of."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/6917d51b-eb61-4e7e-8336-8b4adf8d1e39.jpg?1783916516"
        ruling(
            "2023-05-12",
            "Once a ward ability has triggered, causing that legendary permanent to lose ward by removing " +
                "Gold-Forged Thopteryx won't affect that ability. The appropriate player will still have to " +
                "pay {2} or see their spell or ability countered."
        )
    }
}
