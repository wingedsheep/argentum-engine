package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode

/**
 * Bomat Courier — Kaladesh #199
 * {1} · Artifact Creature — Construct · 1/1
 *
 * Haste
 * Whenever this creature attacks, exile the top card of your library face down. (You can't look at it.)
 * {R}, Discard your hand, Sacrifice this creature: Put all cards exiled with this creature into
 * their owners' hands.
 */
val BomatCourier = card("Bomat Courier") {
    manaCost = "{1}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 1
    oracleText = "Haste\n" +
        "Whenever this creature attacks, exile the top card of your library face down. (You can't look at it.)\n" +
        "{R}, Discard your hand, Sacrifice this creature: Put all cards exiled with this creature into their owners' hands."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(1)))
            exile(top, faceDown = FaceDownMode.HIDDEN, linkToSource = true)
        }
        description = "Whenever this creature attacks, exile the top card of your library face down."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.DiscardHand, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.FromLinkedExile())
            toHand(exiled)
        }
        description = "{R}, Discard your hand, Sacrifice this creature: Put all cards exiled with " +
            "this creature into their owners' hands."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "199"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/425bff89-ad15-4d22-bce9-a4a07dbafd87.jpg?1783937162"
        ruling("2016-09-20", "Each Bomat Courier you control has its own cache of cards. Bomat Courier's last ability only puts those cards into your hand, not those of any other Bomat Courier.")
        ruling("2016-09-20", "If Bomat Courier leaves the battlefield before you activate its last ability, any cards exiled by its triggered ability remain exiled face down for the rest of the game. If you somehow return the same Bomat Courier card to the battlefield, it will be a different object with no connection to those face-down cards.")
        ruling("2016-09-20", "You can pay the cost of “discard your hand” even if your hand contains zero cards.")
    }
}
