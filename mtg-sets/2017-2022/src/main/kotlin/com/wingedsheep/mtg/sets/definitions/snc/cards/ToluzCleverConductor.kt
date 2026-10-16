package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Toluz, Clever Conductor (SNC #228)
 * {W/U}{U}{U/B}  Legendary Creature — Human Rogue  3/1
 *
 * When Toluz enters, it connives.
 * Whenever you discard one or more cards, exile them from your graveyard.
 * When Toluz dies, put the cards exiled with it into their owner's hand.
 *
 * Modeling: the batch discard trigger carries the discarded cards as its captured collection;
 * narrowed to those still in a graveyard (a card a madness discard sent to exile, or one moved
 * out in response, is no longer "in your graveyard"), they are exiled linked to Toluz so the dies
 * trigger can find them through [CardSource.FromLinkedExile].
 */
val ToluzCleverConductor = card("Toluz, Clever Conductor") {
    manaCost = "{W/U}{U}{U/B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Creature — Human Rogue"
    power = 3
    toughness = 1
    oracleText = "When Toluz enters, it connives. (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)\n" +
        "Whenever you discard one or more cards, exile them from your graveyard.\n" +
        "When Toluz dies, put the cards exiled with it into their owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Connive()
    }

    triggeredAbility {
        trigger = Triggers.you.discards(batch = true)
        effect = Effects.Pipeline {
            val discarded = filter(triggerCaptured, GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD))
            exile(discarded, linkToSource = true)
        }
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.FromLinkedExile())
            toHand(exiled)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f01f12e0-f354-43aa-9e2d-b59a99571a5f.jpg?1783923066"
    }
}
