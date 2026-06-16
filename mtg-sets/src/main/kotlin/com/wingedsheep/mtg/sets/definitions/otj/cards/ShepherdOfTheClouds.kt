package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Shepherd of the Clouds
 * {4}{W}
 * Creature — Pegasus
 * 4/3
 *
 * Flying, vigilance
 * When this creature enters, return target permanent card with mana value 3 or less from your
 * graveyard to your hand. Return that card to the battlefield instead if you control a Mount.
 *
 * The destination is chosen at resolution: if you control a Mount when the ability resolves, the
 * targeted card enters the battlefield; otherwise it goes to your hand ([ConditionalEffect] gating
 * on [Conditions.YouControl] for a Mount — "control a Mount" is "control at least one"). "Permanent
 * card … from your graveyard" is a
 * single printed target restricted to permanent cards (type-line check) you own, of mana value 3
 * or less, in the graveyard zone.
 */
val ShepherdOfTheClouds = card("Shepherd of the Clouds") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Pegasus"
    power = 4
    toughness = 3
    oracleText = "Flying, vigilance\n" +
        "When this creature enters, return target permanent card with mana value 3 or less from " +
        "your graveyard to your hand. Return that card to the battlefield instead if you control a Mount."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val card = target(
            "card",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Permanent.ownedByYou().manaValueAtMost(3),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = ConditionalEffect(
            condition = Conditions.YouControl(
                GameObjectFilter.Creature.withSubtype(Subtype("Mount")),
            ),
            effect = Effects.PutOntoBattlefield(card),
            elseEffect = Effects.ReturnToHand(card),
        )
        description = "When this creature enters, return target permanent card with mana value 3 " +
            "or less from your graveyard to your hand. Return that card to the battlefield instead " +
            "if you control a Mount."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Valera Lutfullina"
        flavorText = "It catches those who are not ready to fall."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2245f36-2138-4f01-9b70-151137a5ac59.jpg?1712355336"
    }
}
