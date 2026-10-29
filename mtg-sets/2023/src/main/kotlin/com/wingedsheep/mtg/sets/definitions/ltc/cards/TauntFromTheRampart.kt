package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Taunt from the Rampart
 * {3}{R}{W}
 * Sorcery
 * Goad all creatures your opponents control. Until your next turn, those creatures can't block.
 *
 * "Those creatures" are the ones goaded as the spell resolves, so both halves run per creature
 * inside one group iteration over the opponents' creatures — a creature that arrives later is
 * neither goaded nor barred from blocking. The can't-block restriction lasts until the caster's
 * next turn, the same window as goad (CR 701.15a).
 */
val TauntFromTheRampart = card("Taunt from the Rampart") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Sorcery"
    oracleText = "Goad all creatures your opponents control. Until your next turn, those creatures " +
        "can't block. (Until your next turn, those creatures attack each combat if able and attack " +
        "a player other than you if able.)"

    spell {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesOpponentsControl,
            effect = Effects.Goad(EffectTarget.IterationEntity) then
                Effects.CantBlock(EffectTarget.IterationEntity, Duration.UntilYourNextTurn)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Lorenzo Mastroianni"
        flavorText = "\"Is it not said that no foe has ever taken the Hornburg, if men defended it?\"\n—Aragorn"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd1eaa0e-060d-40f1-84c5-f7e8df2f824c.jpg?1783916016"
        ruling("2023-06-16", "Being goaded isn't an ability the creature has. Once it's been goaded, it must attack as detailed above even if it loses all abilities.")
        ruling("2023-06-16", "Attacking with a goaded creature doesn't cause it to stop being goaded. If there is an additional combat phase that turn, or if another player gains control of it before it stops being goaded, it must attack again if able.")
        ruling("2023-06-16", "If a creature you control has been goaded by multiple opponents, it must attack one of your opponents that hasn't goaded it, as that fulfills the maximum number of goad requirements. If a creature you control has been goaded by each of your opponents, you choose which opponent it attacks.")
    }
}
