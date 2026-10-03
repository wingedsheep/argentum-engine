package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pyretic Rebirth
 * {2}{B}{R}
 * Instant
 *
 * Return target artifact or creature card from your graveyard to your hand. Pyretic Rebirth
 * deals damage equal to that card's mana value to up to one target creature or planeswalker.
 *
 * Target reads are live-only, so the returned card's mana value is frozen with `storeNumber`
 * before the move to hand. An illegal graveyard target reads as 0, so no damage is dealt (ruling).
 */
val PyreticRebirth = card("Pyretic Rebirth") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Instant"
    oracleText = "Return target artifact or creature card from your graveyard to your hand. " +
        "Pyretic Rebirth deals damage equal to that card's mana value to up to one target creature or planeswalker."

    spell {
        val card = target(TargetFilter(Filters.Unified.artifact.or(Filters.Creature).ownedByYou(), zone = Zone.GRAVEYARD))
        val victim = target(Targets.CreatureOrPlaneswalker, optional = true)
        effect = Effects.Pipeline {
            val mv = storeNumber(DynamicAmounts.manaValueOf(card))
            run(Effects.ReturnToHandFromGraveyard(card))
            run(Effects.DealDamage(mv.amount, victim))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Richard Kane Ferguson"
        flavorText = "Death has never kept Darigaaz down for long."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/5968f641-48b5-4b97-8072-ddd8073cd4d7.jpg?1783911246"
        ruling("2024-06-07", "If the target card in your graveyard is an illegal target when Pyretic Rebirth tries to resolve, no damage will be dealt.")
        ruling("2024-06-07", "If the target creature or planeswalker is an illegal target when Pyretic Rebirth tries to resolve, you'll still return the target artifact or creature card from your graveyard to your hand.")
    }
}
