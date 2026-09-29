package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ghalta and Mavren — March of the Machine #225
 * {3}{G}{G}{W}{W} · Legendary Creature — Dinosaur Vampire · 12/12
 *
 * Trample
 * Whenever you attack, choose one —
 * • Create a tapped and attacking X/X green Dinosaur creature token with trample, where X is the
 *   greatest power among other attacking creatures.
 * • Create X 1/1 white Vampire creature tokens with lifelink, where X is the number of other
 *   attacking creatures.
 *
 * "Whenever you attack" is the batch `Triggers.you.attacks()`. Both X values are read at
 * resolution (per ruling) as aggregates over attacking creatures excluding Ghalta and Mavren
 * itself — whether or not it is attacking. The Vampires are *not* tapped and attacking.
 */
val GhaltaAndMavren = card("Ghalta and Mavren") {
    manaCost = "{3}{G}{G}{W}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Dinosaur Vampire"
    power = 12
    toughness = 12
    oracleText = "Trample\n" +
        "Whenever you attack, choose one —\n" +
        "• Create a tapped and attacking X/X green Dinosaur creature token with trample, where X is the greatest power among other attacking creatures.\n" +
        "• Create X 1/1 white Vampire creature tokens with lifelink, where X is the number of other attacking creatures."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.attacks()
        val otherAttackers = DynamicAmounts.battlefield(
            Player.Each, GameObjectFilter.Creature.attacking(), excludeSelf = true
        )
        val greatestPower = otherAttackers.maxPower()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.CreateToken(
                    power = 0,
                    toughness = 0,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Dinosaur"),
                    keywords = setOf(Keyword.TRAMPLE),
                    tapped = true,
                    attacking = true,
                    dynamicPower = greatestPower,
                    dynamicToughness = greatestPower,
                    imageUri = "https://cards.scryfall.io/normal/front/5/0/50b6ea55-c976-40e7-aa09-5ba77688bfe9.jpg?1783916671",
                ),
                "Create a tapped and attacking X/X green Dinosaur creature token with trample, " +
                    "where X is the greatest power among other attacking creatures"
            ),
            Mode.noTarget(
                Effects.CreateToken(
                    count = otherAttackers.count(),
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Vampire"),
                    keywords = setOf(Keyword.LIFELINK),
                    imageUri = "https://cards.scryfall.io/normal/front/0/3/03a2f3c3-fbcb-4b21-8f86-232c0db80c1f.jpg?1783916673",
                ),
                "Create X 1/1 white Vampire creature tokens with lifelink, " +
                    "where X is the number of other attacking creatures"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9ec900f-1e31-4440-a75a-20b256734d5b.jpg?1783916955"
        ruling("2023-04-14", "For both modes, the value of X is determined as the ability resolves.")
        ruling("2023-04-14", "If you choose the first mode and there are no other attacking creatures, you'll create a 0/0 Dinosaur token. Unless something else is immediately raising its toughness, the token will die.")
        ruling("2023-04-14", "Although the Dinosaur token enters the battlefield attacking, it was never declared as an attacking creature.")
        ruling("2023-04-14", "If you choose the second mode and there are no other attacking creatures, you won't create any Vampire tokens.")
    }
}
