package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Illusionist's Stratagem
 * {3}{U}
 * Instant
 * Exile up to two target creatures you control, then return those cards to the battlefield under
 * their owner's control.
 * Draw a card.
 *
 * Ghostly Flicker's linked-exile blink: the chosen targets are exiled together (linked to this
 * spell), then the whole batch returns at once under each card's owner's control (CR 610.3c), as
 * new objects (CR 400.7). An illegal target is simply not exiled while the other is still
 * flickered; a token ceases to exist in exile and never returns. Zero targets is legal — the spell
 * then just draws a card.
 */
val IllusionistsStratagem = card("Illusionist's Stratagem") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Exile up to two target creatures you control, then return those cards to the " +
        "battlefield under their owner's control.\nDraw a card."

    spell {
        targets(TargetFilter.Creature.youControl(), count = 2, optional = true)
        effect = Effects.Pipeline(
            descriptionOverride = "Exile up to two target creatures you control, then return those " +
                "cards to the battlefield under their owner's control"
        ) {
            val chosen = gather(source = CardSource.ChosenTargets)
            exile(chosen, linkToSource = true)
            val returning = gather(source = CardSource.FromLinkedExile())
            move(returning, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
        } then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Svetlin Velinov"
        flavorText = "\"When everything hangs on a single moment, be sure you choose the right moment.\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7eede83-6f1b-4054-b01c-0da17b197bad.jpg?1783936772"
        ruling(
            "2017-02-09",
            "Each card that returns to the battlefield will be a new object with no connection to the creature " +
                "that was exiled. It won't be in combat or have any additional abilities it may have had when it " +
                "was exiled. Any +1/+1 counters on it or Auras attached to it are removed, and any Equipment will " +
                "no longer be attached."
        )
        ruling(
            "2017-02-09",
            "If a creature token is exiled this way, it will cease to exist and will not return to the battlefield."
        )
        ruling(
            "2017-02-09",
            "If you choose two target creatures and one is an illegal target as Illusionist's Stratagem resolves, " +
                "you'll exile the other, return it, and draw a card."
        )
        ruling(
            "2017-02-09",
            "If each target creature is an illegal target as Illusionist's Stratagem resolves, the spell doesn't " +
                "resolve. You won't draw a card."
        )
        ruling(
            "2017-02-09",
            "You may cast Illusionist's Stratagem without any targets if you wish to just draw a card."
        )
    }
}
