package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Charix, the Raging Isle — Zendikar Rising #49
 * {2}{U}{U} · Legendary Creature — Leviathan Crab · 0/17
 *
 * Spells your opponents cast that target Charix cost {2} more to cast.
 * {3}: Charix gets +X/-X until end of turn, where X is the number of Islands you control.
 *
 * The tax is Syr Elenora's shape (applies once however many times the spell targets Charix).
 * X is evaluated by `ModifyStats` as the ability resolves and locked into the floating effect,
 * per the ruling.
 */
val CharixTheRagingIsle = card("Charix, the Raging Isle") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Leviathan Crab"
    oracleText = "Spells your opponents cast that target Charix cost {2} more to cast.\n" +
        "{3}: Charix gets +X/-X until end of turn, where X is the number of Islands you control."
    power = 0
    toughness = 17

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCastTargeting(GroupFilter.source()),
            modification = CostModification.IncreaseGeneric(2),
        )
    }

    activatedAbility {
        val islands = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.withSubtype("Island")).count()
        cost = Costs.Mana("{3}")
        effect = Effects.ModifyStats(islands, -islands, EffectTarget.Self)
        description = "{3}: Charix gets +X/-X until end of turn, where X is the number of Islands you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Viktor Titov"
        flavorText = "Zendikar's seas are deeper than anyone can fathom."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e83a6804-1e3c-428c-af5e-1d56ba11c108.jpg?1783929402"
        ruling("2020-09-25", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases (such as that of Charix), then apply any cost reductions. The mana value of the spell is determined only by its mana cost, no matter what the total cost to cast the spell was.")
        ruling("2020-09-25", "An opponent's spell that targets Charix more than once costs only {2} more to cast.")
        ruling("2020-09-25", "Each time you activate Charix's last ability, the value of X for that instance of the ability is determined only as that ability resolves. Once that happens, the size of the power-and-toughness change for that instance of the ability won't change later in the turn, even if the number of Islands you control does change.")
    }
}
