package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crush the Weak
 * {2}{R}
 * Sorcery
 * Crush the Weak deals 2 damage to each creature. If a creature dealt damage this way would die
 * this turn, exile it instead.
 * Foretell {R}
 *
 * Draconic Intervention's shape with a fixed 2: per creature, the damage instruction records how
 * much it actually dealt (`damageDealtVariable`), and only a creature really dealt damage gets the
 * "exile instead of dying this turn" mark — a creature whose damage was prevented isn't marked.
 * The marks land during this resolution, before state-based actions see the lethal damage.
 * Foretell is lowered into [KeywordAbility.foretell], as on Demon Bolt.
 */
val CrushTheWeak = card("Crush the Weak") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Crush the Weak deals 2 damage to each creature. If a creature dealt damage this way " +
        "would die this turn, exile it instead.\n" +
        "Foretell {R} (During your turn, you may pay {2} and exile this card from your hand " +
        "face down. Cast it on a later turn for its foretell cost.)"

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature),
            Effects.Pipeline {
                val dealt = runStoringNumber {
                    Effects.DealDamage(2, EffectTarget.IterationEntity, damageDealtVariable = it)
                }
                ifThen(Conditions.CompareAmounts(dealt.amount, ComparisonOperator.GT, 0)) {
                    run(Effects.MarkExileOnDeath(EffectTarget.IterationEntity))
                }
            }
        )
    }

    keywordAbility(KeywordAbility.foretell("{R}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/875a20c2-1d17-46ea-b4d2-3e70bc05aae3.jpg?1783928233"
        ruling(
            "2021-02-05",
            "Creatures don't necessarily have to be dealt lethal damage by Crush the Weak to be exiled. " +
                "After being dealt damage, if they would die for any reason that turn, they'll be exiled instead."
        )
    }
}
