package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Galadriel, Light of Valinor
 * {2}{G}{W}{U}
 * Legendary Creature — Elf Noble
 * 3/3
 * Alliance — Whenever another creature you control enters, choose one that hasn't been chosen
 * this turn —
 * • Add {G}{G}{G}.
 * • Put a +1/+1 counter on each creature you control.
 * • Scry 2, then draw a card.
 *
 * The turn-scoped "hasn't been chosen this turn" modal (Monument to Endurance, Breeches) keeps
 * its memory on this Galadriel object, so a re-entered Galadriel starts fresh and a new controller
 * inherits the choices already made (2023-11-03 rulings). Once all three modes are used, later
 * triggers resolve with no effect.
 */
val GaladrielLightOfValinor = card("Galadriel, Light of Valinor") {
    manaCost = "{2}{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Elf Noble"
    power = 3
    toughness = 3
    oracleText = "Alliance — Whenever another creature you control enters, choose one that " +
        "hasn't been chosen this turn —\n" +
        "• Add {G}{G}{G}.\n" +
        "• Put a +1/+1 counter on each creature you control.\n" +
        "• Scry 2, then draw a card."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = ModalEffect.chooseOneNotYetChosenThisTurn(
            Mode.noTarget(Effects.AddMana(Color.GREEN, 3), "Add {G}{G}{G}"),
            Mode.noTarget(
                Effects.ForEachInGroup(
                    GroupFilter.AllCreaturesYouControl,
                    Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
                ),
                "Put a +1/+1 counter on each creature you control"
            ),
            Mode.noTarget(
                Patterns.Library.scry(2) then Effects.DrawCards(1),
                "Scry 2, then draw a card"
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "498"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f485596f-3a23-4714-ad7e-4c493ab1b530.jpg?1783914199"
        ruling("2023-11-03", "If multiple creatures enter the battlefield simultaneously, you must still choose different modes for each instance of the triggered ability that's put onto the stack. If more than three creatures enter the battlefield simultaneously, that choice is made only for the first three.")
        ruling("2023-11-03", "The phrase \"that hasn't been chosen this turn\" refers only to that specific Galadriel, Light of Valinor. If Galadriel leaves the battlefield and then returns to the battlefield later in the turn, it will be a new object with no memory of the modes chosen when it was previously on the battlefield.")
        ruling("2023-11-03", "If all three modes have been chosen this turn, Galadriel, Light of Valinor's triggered ability is removed from the stack with no effect.")
        ruling("2023-11-03", "It doesn't matter who has chosen any particular mode. For example, say you control Galadriel, Light of Valinor and have chosen the first two modes this turn. If an opponent gains control of Galadriel, that player can choose only the third mode this turn.")
    }
}
