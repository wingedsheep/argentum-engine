package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Dreamshaper Shaman
 * {5}{R}
 * Enchantment Creature — Minotaur Shaman
 * 5/4
 * At the beginning of your end step, you may pay {2}{R} and sacrifice a nonland permanent. If you
 * do, reveal cards from the top of your library until you reveal a nonland permanent card. Put that
 * card onto the battlefield and the rest on the bottom of your library in a random order.
 *
 * Sokenzan Smelter's two-part [Effects.MayPay] (mana *and* a sacrifice, all or nothing — the
 * Shaman itself is a legal sacrifice, per the ruling), then Proteus Staff's reveal: gather until a
 * nonland permanent card, put it onto the battlefield, the rest to the bottom at random. An empty
 * search just puts every revealed card on the bottom.
 */
val DreamshaperShaman = card("Dreamshaper Shaman") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Minotaur Shaman"
    power = 5
    toughness = 4
    oracleText = "At the beginning of your end step, you may pay {2}{R} and sacrifice a nonland " +
        "permanent. If you do, reveal cards from the top of your library until you reveal a nonland " +
        "permanent card. Put that card onto the battlefield and the rest on the bottom of your " +
        "library in a random order."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.MayPay(
            cost = Effects.PayMana("{2}{R}") then
                Effects.SacrificeOwn(filter = GameObjectFilter.NonlandPermanent),
            then = Effects.Pipeline {
                val (_, revealed) = gatherUntilMatch(GameObjectFilter.NonlandPermanent)
                reveal(revealed)
                val (found, rest) = filterSplit(revealed, GameObjectFilter.NonlandPermanent)
                move(found, CardDestination.ToZone(Zone.BATTLEFIELD))
                toLibraryBottom(rest, order = CardOrder.Random)
            },
            descriptionOverride = "You may pay {2}{R} and sacrifice a nonland permanent. If you do, " +
                "reveal cards from the top of your library until you reveal a nonland permanent card. " +
                "Put that card onto the battlefield and the rest on the bottom of your library in a " +
                "random order."
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Steve Prescott"
        flavorText = "Magic is the true artist's medium."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6b04ec8-68c9-468c-b137-707db994af68.jpg?1783931556"
        ruling("2020-01-24", "Dreamshaper Shaman can be sacrificed to pay for the cost of its own triggered ability.")
        ruling(
            "2020-01-24",
            "You can't pay {2}{R} and not sacrifice a nonland permanent, or vice versa. The cost must " +
                "be paid in full or not at all."
        )
        ruling(
            "2020-01-24",
            "If there are no nonland cards remaining in your library, you just randomize your library " +
                "and continue playing."
        )
    }
}
