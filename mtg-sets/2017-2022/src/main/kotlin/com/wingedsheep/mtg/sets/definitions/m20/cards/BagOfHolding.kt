package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bag of Holding — Core Set 2020 #222
 * {1} · Artifact · Rare
 *
 * Whenever you discard a card, exile that card from your graveyard.
 * {2}, {T}: Draw a card, then discard a card.
 * {4}, {T}, Sacrifice this artifact: Return all cards exiled with this artifact to their owner's hand.
 *
 * Modelling notes:
 * - The discard trigger fires once per discarded card and binds it as the triggering entity.
 *   `fromZone = GRAVEYARD` makes the exile a no-op if the card already left the graveyard
 *   (first ruling), and `linkToSource = true` ties it to *this* Bag — so a second Bag can't
 *   return it (CR 607 linked abilities; second ruling).
 * - The return gathers the linked exile pile; the Bag is sacrificed as a cost, and the pile is
 *   keyed to the object that exiled the cards (same shape as Synod Sanctum / Hoarding Dragon).
 */
val BagOfHolding = card("Bag of Holding") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever you discard a card, exile that card from your graveyard.\n" +
        "{2}, {T}: Draw a card, then discard a card.\n" +
        "{4}, {T}, Sacrifice this artifact: Return all cards exiled with this artifact to their " +
        "owner's hand."

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.Move(
            EffectTarget.TriggeringEntity,
            Zone.EXILE,
            fromZone = Zone.GRAVEYARD,
            linkToSource = true,
        )
        description = "Whenever you discard a card, exile that card from your graveyard."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Patterns.Hand.loot()
        description = "{2}, {T}: Draw a card, then discard a card."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val bagContents = gather(CardSource.FromLinkedExile())
            toHand(bagContents)
        }
        description = "{4}, {T}, Sacrifice this artifact: Return all cards exiled with this " +
            "artifact to their owner's hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Dmitry Burmak"
        flavorText = "There's no prepared like overprepared."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49283832-54f2-4619-b4a9-750493c93292.jpg?1783932947"
        ruling("2019-07-12", "If you discard a card but that card is not in your graveyard as Bag of Holding's first ability resolves, that card remains wherever it has moved.")
        ruling("2019-07-12", "If you control more than one Bag of Holding, you choose which one will hold the discarded card. Other Bags of Holding can't return that card.")
        ruling("2019-07-12", "You both draw and discard while Bag of Holding's second ability is resolving. No player may take any action—nor can anything else happen—until you've both drawn and discarded.")
        ruling("2019-07-12", "If Bag of Holding is moved to exile when you sacrifice it (most likely due to Leyline of the Void's effect), it remains in exile. It won't be returned to your hand.")
        ruling("2019-07-12", "If Bag of Holding leaves the battlefield, the items it contained are exiled forever (and, perhaps, scattered throughout the Astral Plane). If the same Bag of Holding card returns to the battlefield, it's considered a new object without access to the cards stored by the old object.")
    }
}
