package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Anchor to Reality — Kamigawa: Neon Dynasty #45 (canonical printing)
 * {2}{U}{U} · Sorcery
 *
 * As an additional cost to cast this spell, sacrifice an artifact or creature.
 * Search your library for an Equipment or Vehicle card, put that card onto the battlefield, then
 * shuffle. If it has mana value less than the sacrificed permanent's mana value, scry 2.
 *
 * The sacrifice is a spell additional cost, so its mana value is read from the cost-payment
 * snapshot (`EffectTarget.SacrificedAsCost(0)`, last-known information — a sacrificed token
 * still counts). The found card is kept in a pipeline collection so the scry comparison reads
 * *its* mana value after the shuffle; a failed search leaves the collection empty and there is
 * no "it" to compare, so no scry.
 */
val AnchorToReality = card("Anchor to Reality") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact or creature.\n" +
        "Search your library for an Equipment or Vehicle card, put that card onto the battlefield, " +
        "then shuffle. If it has mana value less than the sacrificed permanent's mana value, scry 2."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Artifact or GameObjectFilter.Creature))

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT) or
                        GameObjectFilter.Any.withSubtype(Subtype.VEHICLE)
                ),
                search = true
            )
            val found = chooseUpTo(
                1,
                from = searchable,
                prompt = "Search your library for an Equipment or Vehicle card",
            )
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
            ifNotEmpty(found) {
                run(Effects.If(
                    condition = Conditions.CompareAmounts(
                        left = DynamicAmounts.manaValueOf(found),
                        operator = ComparisonOperator.LT,
                        right = DynamicAmounts.manaValueOf(EffectTarget.SacrificedAsCost(0))
                    ),
                    then = Effects.Scry(2)
                ))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Alix Branwyn"
        flavorText = "The Wanderer sighed in relief as the Reality Chip tethered her wayward spark to Kamigawa."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58141ad0-32ea-4e07-a133-ff7812d58c0e.jpg?1783923909"
        ruling(
            "2022-02-18",
            "You must sacrifice exactly one artifact or creature to cast this spell; you can't cast it " +
                "without sacrificing an artifact or creature, and you can't sacrifice additional artifacts or creatures."
        )
        ruling("2022-02-18", "If a card in a player's library has {X} in its mana cost, X is 0.")
    }
}
