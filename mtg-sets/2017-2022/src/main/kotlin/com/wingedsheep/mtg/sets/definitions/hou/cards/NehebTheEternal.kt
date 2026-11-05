package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Neheb, the Eternal — Hour of Devastation #104 (canonical printing)
 * {3}{R}{R} · Legendary Creature — Zombie Minotaur Warrior · 4/6
 *
 * Afflict 3 (Whenever this creature becomes blocked, defending player loses 3 life.)
 * At the beginning of each of your postcombat main phases, add {R} for each 1 life your
 * opponents have lost this turn.
 *
 * The mana ability is a triggered ability that uses the stack (not a mana ability). The amount is
 * life *lost* this turn summed over every opponent — never netted against life gained. The mana
 * is ordinary mana: added at the start of the main phase, it stays until that phase ends.
 */
val NehebTheEternal = card("Neheb, the Eternal") {
    manaCost = "{3}{R}{R}"
    typeLine = "Legendary Creature — Zombie Minotaur Warrior"
    power = 4
    toughness = 6
    oracleText = "Afflict 3 (Whenever this creature becomes blocked, defending player loses 3 life.)\n" +
        "At the beginning of each of your postcombat main phases, add {R} for each 1 life your opponents have lost this turn."

    afflict(3)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.POSTCOMBAT_MAIN)
        effect = Effects.AddMana(Color.RED, DynamicAmounts.lifeLostThisTurn(Player.EachOpponent))
        description = "At the beginning of each of your postcombat main phases, add {R} for each 1 life " +
            "your opponents have lost this turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "104"
        artist = "Chris Rahn"
        flavorText = "\"This is what glory looks like.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54231832-d492-4812-b658-4ab9a30fefe2.jpg?1783936025"
        ruling("2017-07-14", "Because it triggers at the beginning of a phase, Neheb's last ability isn't a mana ability. It uses the stack and can be countered by effects that interact with triggered abilities, such as that of Nimble Obstructionist.")
        ruling("2017-07-14", "You get a postcombat main phase even if no creatures attacked during a turn. Neheb's last ability will trigger.")
        ruling("2017-07-14", "If an opponent loses life but Neheb leaves the battlefield before your postcombat main phase begins, its last ability doesn't trigger.")
        ruling("2017-07-14", "Neheb's ability checks only how much life opponents lost during the turn, not by how much their life total decreased compared to the start of the turn. For example, if an opponent lost 2 life and then gained 8 life before your postcombat main phase, you'll add {R}{R}.")
        ruling("2017-07-14", "If you somehow have more than two main phases in a turn, each main phase after your first one is a postcombat main phase, and Neheb's last ability triggers at the beginning of each of them.")
        ruling("2017-07-14", "If multiple creatures block a creature with afflict, afflict triggers only once.")
        ruling("2017-07-14", "Afflict causes the defending player to lose life; it's not damage or combat damage.")
        ruling("2017-07-14", "Afflict resolves before combat damage is dealt. If this loss of life brings a player to 0 life or less, that player loses the game immediately. A blocking creature with lifelink won't deal combat damage in time to save that player.")
        ruling("2017-07-14", "If an opponent loses life and subsequently loses the game before your postcombat main phase, Neheb's last ability counts that loss of life.")
        ruling("2023-07-28", "If a creature is attacking a planeswalker, that planeswalker's controller is the defending player. If a creature is attacking a battle, that battle's protector is the defending player.")
    }
}
