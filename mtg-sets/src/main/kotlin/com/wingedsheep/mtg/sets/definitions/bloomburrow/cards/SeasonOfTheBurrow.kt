package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.BudgetModalEffect
import com.wingedsheep.sdk.scripting.effects.BudgetMode
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Season of the Burrow
 * {3}{W}{W}
 * Sorcery
 *
 * Choose up to five {P} worth of modes. You may choose the same mode more than once.
 * {P} — Create a 1/1 white Rabbit creature token.
 * {P}{P} — Exile target nonland permanent. Its controller draws a card.
 * {P}{P}{P} — Return target permanent card with mana value 3 or less from your graveyard
 *             to the battlefield with an indestructible counter on it.
 */
val SeasonOfTheBurrow = card("Season of the Burrow") {
    manaCost = "{3}{W}{W}"
    typeLine = "Sorcery"
    oracleText = "Choose up to five {P} worth of modes. You may choose the same mode more than once.\n" +
        "{P} — Create a 1/1 white Rabbit creature token.\n" +
        "{P}{P} — Exile target nonland permanent. Its controller draws a card.\n" +
        "{P}{P}{P} — Return target permanent card with mana value 3 or less from your graveyard to the battlefield with an indestructible counter on it."

    spell {
        effect = BudgetModalEffect(
            budget = 5,
            modes = listOf(
                // {P} — Create a 1/1 white Rabbit creature token
                BudgetMode(
                    cost = 1,
                    effect = Effects.CreateToken(
                        power = 1,
                        toughness = 1,
                        colors = setOf(Color.WHITE),
                        creatureTypes = setOf("Rabbit"),
                        imageUri = "https://cards.scryfall.io/normal/front/8/1/81de52ef-7515-4958-abea-fb8ebdcef93c.jpg?1721431122"
                    ),
                    description = "Create a 1/1 white Rabbit creature token"
                ),
                // {P}{P} — Exile target nonland permanent. Its controller draws a card.
                BudgetMode(
                    cost = 2,
                    effect = SelectTargetEffect(
                        requirement = TargetObject(filter = TargetFilter.NonlandPermanent),
                        storeAs = "exileTarget"
                    )
                        .then(Effects.Exile(EffectTarget.PipelineTarget("exileTarget")))
                        .then(DrawCardsEffect(
                            count = DynamicAmount.Fixed(1),
                            target = EffectTarget.ControllerOfPipelineTarget("exileTarget")
                        )),
                    description = "Exile target nonland permanent. Its controller draws a card"
                ),
                // {P}{P}{P} — Return target permanent card with MV 3 or less from your graveyard
                //             to the battlefield with an indestructible counter on it
                BudgetMode(
                    cost = 3,
                    effect = SelectTargetEffect(
                        requirement = TargetObject(
                            filter = TargetFilter(
                                GameObjectFilter.Permanent.manaValueAtMost(3).ownedByYou(),
                                zone = Zone.GRAVEYARD
                            )
                        ),
                        storeAs = "returnTarget"
                    )
                        .then(Effects.PutOntoBattlefield(EffectTarget.PipelineTarget("returnTarget")))
                        .then(Effects.AddCounters(Counters.INDESTRUCTIBLE, 1, EffectTarget.PipelineTarget("returnTarget"))),
                    description = "Return target permanent card with mana value 3 or less from your graveyard to the battlefield with an indestructible counter on it"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "29"
        artist = "Serena Malyon"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33bf9c60-4e58-48a4-8e53-abef7ab3b671.jpg?1721425932"
        ruling("2024-07-26", "You don't have to choose modes that add up to exactly five pawprints.")
        ruling("2024-07-26", "If a mode requires a target, you can select that mode only if there's a legal target available.")
        ruling("2024-07-26", "No matter which combination of modes you choose, you always follow the instructions of a Season in the order they are written.")
        ruling("2024-07-26", "If all targets for the chosen modes become illegal before the Season resolves, the spell won't resolve and none of its effects will happen.")
    }
}
