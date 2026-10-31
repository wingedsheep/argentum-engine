package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Heartless Act
 * {1}{B}
 * Instant
 * Choose one —
 * • Destroy target creature with no counters on it.
 * • Remove up to three counters from target creature.
 *
 * A true "Choose one —" modal spell ([ModalEffect.chooseOne]). Mode 1 destroys a creature
 * constrained to having no counters via [TargetFilter.withoutCounters] (composed from
 * `StatePredicate.Not(HasAnyCounter)`). Mode 2 uses [Effects.RemoveCountersUpTo], the
 * total-budget-capped counter-removal effect: the controller chooses how many counters of each
 * kind to remove from the target, capped at three counters in total.
 */
val HeartlessAct = card("Heartless Act") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Destroy target creature with no counters on it.\n" +
        "• Remove up to three counters from target creature."

    spell {
        effect = ModalEffect.chooseOne(
            mode("Destroy target creature with no counters on it") {
                val creature = target(TargetFilter.Creature.withoutCounters())
                effect = Effects.Destroy(creature)
            },
            mode("Remove up to three counters from target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.RemoveCountersUpTo(3, creature)
            },
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Ryan Pancoast"
        flavorText = "\"It's them or us.\"\n—General Kudro"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4e6794a-feeb-4fc8-a2ee-38c75c18aaae.jpg?1783931060"
    }
}
