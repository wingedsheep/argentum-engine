package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gravedig
 * {1}{B}
 * Sorcery
 * Choose one —
 * • Target player creates a 2/2 black Zombie creature token.
 * • Return target creature card from your graveyard to your hand.
 * Entwine {2} (Choose both if you pay the entwine cost.)
 *
 * Entwine is the two-mode `modal(chooseCount = 2, minChooseCount = 1,
 * additionalManaCostPerExtraMode = …)` shape shared with Mirrodin's entwine cards: choosing the
 * second mode costs exactly the entwine cost. Each mode declares its own target, so a target is
 * only chosen for the modes picked.
 */
val Gravedig = card("Gravedig") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Target player creates a 2/2 black Zombie creature token.\n" +
        "• Return target creature card from your graveyard to your hand.\n" +
        "Entwine {2} (Choose both if you pay the entwine cost.)"

    spell {
        modal(
            chooseCount = 2,
            minChooseCount = 1,
            additionalManaCostPerExtraMode = "{2}",
        ) {
            mode("Target player creates a 2/2 black Zombie creature token") {
                val player = target(Targets.Player)
                effect = Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Zombie"),
                    controller = player,
                    imageUri = "https://cards.scryfall.io/normal/front/9/0/909387e1-dc33-446c-825f-07c915ad73ee.jpg?1783911112"
                )
            }
            mode("Return target creature card from your graveyard to your hand") {
                val creature = target(TargetFilter.CreatureInYourGraveyard)
                effect = Effects.ReturnToHand(creature)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Drew Baker"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2b054a9-7565-4fdd-b6a5-2786c5bb5b7e.jpg?1783911278"
    }
}
