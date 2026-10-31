package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Unbreakable Bond
 * {4}{B}
 * Sorcery
 * Return target creature card from your graveyard to the battlefield with a lifelink counter on it.
 *
 * The creature *enters* with the counter (Ascent of the Worthy / Razorlash Transmogrant shape:
 * a graveyard-to-battlefield `Move` carrying `addCounterType`), not a counter added afterwards.
 */
val UnbreakableBond = card("Unbreakable Bond") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to the battlefield with a " +
        "lifelink counter on it."

    spell {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(
            creatureCard,
            Zone.BATTLEFIELD,
            fromZone = Zone.GRAVEYARD,
            addCounterType = CounterType.LIFELINK,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Randy Vargas"
        flavorText = "From now until the crystals stop glowing\n—Ketria oath meaning \"forever\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9da02a1-7b39-4a0e-8466-6512a02f3e3b.jpg?1783931056"
    }
}
