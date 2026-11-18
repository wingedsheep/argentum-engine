package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hornet Nest
 * {2}{G}
 * Creature — Insect
 * 0/2
 * Defender (This creature can't attack.)
 * Whenever this creature is dealt damage, create that many 1/1 green Insect creature tokens with
 * flying and deathtouch.
 *
 * Any damage — combat or not — triggers it, and "that many" is the damage dealt in that event. It
 * still triggers when the damage is lethal (its ruling).
 */
val HornetNest = card("Hornet Nest") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    power = 0
    toughness = 2
    oracleText = "Defender (This creature can't attack.)\n" +
        "Whenever this creature is dealt damage, create that many 1/1 green Insect creature tokens " +
        "with flying and deathtouch. (Any amount of damage a creature with deathtouch deals to a " +
        "creature is enough to destroy it.)"

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.CreateToken(
            count = DynamicAmounts.triggerDamageAmount(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
            keywords = setOf(Keyword.FLYING, Keyword.DEATHTOUCH),
            imageUri = "https://cards.scryfall.io/normal/front/e/1/e101a5b1-614e-43a0-9e7e-de5a248e2e43.jpg?1783939140"
        )
        description = "Whenever this creature is dealt damage, create that many 1/1 green Insect " +
            "creature tokens with flying and deathtouch."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "177"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/4784dbd3-8ae0-45a0-8cde-908fba6af9d2.jpg?1783939166"
        ruling("2014-07-18", "Hornet Nest's triggered ability will trigger even if it's dealt lethal damage. For example, if it's dealt 7 damage, its ability will trigger and you'll put seven Insect creature tokens onto the battlefield.")
    }
}
