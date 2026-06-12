package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns

/**
 * Decree of Annihilation
 * {8}{R}{R}
 * Sorcery
 * Exile all artifacts, creatures, and lands from the battlefield, all cards from all graveyards,
 * and all cards from all hands.
 * Cycling {5}{R}{R}
 * When you cycle Decree of Annihilation, destroy all lands.
 */
val DecreeOfAnnihilation = card("Decree of Annihilation") {
    manaCost = "{8}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Exile all artifacts, creatures, and lands from the battlefield, all cards from all graveyards, and all cards from all hands.\nCycling {5}{R}{R}\nWhen you cycle Decree of Annihilation, destroy all lands."

    spell {
        val artifactCreatureOrLand = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsArtifact, CardPredicate.IsCreature, CardPredicate.IsLand))
            )
        )

        effect = Effects.Pipeline {
            run(
                Effects.ForEachInGroup(
                    filter = GroupFilter(artifactCreatureOrLand),
                    effect = Effects.Move(EffectTarget.Self, Zone.EXILE)
                )
            )
            run(
                ForEachPlayerEffect(
                    players = Player.Each,
                    effects = Effects.PipelineSteps {
                        val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD), name = "graveyard")
                        move(graveyard, CardDestination.ToZone(Zone.EXILE))
                        val hand = gather(CardSource.FromZone(Zone.HAND), name = "hand")
                        move(hand, CardDestination.ToZone(Zone.EXILE))
                    }
                )
            )
        }
    }

    keywordAbility(KeywordAbility.cycling("{5}{R}{R}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        effect = Patterns.Group.destroyAll(GroupFilter.AllLands)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73744717-518c-478e-9da9-201c49124f37.jpg?1562530626"
        ruling("2022-12-08", "When you cycle this card, first the cycling ability goes on the stack, then the triggered ability goes on the stack on top of it. The triggered ability will resolve before you draw a card from the cycling ability.")
        ruling("2022-12-08", "The cycling ability and the triggered ability are separate. If the triggered ability doesn't resolve (because, for example, it has been countered, or all of its targets have become illegal), the cycling ability will still resolve, and you'll draw a card.")
    }
}
