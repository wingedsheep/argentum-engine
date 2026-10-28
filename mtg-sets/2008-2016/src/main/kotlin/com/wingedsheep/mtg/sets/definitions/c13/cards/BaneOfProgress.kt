package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bane of Progress — Commander 2013 #137
 * {4}{G}{G} · Creature — Elemental · 2/2
 *
 * When this creature enters, destroy all artifacts and enchantments. Put a +1/+1 counter on this
 * creature for each permanent destroyed this way.
 *
 * Death Begets Life's shape: [Effects.DestroyAll] with `storeDestroyedAs` records only the
 * permanents actually destroyed (indestructible / regenerated ones stay out of the count), and
 * that collection's count feeds the +1/+1 counters. [GameObjectFilter.ArtifactOrEnchantment] is a
 * single Or predicate, so an enchantment artifact is destroyed and counted once.
 */
val BaneOfProgress = card("Bane of Progress") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, destroy all artifacts and enchantments. Put a +1/+1 counter " +
        "on this creature for each permanent destroyed this way."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val destroyed = runStoringCollection {
                Effects.DestroyAll(GameObjectFilter.ArtifactOrEnchantment, storeDestroyedAs = it)
            }
            run(Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, destroyed.count, EffectTarget.Self))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Lars Grant-West"
        flavorText = "It sees shaped stone and carved wood as mutilations."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb9b492f-a404-400f-be70-8b4901d2d82b.jpg?1783939663"
        ruling(
            "2013-10-17",
            "Bane of Progress's ability destroys all artifacts and enchantments, including those you control."
        )
        ruling(
            "2013-10-17",
            "If an artifact or enchantment isn't destroyed (perhaps because it has indestructible or it " +
                "regenerated), it won't count toward the number of +1/+1 counters put on Bane of Progress. " +
                "However, if an artifact or enchantment is destroyed but doesn't go to its owner's graveyard " +
                "due to a replacement effect (like the one Rest in Peace creates), it will count."
        )
    }
}
