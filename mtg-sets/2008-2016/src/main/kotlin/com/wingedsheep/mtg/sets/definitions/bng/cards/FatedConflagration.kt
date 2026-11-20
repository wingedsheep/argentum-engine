package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Fated Conflagration — Born of the Gods #94 (canonical / earliest real printing)
 * {1}{R}{R}{R} · Instant
 *
 * Fated Conflagration deals 5 damage to target creature or planeswalker. If it's your turn, scry 2.
 *
 * [Targets.CreatureOrPlaneswalker] (Dreadbore's single-object requirement) takes 5 damage, then
 * [Effects.Scry] gated on [Conditions.IsYourTurn] — the Fated cycle's shared tail, checked at
 * resolution.
 */
val FatedConflagration = card("Fated Conflagration") {
    manaCost = "{1}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Fated Conflagration deals 5 damage to target creature or planeswalker. " +
        "If it's your turn, scry 2. (Look at the top two cards of your library, then put any " +
        "number of them on the bottom of your library and the rest on top in any order.)"

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(5, t) then
            Effects.If(Conditions.IsYourTurn, Effects.Scry(2))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66ea1aeb-bd42-4960-b40f-e7c2fd1efb5c.jpg?1783939545"
    }
}
