package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Pacifism
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't attack or block.
 */
val Pacifism = card("Pacifism") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment — Aura"

    auraTarget = Targets.Creature

    staticAbility {
        ability = CantAttack(target = StaticTarget.AttachedCreature)
    }

    staticAbility {
        ability = CantBlock(target = StaticTarget.AttachedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Robert Bliss"
        flavorText = "For the first time in his life, Grakk felt a strange sensation. It wouldn't be until later that he would recognize it as peace."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e0df325-ef60-4944-b326-535585e2ccbb.jpg?1562932414"
    }
}
