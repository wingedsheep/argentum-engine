package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Stoneshock Giant — Theros #142
 * {3}{R}{R} · Creature — Giant · 5/4
 *
 * {6}{R}{R}: Monstrosity 3.
 * When this creature becomes monstrous, creatures without flying your opponents control can't block
 * this turn.
 *
 * The can't-block is a floating group restriction ([Effects.CantBlockGroup]), re-evaluated when
 * blockers are declared — per the ruling, an opponent's creature that loses flying or enters later
 * this turn can't block either.
 */
val StoneshockGiant = card("Stoneshock Giant") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    power = 5
    toughness = 4
    oracleText = "{6}{R}{R}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)\n" +
        "When this creature becomes monstrous, creatures without flying your opponents control can't block this turn."

    activatedAbility {
        cost = Costs.Mana("{6}{R}{R}")
        effect = Effects.Monstrosity(3)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesMonstrous()
        effect = Effects.CantBlockGroup(
            GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING).opponentControls())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d890801-5ffa-48af-959f-72e5f371fc61.jpg?1783939754"
        ruling("2013-09-15", "If blockers have already been declared when Stoneshock Giant's last ability resolves, those blocks won't change or become undone.")
        ruling("2013-09-15", "Your opponents can't assign any creature without flying to block that turn, even if that creature had flying or wasn't on the battlefield under one of your opponent's control when Stoneshock Giant's ability resolved.")
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
