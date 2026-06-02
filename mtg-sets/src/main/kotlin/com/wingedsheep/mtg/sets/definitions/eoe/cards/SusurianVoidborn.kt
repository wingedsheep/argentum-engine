package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
/**
 * Susurian Voidborn
 * {2}{B}
 * Creature — Vampire Soldier
 * Whenever this creature or another creature or artifact you control dies, target opponent loses 1 life and you gain 1 life.
 * Warp {B}
 * 2/2
 */
val SusurianVoidborn = card("Susurian Voidborn") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Soldier"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature or another creature or artifact you control dies, target opponent loses 1 life and you gain 1 life.\n" +
        "Warp {B} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"

    // Whenever this creature or another creature or artifact you control dies, target opponent loses 1 life and you gain 1 life.
    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.CreatureOrArtifact.youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        val opponent = target("target opponent", Targets.Opponent)
        effect = Effects.Composite(
            listOf(
                Effects.LoseLife(1, opponent),
                Effects.GainLife(1)
            )
        )
        description = "Whenever this creature or another creature or artifact you control dies, target opponent loses 1 life and you gain 1 life."
    }

    // Warp ability
    warp = "{B}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Jehan Choo"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/beb97e7b-0ae7-4b08-9ceb-6a7f825bcd49.jpg?1752947031"
        ruling("2025-07-25", "If Susurian Voidborn dies at the same time as one or more creatures or artifacts you control, its first ability will still trigger for itself and each of those creatures and artifacts.")
        ruling("2025-07-25", "If the target opponent is an illegal target as Susurian Voidborn's first ability tries to resolve, it won't resolve and none of its effects will happen. You won't gain life.")
    }
}
