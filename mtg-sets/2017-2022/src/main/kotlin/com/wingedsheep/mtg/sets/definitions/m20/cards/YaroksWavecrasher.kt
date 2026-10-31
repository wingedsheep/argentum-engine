package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Yarok's Wavecrasher — Core Set 2020 #81
 * {3}{U} · Creature — Elemental 4/4 · Uncommon
 *
 * When this creature enters, return another creature you control to its owner's hand.
 *
 * Loyal Gryff's untargeted bounce without the "may": a gather → `chooseExactly(1)` → `toHand`
 * pipeline, chosen on resolution. With no other creature, nothing is chosen and nothing moves
 * (2019-07-12 ruling).
 */
val YaroksWavecrasher = card("Yarok's Wavecrasher") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    oracleText = "When this creature enters, return another creature you control to its owner's hand."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = Player.You,
                    excludeSelf = true
                )
            )
            val bounced = chooseExactly(
                1,
                from = candidates,
                prompt = "Return another creature you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(bounced)
        }
        description = "When this creature enters, return another creature you control to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Randy Vargas"
        flavorText = "\"Yarok's waters rush and rage\nWhere armies bled into the sand.\"\n" +
            "—Lament for Bala Ged, stanza 2"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c86a8974-df86-43ee-b07f-fd2837c08a6a.jpg?1783933002"
        ruling(
            "2019-07-12",
            "If you control no other creatures when the triggered ability resolves, you simply don't " +
                "return anything. There's no penalty for being unable to do so."
        )
    }
}
