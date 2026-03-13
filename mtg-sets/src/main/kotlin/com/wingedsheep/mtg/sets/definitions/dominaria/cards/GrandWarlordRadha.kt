package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Player

/**
 * Grand Warlord Radha
 * {2}{R}{G}
 * Legendary Creature — Elf Warrior
 * 3/4
 * Haste
 * Whenever one or more creatures you control attack, add that much mana in any
 * combination of {R} and/or {G}. Until end of turn, you don't lose this mana as
 * steps and phases end.
 *
 * Note: The "don't lose this mana" clause is effectively a no-op in this engine
 * since mana pools are only emptied at end of turn, not between steps/phases.
 * The mana amount equals the number of attacking creatures you control at the time
 * the triggered ability resolves.
 */
val GrandWarlordRadha = card("Grand Warlord Radha") {
    manaCost = "{2}{R}{G}"
    typeLine = "Legendary Creature — Elf Warrior"
    power = 3
    toughness = 4
    oracleText = "Haste\nWhenever one or more creatures you control attack, add that much mana in any combination of {R} and/or {G}. Until end of turn, you don't lose this mana as steps and phases end."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = Effects.AddDynamicMana(
            amount = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.attacking()),
            allowedColors = setOf(Color.RED, Color.GREEN)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "195"
        artist = "Anna Steinbauer"
        flavorText = "\"The future is my gift to Keld.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/8/986981fa-a744-45d8-81c7-68ef335c79e2.jpg?1562739940"
        ruling("2018-04-27", "The amount of mana you'll add is the number of creatures you attack with. Creatures that are put onto the battlefield attacking before Radha's triggered ability resolves don't count, and creatures that attacked but left combat before the triggered ability resolves do count.")
        ruling("2018-04-27", "After Radha's triggered ability resolves, you can cast spells and activate abilities before blockers are declared.")
    }
}
