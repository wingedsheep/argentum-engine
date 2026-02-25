package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect

/**
 * Wirewood Symbiote
 * {G}
 * Creature — Insect
 * 1/1
 * Return an Elf you control to its owner's hand: Untap target creature. Activate only once each turn.
 */
val WirewoodSymbiote = card("Wirewood Symbiote") {
    manaCost = "{G}"
    typeLine = "Creature — Insect"
    oracleText = "Return an Elf you control to its owner's hand: Untap target creature. Activate only once each turn."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.ReturnToHand(GameObjectFilter.Creature.withSubtype("Elf"))
        val t = target("creature", Targets.Creature)
        effect = TapUntapEffect(
            target = t,
            tap = false
        )
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Thomas M. Baxa"
        flavorText = "It drinks fatigue."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49488b76-abaf-4dba-b01f-7b418e4ff295.jpg?1562528525"
    }
}
