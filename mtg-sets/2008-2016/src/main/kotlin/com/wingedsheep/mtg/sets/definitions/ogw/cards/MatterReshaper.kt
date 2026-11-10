package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Matter Reshaper
 * {2}{C}
 * Creature — Eldrazi
 * 3/2
 * ({C} represents colorless mana.)
 * When this creature dies, reveal the top card of your library. You may put that card onto the
 * battlefield if it's a permanent card with mana value 3 or less. Otherwise, put that card into
 * your hand.
 *
 * The reveal is Aid from the Cowl's pipeline: an up-to-one split filtered to permanent cards with
 * mana value 3 or less, moved onto the battlefield. "Otherwise" is Rashmi's tail — per the ruling,
 * the card goes to your hand if you don't put it onto the battlefield *for any reason*: it isn't an
 * eligible permanent card, you decline, or it's an Aura with nothing to enchant (which stays where
 * it is, CR 303.4g). Whatever is still in the library afterwards goes to your hand.
 */
val MatterReshaper = card("Matter Reshaper") {
    manaCost = "{2}{C}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    power = 3
    toughness = 2
    oracleText = "({C} represents colorless mana.)\n" +
        "When this creature dies, reveal the top card of your library. You may put that card onto the " +
        "battlefield if it's a permanent card with mana value 3 or less. Otherwise, put that card into your hand."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1), revealed = true)
            val (toBattlefield, _) = chooseUpToSplit(
                1,
                from = revealed,
                filter = GameObjectFilter.Permanent.manaValueAtMost(3),
                prompt = "Put the revealed card onto the battlefield? Otherwise it goes to your hand.",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Put into your hand"
            )
            move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD))
            // "Otherwise, put that card into your hand." — anything not put onto the battlefield.
            toHand(filter(revealed, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY)))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/3906b61a-3865-4dfd-ae06-a7d2a608851a.jpg?1783937929"
        ruling("2016-01-22", "If you put an Aura card onto the battlefield this way, you choose what it enchants as it enters the battlefield. You must be able to choose a legal player or permanent or you can't put the Aura onto the battlefield.")
        ruling("2016-01-22", "If you don't put the card onto the battlefield for any reason, you put the card into your hand.")
    }
}
