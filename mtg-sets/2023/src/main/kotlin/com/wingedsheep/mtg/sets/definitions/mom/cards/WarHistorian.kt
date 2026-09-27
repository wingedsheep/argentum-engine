package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * War Historian — March of the Machine #214
 * {2}{G} · Creature — Human Monk · 3/3
 *
 * Reach
 * This creature has indestructible as long as it attacked a battle this turn.
 *
 * `attackedABattleThisTurn()` is stamped at declaration and lasts the turn, so — per the ruling —
 * the battle being defeated or the creature leaving combat doesn't switch it off.
 */
val WarHistorian = card("War Historian") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Monk"
    oracleText = "Reach\nThis creature has indestructible as long as it attacked a battle this turn."
    power = 3
    toughness = 3

    keywords(Keyword.REACH)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.source()),
            condition = Conditions.SourceMatches(GameObjectFilter.Any.attackedABattleThisTurn())
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "214"
        artist = "Ryan Valle"
        flavorText = "All children on Kamigawa learn of the Kami War. Hostilities with beings from another reality were understood, and no time was wasted in disbelief."
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0007efdf-417d-48a7-b119-3a2fda3e1158.jpg?1783916958"
        ruling(
            "2023-04-14",
            "War Historian's last ability starts to apply as soon as it's declared as an attacker " +
                "that's attacking a battle, and it applies for the entire turn. It doesn't matter " +
                "what happens to the battle after that point."
        )
    }
}
