package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantActivateAbilities
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Grand Abolisher — Magic 2012 #19 (canonical printing)
 * {W}{W} · Creature — Human Cleric · 2/2
 *
 * During your turn, your opponents can't cast spells or activate abilities of artifacts,
 * creatures, or enchantments.
 *
 * Two reusable continuous restrictions, each scoped to your opponents during your turn:
 *  - The cast clause is [PlayersCantCastSpells] (`EachOpponent`, `condition = IsYourTurn`),
 *    read at cast-legality time across every casting zone — the same primitive Voice of
 *    Victory uses.
 *  - The activate clause is [PlayersCantActivateAbilities] (`EachOpponent`,
 *    `permanentFilter = artifact/creature/enchantment`, `condition = IsYourTurn`), read at
 *    ability-activation-legality time. Loyalty abilities of planeswalkers and land mana
 *    abilities are unaffected because the filter only matches artifacts, creatures, and
 *    enchantments in projected state — exactly as the oracle text restricts.
 */
val GrandAbolisher = card("Grand Abolisher") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "During your turn, your opponents can't cast spells or activate abilities of " +
        "artifacts, creatures, or enchantments."

    staticAbility {
        ability = PlayersCantCastSpells(affected = Player.EachOpponent, condition = IsYourTurn)
    }

    staticAbility {
        ability = PlayersCantActivateAbilities(
            affected = Player.EachOpponent,
            permanentFilter = GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Enchantment,
            condition = IsYourTurn,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Eric Deschamps"
        flavorText = "\"Your superstitions and mumblings are useless chaff before my righteousness.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67e35a40-37dd-436c-b4ac-b17b04508c1f.jpg?1562645447"
    }
}
