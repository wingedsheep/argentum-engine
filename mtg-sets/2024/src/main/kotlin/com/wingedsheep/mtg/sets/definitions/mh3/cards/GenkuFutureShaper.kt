package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Genku, Future Shaper
 * {2}{W}{U}
 * Legendary Creature — Moonfolk Wizard
 * 2/5
 *
 * Whenever another nontoken permanent you control leaves the battlefield, choose one that hasn't
 * been chosen this turn. Create a creature token with those characteristics.
 * • 2/2 white Fox with vigilance.
 * • 1/2 blue Moonfolk with flying.
 * • 1/1 black Rat with lifelink.
 * {3}{W}{U}: Put a +1/+1 counter on each creature you control.
 *
 * "Choose one that hasn't been chosen this turn" is [ModalEffect.chooseOneNotYetChosenThisTurn]
 * (per-source turn memory, cleared at cleanup; with every mode spent the trigger does nothing),
 * the same shape as Breeches, Eager Pillager.
 */
val GenkuFutureShaper = card("Genku, Future Shaper") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Moonfolk Wizard"
    power = 2
    toughness = 5
    oracleText = "Whenever another nontoken permanent you control leaves the battlefield, choose one " +
        "that hasn't been chosen this turn. Create a creature token with those characteristics.\n" +
        "• 2/2 white Fox with vigilance.\n" +
        "• 1/2 blue Moonfolk with flying.\n" +
        "• 1/1 black Rat with lifelink.\n" +
        "{3}{W}{U}: Put a +1/+1 counter on each creature you control."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.nontoken().youControl()).leaves()
        effect = ModalEffect.chooseOneNotYetChosenThisTurn(
            Mode.noTarget(
                Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Fox"),
                    keywords = setOf(Keyword.VIGILANCE),
                    imageUri = "https://cards.scryfall.io/normal/front/d/3/d31d906e-e5c2-48ad-b7b0-f740ea4447ff.jpg?1783911117"
                ),
                "2/2 white Fox with vigilance"
            ),
            Mode.noTarget(
                Effects.CreateToken(
                    power = 1,
                    toughness = 2,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Moonfolk"),
                    keywords = setOf(Keyword.FLYING),
                    imageUri = "https://cards.scryfall.io/normal/front/1/0/10136b4b-ccc1-48ce-bc8c-295660464cf7.jpg?1783911115"
                ),
                "1/2 blue Moonfolk with flying"
            ),
            Mode.noTarget(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Rat"),
                    keywords = setOf(Keyword.LIFELINK),
                    imageUri = "https://cards.scryfall.io/normal/front/f/3/f3054ecd-0f8a-439d-ab7c-129d5aa0ccb2.jpg?1783911115"
                ),
                "1/1 black Rat with lifelink"
            ),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}{W}{U}")
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "186"
        artist = "Chuck Lukacs"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cbcc962-65c4-496d-8034-75c8d204dba3.jpg?1783911250"

        ruling("2024-06-07", "If multiple other nontoken permanents you control leave the battlefield simultaneously, you must still choose different modes for each instance of the triggered ability that's put onto the stack. If more than three other nontoken permanents you control leave the battlefield simultaneously, that choice is made only for the first three.")
        ruling("2024-06-07", "If Genku and one or more other nontoken permanents you control leave the battlefield at the same time, Genku's ability will trigger for each of those other nontoken permanents.")
        ruling("2024-06-07", "If you can't legally choose a mode because all three have been chosen that turn, that instance of the ability is removed from the stack with no effect.")
        ruling("2024-06-07", "If you somehow control two or more Genku, Future Shapers, track which modes have been chosen each turn for each one's ability separately.")
    }
}
