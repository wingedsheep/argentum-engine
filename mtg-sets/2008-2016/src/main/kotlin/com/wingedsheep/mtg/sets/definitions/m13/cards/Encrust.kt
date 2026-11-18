package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Encrust
 * {1}{U}{U}
 * Enchantment — Aura
 *
 * Enchant artifact or creature
 * Enchanted permanent doesn't untap during its controller's untap step and its activated
 * abilities can't be activated.
 *
 * Magic 2013 is the earliest real-expansion printing, so the canonical definition lives here;
 * Magic 2015 contributes a `Printing` row.
 *
 * Malfunction's untap lock (the default `GrantKeyword` scope is whatever this Aura is attached
 * to, artifact or creature) plus Arrest's activation lock via [PreventActivatedAbilities] scoped
 * by `attachedToBySource()`. Mana abilities are locked too — there's no "unless they're mana
 * abilities" clause — so `abilities` stays `ALL`.
 */
val Encrust = card("Encrust") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact or creature\n" +
        "Enchanted permanent doesn't untap during its controller's untap step and its activated " +
        "abilities can't be activated."

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact)

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Jason Felix"
        flavorText = "\"Barnacles are the sea's final defense.\"\n—Garild, merfolk mage"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfd05474-5cec-4c71-85e7-79cf25958525.jpg?1783940509"
        ruling(
            "2016-06-08",
            "Activated abilities contain a colon. They're generally written " +
                "\"[Cost]: [Effect].\" Some keywords are activated abilities and will have " +
                "colons in their reminder text.",
        )
        ruling("2014-07-18", "Static and triggered abilities are unaffected by Encrust.")
        ruling("2014-07-18", "The permanent can be untapped by other spells and abilities.")
    }
}
