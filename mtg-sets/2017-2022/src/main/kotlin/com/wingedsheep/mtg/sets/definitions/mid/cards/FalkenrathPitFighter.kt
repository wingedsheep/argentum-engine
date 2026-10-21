package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Falkenrath Pit Fighter
 * {R}
 * Creature — Vampire Warrior
 * 2/1
 * {1}{R}, Discard a card, Sacrifice a Vampire: Draw two cards. Activate only if an opponent lost
 * life this turn.
 *
 * The sacrifice may be the Pit Fighter itself (it is a Vampire). The activation gate is
 * [ActivationRestriction.OnlyIfCondition] over [Conditions.OpponentLostLifeThisTurn].
 */
val FalkenrathPitFighter = card("Falkenrath Pit Fighter") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Warrior"
    power = 2
    toughness = 1
    oracleText = "{1}{R}, Discard a card, Sacrifice a Vampire: Draw two cards. Activate only if an " +
        "opponent lost life this turn."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{R}"),
            Costs.DiscardCard,
            // A bare tribal noun names permanents of that tribe.
            Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype(Subtype.VAMPIRE))
        )
        effect = Effects.DrawCards(2)
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.OpponentLostLifeThisTurn))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Anna Fehr"
        flavorText = "In the arena, vampires set aside all pretense of honor or civilization and fight " +
            "like the predators they know themselves to be."
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a743a3b6-3278-4644-a2d5-acf495c273ed.jpg"
    }
}
