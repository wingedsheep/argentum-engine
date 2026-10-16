package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Evolving Door
 * {2}{G}
 * Artifact
 * {1}, {T}, Sacrifice a creature: Count the colors of the sacrificed creature, then search your
 * library for a creature card that's exactly that many colors plus one. Exile that card, then
 * shuffle. You may cast the exiled card. Activate only as a sorcery.
 *
 * The sacrificed creature's colors are read from the cost-payment snapshot (last-known
 * information, so a sacrificed token or a recoloured creature counts as it last existed), and the
 * search filter compares each card's color count with `1 + that count`. The cast happens during
 * resolution, paying its costs ([Effects.CastFromCollection]); a card left uncast stays in exile.
 */
val EvolvingDoor = card("Evolving Door") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice a creature: Count the colors of the sacrificed creature, then search " +
        "your library for a creature card that's exactly that many colors plus one. Exile that card, then " +
        "shuffle. You may cast the exiled card. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.Sacrifice(GameObjectFilter.Creature))
        timing = TimingRule.SorcerySpeed
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter.Creature.exactlyColors(
                        DynamicAmounts.colorCountOf(EffectTarget.SacrificedAsCost(0)) + 1
                    )
                ),
                search = true
            )
            val found = chooseUpTo(1, from = searchable, prompt = "Search your library for a creature card")
            exile(found)
            run(Effects.ShuffleLibrary())
            ifNotEmpty(found) {
                run(Effects.May(
                    Effects.CastFromCollection(found),
                    descriptionOverride = "You may cast the exiled card."
                ))
            }
        }
        description = "Count the colors of the sacrificed creature, then search your library for a creature " +
            "card that's exactly that many colors plus one. Exile that card, then shuffle. You may cast the " +
            "exiled card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "144"
        artist = "Drew Baker"
        flavorText = "In with the old, out with the new."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01bc3b04-46b7-43df-9b29-1502b9f18bbf.jpg?1783923104"
    }
}
