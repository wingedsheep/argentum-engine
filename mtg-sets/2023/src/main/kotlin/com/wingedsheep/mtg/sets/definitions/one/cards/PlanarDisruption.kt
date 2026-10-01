package com.wingedsheep.mtg.sets.definitions.one.cards

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
 * Planar Disruption
 * {1}{W}
 * Enchantment — Aura
 *
 * Enchant artifact, creature, or planeswalker
 * Enchanted permanent can't attack or block, and its activated abilities can't be activated.
 *
 * Arrest's three-static shape with a wider enchant restriction. The activation lock
 * ([PreventActivatedAbilities] scoped by `attachedToBySource()`) covers mana abilities and a
 * planeswalker's loyalty abilities alike — both are activated abilities. The combat halves are
 * inert on a noncreature host and apply automatically if it later becomes a creature.
 */
val PlanarDisruption = card("Planar Disruption") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact, creature, or planeswalker\n" +
        "Enchanted permanent can't attack or block, and its activated abilities can't be activated."

    auraTarget = TargetObject(
        filter = TargetFilter(
            GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Planeswalker
        )
    )

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
        collectorNumber = "28"
        artist = "Campbell White"
        flavorText = "New Phyrexia's planar defenses scattered the Planeswalker strike team on " +
            "arrival. Elesh Norn knew they were coming."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8ee69a1f-aeed-4eb4-8987-fa720fc99715.jpg?1783918075"
        ruling(
            "2023-02-04",
            "Activated abilities contain a colon. They're generally written \"[Cost]: [Effect].\" " +
                "Some keywords are activated abilities and will have colons in their reminder " +
                "text. Notably, loyalty abilities of planeswalkers are activated abilities."
        )
    }
}
