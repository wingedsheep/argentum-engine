package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Blood Age General — Strixhaven: School of Mages #93 (canonical printing)
 * {1}{R} · Creature — Spirit Warrior · 2/2
 *
 * {T}: Attacking Spirits get +1/+0 until end of turn.
 *
 * One [Patterns.Group.modifyStatsForAll] pass over every attacking Spirit (not only yours — the
 * text says "Attacking Spirits"). The group is gathered as the ability resolves, so a Spirit that
 * attacks later this turn gets nothing, and a Spirit that stops attacking keeps the bonus — both
 * per the rulings.
 */
val BloodAgeGeneral = card("Blood Age General") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Spirit Warrior"
    oracleText = "{T}: Attacking Spirits get +1/+0 until end of turn."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Group.modifyStatsForAll(
            power = 1,
            toughness = 0,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Spirit").attacking())
        )
        description = "{T}: Attacking Spirits get +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Johan Grenier"
        flavorText = "His troops took the Fortress of Caerdoon centuries ago, yet he fights on."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f30237cc-27c8-4075-acbd-d75a16588a68.jpg?1783927359"
        ruling("2021-04-16", "Blood Age General's activated ability affects only Spirits that are attacking as it resolves. If you activate it while no Spirits are attacking, it will have no effect, even if you attack with one or more Spirits later in the turn.")
        ruling("2021-04-16", "Once the activated ability resolves, Spirits that get the +1/+0 bonus will retain it until end of turn, even after they stop attacking or if they stop being Spirits.")
    }
}
