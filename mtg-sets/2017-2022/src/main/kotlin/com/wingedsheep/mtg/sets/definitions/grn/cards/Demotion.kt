package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Demotion
 * {W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature can't block, and its activated abilities can't be activated.
 *
 * Arrest (MMQ) without the attack lock. The activation lock covers mana abilities too — there is
 * no "unless they're mana abilities" clause — so `nonManaAbilitiesOnly` stays false.
 */
val Demotion = card("Demotion") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't block, and its activated abilities can't be activated."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Joe Slucher"
        flavorText = "There's no greater honor than joining your chosen guild, and no greater " +
            "shame than failing it."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69202217-ef36-4d6c-bbd0-ebbb2ade51f7.jpg?1783934206"
        ruling(
            "2018-10-05",
            "Activated abilities contain a colon. They're generally written " +
                "\"[Cost]: [Effect].\" Some keyword abilities are activated abilities and will have " +
                "colons in their reminder text. Triggered abilities (starting with \"when,\" " +
                "\"whenever,\" or \"at\") are unaffected by Demotion."
        )
    }
}
