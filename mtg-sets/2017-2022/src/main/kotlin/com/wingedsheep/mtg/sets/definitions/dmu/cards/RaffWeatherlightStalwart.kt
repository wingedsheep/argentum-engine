package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Raff, Weatherlight Stalwart
 * {W}{U}
 * Legendary Creature — Human Wizard
 * 1/3
 * Whenever you cast an instant or sorcery spell, you may tap two untapped creatures you control.
 * If you do, draw a card.
 * {3}{W}{W}: Creatures you control get +1/+1 and gain vigilance until end of turn.
 *
 * "You may tap two untapped creatures you control. If you do" is an [Effects.MayPay] whose payable
 * cost is the Gather → Select-exactly-2 → Tap pipeline (same shape as Aziza, Mage Tower Captain and
 * Caparocti Sunborn). Raff itself may be one of the two, and summoning sickness doesn't matter — it
 * isn't a {T} cost. The pump is [Patterns.Group.pumpAndGrantToAll], which locks in the affected
 * creatures on resolution.
 */
val RaffWeatherlightStalwart = card("Raff, Weatherlight Stalwart") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "Whenever you cast an instant or sorcery spell, you may tap two untapped creatures " +
        "you control. If you do, draw a card.\n" +
        "{3}{W}{W}: Creatures you control get +1/+1 and gain vigilance until end of turn."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        val tapCost = Effects.Pipeline {
            val raffTapPool = gather(
                CardSource.ControlledPermanents(
                    player = Player.You,
                    filter = GameObjectFilter.Creature.untapped(),
                )
            )
            val raffToTap = chooseExactly(
                2,
                from = raffTapPool,
                prompt = "Tap two untapped creatures you control",
                useTargetingUI = true
            )
            run(Effects.TapCollection(raffToTap, tap = true))
        }
        effect = Effects.MayPay(
            cost = tapCost,
            then = Effects.DrawCards(1),
            descriptionOverride = "You may tap two untapped creatures you control. If you do, draw a card.",
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}{W}{W}")
        effect = Patterns.Group.pumpAndGrantToAll(
            power = 1,
            toughness = 1,
            keyword = Keyword.VIGILANCE,
            filter = Filters.Group.creaturesYouControl,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Eelis Kyttanen"
        flavorText = "\"This old ship has seen a lot, but she's not done yet.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc0cba4c-d9f0-421f-acf4-ab1adcd59c80.jpg?1783921279"
        ruling(
            "2022-09-09",
            "Raff, Weatherlight Stalwart's triggered ability resolves before the spell that caused it to " +
                "trigger. The ability will resolve even if that spell is countered."
        )
    }
}
