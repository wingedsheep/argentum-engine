package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Doors of Durin
 * {3}{R}{G}
 * Legendary Artifact
 *
 * Whenever you attack, scry 2, then you may reveal the top card of your library.
 * If it's a creature card, put it onto the battlefield tapped and attacking.
 * Until your next turn, it gains trample if you control a Dwarf and hexproof
 * if you control an Elf.
 */
val DoorsOfDurin = card("Doors of Durin") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "GR"
    typeLine = "Legendary Artifact"
    oracleText = "Whenever you attack, scry 2, then you may reveal the top card of your library. If it's a creature card, put it onto the battlefield tapped and attacking. Until your next turn, it gains trample if you control a Dwarf and hexproof if you control an Elf."

    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = LibraryPatterns.scry(2).then(
            MayEffect(
                effect = Effects.Composite(listOf(
                    GatherCardsEffect(
                        source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), Player.You),
                        storeAs = "top"
                    ),
                    RevealCollectionEffect(from = "top"),
                    FilterCollectionEffect(
                        from = "top",
                        filter = CollectionFilter.MatchesFilter(GameObjectFilter.Creature),
                        storeMatching = "topCreature"
                    ),
                    MoveCollectionEffect(
                        from = "topCreature",
                        destination = CardDestination.ToZone(
                            Zone.BATTLEFIELD,
                            Player.You,
                            ZonePlacement.TappedAndAttacking
                        )
                    ),
                    ConditionalEffect(
                        condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.withSubtype("Dwarf")),
                        effect = Effects.GrantKeyword(
                            Keyword.TRAMPLE,
                            EffectTarget.PipelineTarget("topCreature"),
                            Duration.UntilYourNextTurn
                        )
                    ),
                    ConditionalEffect(
                        condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.withSubtype("Elf")),
                        effect = Effects.GrantKeyword(
                            Keyword.HEXPROOF,
                            EffectTarget.PipelineTarget("topCreature"),
                            Duration.UntilYourNextTurn
                        )
                    )
                )),
                descriptionOverride = "You may reveal the top card of your library. If it's a creature card, put it onto the battlefield tapped and attacking. Until your next turn, it gains trample if you control a Dwarf and hexproof if you control an Elf."
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "199"
        artist = "Marc Simonetti"
        flavorText = "\"Speak, friend, and enter.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8ebd8813-4aaf-48bf-9243-3ec4099b8372.jpg?1686969721"
    }
}
