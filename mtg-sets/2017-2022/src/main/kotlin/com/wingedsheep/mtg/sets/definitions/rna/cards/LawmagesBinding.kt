package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Lawmage's Binding
 * {1}{W}{U}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * Enchanted creature can't attack or block, and its activated abilities can't be activated.
 *
 * Arrest (MMQ) with flash: the same three statics. The activation lock covers mana abilities too.
 * Ravnica Allegiance is the earliest printing; Jumpstart contributes a `Printing` row.
 */
val LawmagesBinding = card("Lawmage's Binding") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant creature\n" +
        "Enchanted creature can't attack or block, and its activated abilities can't be activated."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "190"
        artist = "Mark Behm"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8a7be1f-3fba-4ff2-a4f1-aebd3d20da8f.jpg?1783933643"
        ruling(
            "2019-01-25",
            "Activated abilities contain a colon. They're generally written \"[Cost]: [Effect].\" " +
                "Some keyword abilities are activated abilities and will have colons in their reminder text. " +
                "Triggered abilities (starting with \"when,\" \"whenever,\" or \"at\") are unaffected by Lawmage's Binding."
        )
        ruling(
            "2019-01-25",
            "Once a creature has attacked or blocked, casting Lawmage's Binding won't remove that creature from combat. " +
                "Similarly, once a creature's ability has been activated, casting Lawmage's Binding won't counter that ability."
        )
    }
}
