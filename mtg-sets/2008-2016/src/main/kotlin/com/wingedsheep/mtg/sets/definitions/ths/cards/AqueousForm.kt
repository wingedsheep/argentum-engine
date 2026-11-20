package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Aqueous Form
 * {U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature can't be blocked.
 * Whenever enchanted creature attacks, scry 1.
 *
 * The scry belongs to the Aura's controller ("you"), not the enchanted creature's controller.
 */
val AqueousForm = card("Aqueous Form") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't be blocked.\n" +
        "Whenever enchanted creature attacks, scry 1. " +
        "(Look at the top card of your library. You may put that card on the bottom.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantBeBlocked(GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba104245-0483-407f-971b-ad6d8efd9733.jpg?1783939802"
    }
}
