package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Novice Knight — Core Set 2019 #30
 * {W} · Creature — Human Knight · 2/3
 *
 * Defender
 * As long as this creature is enchanted or equipped, it can attack as though it didn't have defender.
 *
 * "Enchanted or equipped" is the Aura/Equipment-agnostic IsEnchanted / IsEquipped pair, so the
 * attachment needn't be controlled by Novice Knight's controller.
 */
val NoviceKnight = card("Novice Knight") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 3
    oracleText = "Defender (This creature can't attack.)\n" +
        "As long as this creature is enchanted or equipped, it can attack as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    staticAbility {
        ability = CanAttackDespiteDefender(
            condition = Conditions.SourceMatches(
                GameObjectFilter.Any.withStatePredicate(
                    StatePredicate.Or(listOf(StatePredicate.IsEnchanted, StatePredicate.IsEquipped))
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Yongjae Choi"
        flavorText = "Even the greatest hero begins with nothing."
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00101358-0e89-4bd1-b1f2-e889645b616e.jpg?1783934599"
        ruling("2018-07-13", "Once Novice Knight has attacked, it will remain in combat even if it's no longer enchanted or equipped.")
    }
}
