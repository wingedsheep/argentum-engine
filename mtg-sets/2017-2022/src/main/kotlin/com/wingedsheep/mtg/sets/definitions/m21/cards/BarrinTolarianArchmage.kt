package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Barrin, Tolarian Archmage
 * {1}{U}{U}
 * Legendary Creature — Human Wizard
 * 2/2
 * When Barrin enters, return up to one other target creature or planeswalker to its owner's hand.
 * At the beginning of your end step, if a permanent was put into your hand from the battlefield
 * this turn, draw a card.
 *
 * The end-step "if" is an intervening-if (CR 603.4) over the whole turn — tokens count, and the
 * permanent needn't still be in hand (rulings).
 *
 * Canonical printing: Core Set 2021. Reprinted in J22 as a `Printing` row.
 */
val BarrinTolarianArchmage = card("Barrin, Tolarian Archmage") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "When Barrin enters, return up to one other target creature or planeswalker to its " +
        "owner's hand.\nAt the beginning of your end step, if a permanent was put into your hand from " +
        "the battlefield this turn, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker).other(), optional = true)
        effect = Effects.ReturnToHand(permanent)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.PermanentPutIntoYourHandFromBattlefieldThisTurn
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Ryan Pancoast"
        flavorText = "\"There is no age at which you stop learning.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb078fbb-beb9-4c0b-be93-ed1e73e6f8d8.jpg?1783930730"
        ruling(
            "2020-06-23",
            "You draw only one card, no matter how many permanents were put into your hand from the " +
                "battlefield during that turn."
        )
        ruling("2020-06-23", "The permanent that was returned to your hand doesn't have to still be there.")
        ruling("2020-06-23", "If a token is returned to your hand, it's put there before it ceases to exist.")
        ruling(
            "2020-06-23",
            "Barrin's last ability checks the entire turn, even before Barrin was on the battlefield."
        )
        ruling(
            "2020-06-23",
            "If a permanent wasn't put into your hand from the battlefield before your end step begins, " +
                "Barrin's ability doesn't trigger at all."
        )
    }
}
