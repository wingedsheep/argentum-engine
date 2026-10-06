package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Spiteful Prankster
 * {2}{R}
 * Creature — Devil
 * 3/2
 * During your turn, this creature has first strike.
 * Whenever another creature dies, this creature deals 1 damage to target player or planeswalker.
 */
val SpitefulPrankster = card("Spiteful Prankster") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil"
    power = 3
    toughness = 2
    oracleText = "During your turn, this creature has first strike.\n" +
        "Whenever another creature dies, this creature deals 1 damage to target player or planeswalker."

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.source())
        condition = Conditions.IsYourTurn
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).dies()
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(1, victim)
        description = "Whenever another creature dies, this creature deals 1 damage to target player or planeswalker."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Antonio José Manzanedo"
        flavorText = "The devil appreciated the irony of using doves for ammunition. The townsfolk, not so much."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d59ee7a6-3dfa-44c7-8f00-0183137c4d31.jpg?1783930500"
        ruling("2020-06-23", "If another creature dies at the same time as Spiteful Prankster, Spiteful Prankster's last ability triggers for that creature.")
        ruling("2020-06-23", "If your life total is brought to 0 or less at the same time that a creature is dealt lethal damage, you lose the game before Spiteful Prankster's ability goes on the stack.")
    }
}
