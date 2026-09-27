package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Lure
 * {1}{G}{G}
 * Enchantment — Aura
 * Enchant creature
 * All creatures able to block enchanted creature do so.
 */
val Lure = card("Lure") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nAll creatures able to block enchanted creature do so."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = MustBeBlocked(allCreatures = true, filter = GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a87b26e-0431-42e9-b44f-94ba8546111a.jpg?1783948673"
        ruling(
            "2017-11-17",
            "Lure doesn't give any creatures the ability to block the target creature. It just forces those creatures that are already able to block the creature to do so."
        )
        ruling(
            "2017-11-17",
            "As blockers are declared, any creature that's tapped or affected by a spell or ability that says it can't block doesn't block. If there's a cost associated with having the creature block, no player is forced to pay that cost, so it doesn't block if that cost isn't paid."
        )
    }
}
