package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Colossus of Akros — Theros #214
 * {8} · Artifact Creature — Golem · 10/10
 *
 * Defender, indestructible
 * {10}: Monstrosity 10.
 * As long as this creature is monstrous, it has trample and can attack as though it didn't have defender.
 *
 * The monstrous clause is two self-scoped statics behind [Conditions.SourceIsMonstrous]: the trample
 * grant and the defender bypass. Defender itself is never removed (per the 2013-09-15 ruling).
 */
val ColossusOfAkros = card("Colossus of Akros") {
    manaCost = "{8}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    power = 10
    toughness = 10
    oracleText = "Defender, indestructible\n" +
        "{10}: Monstrosity 10. (If this creature isn't monstrous, put ten +1/+1 counters on it and it becomes monstrous.)\n" +
        "As long as this creature is monstrous, it has trample and can attack as though it didn't have defender."

    keywords(Keyword.DEFENDER, Keyword.INDESTRUCTIBLE)

    activatedAbility {
        cost = Costs.Mana("{10}")
        effect = Effects.Monstrosity(10)
    }

    staticAbility {
        ability = GrantKeyword(keyword = Keyword.TRAMPLE.name, filter = GroupFilter.source())
        condition = Conditions.SourceIsMonstrous
    }

    staticAbility {
        ability = CanAttackDespiteDefender(condition = Conditions.SourceIsMonstrous)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e394ffe-7460-4c59-a403-e9404b0d10c1.jpg?1783939720"
        ruling("2013-09-15", "Colossus of Akros doesn't lose defender when it's monstrous. It's just able to attack.")
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
    }
}
