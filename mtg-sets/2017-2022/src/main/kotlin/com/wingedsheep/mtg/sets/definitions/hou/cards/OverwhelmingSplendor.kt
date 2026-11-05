package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbilityScope
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.PlayersCantActivateAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Overwhelming Splendor
 * {6}{W}{W}
 * Enchantment — Aura Curse
 * Enchant player
 * Creatures enchanted player controls lose all abilities and have base power and toughness 1/1.
 * Enchanted player can't activate abilities that aren't mana abilities or loyalty abilities.
 *
 * The activation lock is [PlayersCantActivateAbilities] scoped to [Player.EnchantedPlayer] with
 * `abilities = NON_MANA_NON_LOYALTY` and `anyZone = true` — the wording is unqualified, so cycling
 * and graveyard abilities are forbidden too, while a planeswalker's loyalty abilities stay legal.
 */
val OverwhelmingSplendor = card("Overwhelming Splendor") {
    manaCost = "{6}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\n" +
        "Creatures enchanted player controls lose all abilities and have base power and toughness 1/1.\n" +
        "Enchanted player can't activate abilities that aren't mana abilities or loyalty abilities."

    auraTarget = Targets.Player

    val theirCreatures = GroupFilter(GameObjectFilter.Creature.controlledByEnchantedPlayer())

    staticAbility { ability = LoseAllAbilities(filter = theirCreatures) }
    staticAbility { ability = SetBasePowerToughnessStatic(power = 1, toughness = 1, filter = theirCreatures) }
    staticAbility {
        ability = PlayersCantActivateAbilities(
            affected = Player.EnchantedPlayer,
            abilities = ActivatedAbilityScope.NON_MANA_NON_LOYALTY,
            anyZone = true
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "19"
        artist = "Richard Wright"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/867b32d2-e396-411d-ac02-1af4106dd3d2.jpg?1783936059"
    }
}
