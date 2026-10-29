package com.wingedsheep.mtg.sets.definitions.moc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Shalai and Hallar — March of the Machine Commander #10
 * {1}{R}{G}{W} · Legendary Creature — Angel Elf · 3/3
 *
 * Flying, vigilance
 * Whenever one or more +1/+1 counters are put on a creature you control, Shalai and Hallar deals
 * that much damage to target opponent.
 *
 * The passive "are put on" wording, so no `by` placer scope — an opponent putting counters on your
 * creature triggers it too. The per-recipient template folds a recipient's simultaneous placements
 * into one firing, and "that much" is that folded count (`triggerCountersPlaced`); several creatures
 * getting counters at once trigger it once each.
 */
val ShalaiAndHallar = card("Shalai and Hallar") {
    manaCost = "{1}{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Angel Elf"
    power = 3
    toughness = 3
    oracleText = "Flying, vigilance\n" +
        "Whenever one or more +1/+1 counters are put on a creature you control, Shalai and Hallar " +
        "deals that much damage to target opponent."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        val opponent = target(Targets.Opponent)
        effect = Effects.DealDamage(DynamicAmounts.triggerCountersPlaced(), opponent)
        description = "Whenever one or more +1/+1 counters are put on a creature you control, " +
            "Shalai and Hallar deals that much damage to target opponent."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "10"
        artist = "Mila Pesic"
        flavorText = "Between the angel's blade and the elf's bow, few Phyrexians made it as far as " +
            "Llanowar's canopy."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80804f25-efc3-44a2-bbae-9a97fec98009.jpg?1783917291"
    }
}
