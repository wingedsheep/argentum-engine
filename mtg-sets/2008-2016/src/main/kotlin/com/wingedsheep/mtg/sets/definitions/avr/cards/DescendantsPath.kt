package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Descendants' Path — Avacyn Restored #173 (canonical printing)
 *
 * Upkeep trigger: reveal the top card; a creature card sharing a creature type with a creature you
 * control may be cast for free while the trigger resolves (Rashmi's shape). Any card still in the
 * library afterwards — not a creature, no shared type, or the cast declined — goes to the bottom.
 */
val DescendantsPath = card("Descendants' Path") {
    manaCost = "{2}{G}"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, reveal the top card of your library. If it's a creature " +
        "card that shares a creature type with a creature you control, you may cast it without paying its " +
        "mana cost. If you don't cast it, put it on the bottom of your library."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1, Player.You), revealed = true)
            val chosen = chooseSpell(
                from = revealed,
                filter = GameObjectFilter.Creature
                    .sharingCreatureTypeWithPermanentYouControl(GameObjectFilter.Creature),
                showAllCards = true,
                prompt = "You may cast the revealed card without paying its mana cost if it's a creature card " +
                    "that shares a creature type with a creature you control. Otherwise it goes to the bottom " +
                    "of your library.",
                selectedLabel = "Cast for free",
                remainderLabel = "Put on bottom",
            )
            run(Effects.CastFromCollectionWithoutPayingCost(from = chosen))
            // "If you don't cast it, put it on the bottom of your library." — a cast card has left the library.
            move(
                filter(revealed, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY)),
                CardDestination.ToZone(Zone.LIBRARY, Player.You, placement = ZonePlacement.Bottom),
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Terese Nielsen"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ada93208-5cda-4e2d-b9a7-15345e30b831.jpg?1783940670"
        ruling("2012-05-01", "If the card has {X} in its mana cost, you must choose 0 as its value.")
        ruling("2012-05-01", "If you cast a card \"without paying its mana cost,\" you can't pay any alternative costs. You can pay additional costs, such as kicker costs. If the card has mandatory additional costs, you must pay those.")
        ruling("2012-05-01", "If the revealed card is a creature card that shares a creature type with a creature you control, but you choose not to cast it, it's put on the bottom of your library. (This is a recent change.)")
    }
}
