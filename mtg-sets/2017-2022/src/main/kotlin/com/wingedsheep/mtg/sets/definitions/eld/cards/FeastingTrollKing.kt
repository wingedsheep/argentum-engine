package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Feasting Troll King — Throne of Eldraine #152
 * {2}{G}{G}{G}{G} · Creature — Troll Noble · 7/6
 *
 * Vigilance, trample
 * When this creature enters, if you cast it from your hand, create three Food tokens.
 * Sacrifice three Foods: Return this card from your graveyard to the battlefield. Activate only
 * during your turn.
 *
 * The Food payoff is an intervening-if on [Conditions.WasCastFromHand] (Reiver Demon's shape), so
 * returning it from the graveyard makes no new Food. The recursion is a graveyard-zone activated
 * ability (Cauldron Familiar's shape) with a subtype-based three-Food sacrifice cost (Savvy Hunter's
 * shape) and the `OnlyDuringYourTurn` restriction.
 */
val FeastingTrollKing = card("Feasting Troll King") {
    manaCost = "{2}{G}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Troll Noble"
    power = 7
    toughness = 6
    oracleText = "Vigilance, trample\n" +
        "When this creature enters, if you cast it from your hand, create three Food tokens. " +
        "(They're artifacts with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Sacrifice three Foods: Return this card from your graveyard to the battlefield. " +
        "Activate only during your turn."

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCastFromHand
        effect = Effects.CreateFood(3)
    }

    activatedAbility {
        cost = Costs.SacrificeMultiple(3, GameObjectFilter.Any.withSubtype("Food"))
        activateFromZone = Zone.GRAVEYARD
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a6bb435-1205-416a-a5a0-ca6d37b4dcb2.jpg?1783932612"
        ruling(
            "2019-10-04",
            "If you put Feasting Troll King onto the battlefield from your hand without casting it, its " +
                "enters-the-battlefield ability won't trigger."
        )
        ruling(
            "2019-10-04",
            "If you cast a creature spell that enters the battlefield as a copy of Feasting Troll King, " +
                "such as Clone, the enters-the-battlefield ability will trigger."
        )
        ruling(
            "2024-11-08",
            "If an effect refers to a Food, it means any Food artifact, not just a Food artifact token."
        )
    }
}
