package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Arrest
 * {2}{W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature can't attack or block, and its activated abilities can't be activated.
 *
 * Mercadian Masques is the earliest real-expansion printing, so the canonical definition lives
 * here; Mirrodin, Scars of Mirrodin and later sets contribute only `Printing` rows.
 *
 * Same three-static shape as Petrify (LCI): Pacifism's combat lock scoped to the attached
 * permanent, plus the Cursed Totem-style activation lock narrowed to just this Aura's host via
 * [PreventActivatedAbilities] with `attachedToBySource()`. The lock covers mana abilities too —
 * Arrest has no "unless they're mana abilities" clause (CR 605.1a abilities are still activated
 * abilities), so `nonManaAbilitiesOnly` stays false.
 */
val Arrest = card("Arrest") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack or block, and its activated abilities can't be activated."

    auraTarget = Targets.Creature

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
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Dan Frazier"
        flavorText = "Orim had no memory of the previous night—the first thing she could " +
            "remember was waking up with the dead guard's blood on her hands."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b083fd8-6422-4cd3-a27d-41b6d88598c2.jpg?1783945985"
        ruling(
            "2016-06-08",
            "Activated abilities contain a colon. They're generally written " +
                "\"[Cost]: [Effect].\" Some keywords are activated abilities and will have " +
                "colons in their reminder text."
        )
    }
}
