package com.wingedsheep.mtg.sets.definitions.eoc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Planetary Annihilation — Edge of Eternities Commander #12 (canonical printing)
 * {3}{R}{R} · Sorcery
 *
 * Each player chooses six lands they control, then sacrifices the rest. Planetary Annihilation
 * deals 6 damage to each creature.
 *
 * Balance's land step with a fixed six: `forEachPlayerCollecting(ActivePlayerFirst)` has every
 * player, in APNAP order, keep exactly six of their own lands (`ChooseExactly` clamps to the pool,
 * so a player with six or fewer keeps them all), and the unkept lands across the table are
 * sacrificed in one simultaneous move. Then 6 damage is dealt to each creature.
 */
val PlanetaryAnnihilation = card("Planetary Annihilation") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Each player chooses six lands they control, then sacrifices the rest. " +
        "Planetary Annihilation deals 6 damage to each creature."

    spell {
        effect = Effects.Pipeline {
            val (landsToSacrifice) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val lands = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Land))
                val (_, rest) = chooseExactlySplit(
                    6,
                    from = lands,
                    chooser = Chooser.Controller,
                    selectedLabel = "Keep",
                    remainderLabel = "Sacrifice",
                    prompt = "Choose six lands you control to keep; the rest are sacrificed.",
                    useTargetingUI = true
                )
                listOf(rest)
            }
            sacrifice(landsToSacrifice)
        } then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature),
            Effects.DealDamage(6, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Cristi Balanescu"
        flavorText = "\"The better you understand how a planet is made, the better you understand how one is unmade.\"\n" +
            "—Ulix, Eumidian terrademolitionist"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8695796f-b6ed-46db-b1b8-07ed0cc5a5e5.jpg?1783906064"
    }
}
