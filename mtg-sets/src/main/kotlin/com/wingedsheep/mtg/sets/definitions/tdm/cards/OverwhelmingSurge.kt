package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Overwhelming Surge
 * {2}{R}
 * Instant
 * Choose one or both —
 * • Overwhelming Surge deals 3 damage to target creature.
 * • Destroy target noncreature artifact.
 *
 * Modeled as three modes (damage only / destroy only / both), so "choose one or both"
 * is expressed as choosing exactly one of the combined options — the same shape as
 * Winterflame.
 */
private val NoncreatureArtifact = TargetFilter(
    GameObjectFilter(cardPredicates = listOf(CardPredicate.IsArtifact, CardPredicate.IsNoncreature))
)

val OverwhelmingSurge = card("Overwhelming Surge") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one or both —\n" +
        "• Overwhelming Surge deals 3 damage to target creature.\n" +
        "• Destroy target noncreature artifact."

    spell {
        modal(chooseCount = 1) {
            mode("Overwhelming Surge deals 3 damage to target creature") {
                val creature = target("creature", TargetCreature())
                effect = Effects.DealDamage(3, creature)
            }
            mode("Destroy target noncreature artifact") {
                val artifact = target("noncreature artifact", TargetPermanent(filter = NoncreatureArtifact))
                effect = Effects.Destroy(artifact)
            }
            mode("Deal 3 damage to target creature and destroy target noncreature artifact") {
                val creature = target("creature", TargetCreature())
                val artifact = target("noncreature artifact", TargetPermanent(filter = NoncreatureArtifact))
                effect = Effects.Composite(
                    listOf(
                        Effects.DealDamage(3, creature),
                        Effects.Destroy(artifact),
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Gaboleps"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd7af85f-354e-468a-990b-bd774e68240f.jpg?1743204422"
    }
}
