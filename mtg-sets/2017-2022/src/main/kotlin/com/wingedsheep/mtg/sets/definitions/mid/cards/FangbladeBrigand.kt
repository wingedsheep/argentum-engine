package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fangblade Brigand // Fangblade Eviscerator (Innistrad: Midnight Hunt)
 * {3}{R}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Fangblade Brigand (3/4): "{1}{R}: This creature gets +1/+0 and gains first strike until
 *          end of turn." Daybound.
 * Back  — Fangblade Eviscerator (4/5): the same pump, plus "{4}{R}: Creatures you control get +2/+0
 *          until end of turn." Nightbound.
 */

private const val PUMP_TEXT = "{1}{R}: This creature gets +1/+0 and gains first strike until end of turn."
private const val TEAM_PUMP_TEXT = "{4}{R}: Creatures you control get +2/+0 until end of turn."

private val FangbladeBrigandFront = card("Fangblade Brigand") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Werewolf"
    power = 3
    toughness = 4
    oracleText = "$PUMP_TEXT\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
        description = PUMP_TEXT
    }
    daybound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Vincent Proce"
        flavorText = "As long as the sun is up, you'll only lose your purse."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2feb859-bfae-4bc4-8181-5737dd5c3b08.jpg?1783925607"
    }
}

private val FangbladeEviscerator = card("Fangblade Eviscerator") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 5
    oracleText = "$PUMP_TEXT\n" +
        "$TEAM_PUMP_TEXT\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
        description = PUMP_TEXT
    }
    activatedAbility {
        cost = Costs.Mana("{4}{R}")
        effect = Patterns.Group.modifyStatsForAll(2, 0, GroupFilter(GameObjectFilter.Creature.youControl()))
        description = TEAM_PUMP_TEXT
    }
    nightbound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/back/d/2/d2feb859-bfae-4bc4-8181-5737dd5c3b08.jpg?1783925607"
    }
}

val FangbladeBrigand: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = FangbladeBrigandFront,
    backFace = FangbladeEviscerator,
)
