package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CreatePermanentGlobalTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sorin, Solemn Visitor - {2}{W}{B}
 * Legendary Planeswalker — Sorin
 * Starting Loyalty: 4
 *
 * +1: Until your next turn, creatures you control get +1/+0 and gain lifelink.
 *
 * −2: Create a 2/2 black Vampire creature token with flying.
 *
 * −6: You get an emblem with "At the beginning of each opponent's upkeep, that player
 * sacrifices a creature."
 */
val SorinSolemnVisitor = card("Sorin, Solemn Visitor") {
    manaCost = "{2}{W}{B}"
    typeLine = "Legendary Planeswalker — Sorin"
    startingLoyalty = 4

    // +1: Until your next turn, creatures you control get +1/+0 and gain lifelink.
    loyaltyAbility(+1) {
        effect = Effects.Composite(
            EffectPatterns.modifyStatsForAll(
                power = 1,
                toughness = 0,
                filter = GroupFilter.AllCreaturesYouControl,
                duration = Duration.UntilYourNextTurn
            ),
            EffectPatterns.grantKeywordToAll(
                keyword = Keyword.LIFELINK,
                filter = GroupFilter.AllCreaturesYouControl,
                duration = Duration.UntilYourNextTurn
            )
        )
    }

    // -2: Create a 2/2 black Vampire creature token with flying.
    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Vampire"),
            keywords = setOf(Keyword.FLYING)
        )
    }

    // -6: Emblem with "At the beginning of each opponent's upkeep, that player sacrifices a creature."
    loyaltyAbility(-6) {
        effect = CreatePermanentGlobalTriggeredAbilityEffect(
            ability = TriggeredAbility.create(
                trigger = Triggers.EachOpponentUpkeep.event,
                binding = Triggers.EachOpponentUpkeep.binding,
                effect = ForceSacrificeEffect(
                    filter = GameObjectFilter.Creature,
                    count = 1,
                    target = EffectTarget.PlayerRef(Player.Opponent)
                )
            ),
            descriptionOverride = "At the beginning of each opponent's upkeep, that player sacrifices a creature."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "202"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da1a9643-34d6-4b4b-b896-2d4626eca40a.jpg?1562794474"
        ruling("2014-09-20", "Only creatures you control as the first ability resolves will get the bonuses. Creatures you come to control after that happens will not.")
        ruling("2014-09-20", "When the emblem's ability resolves, if the player controls no creatures, the player won't sacrifice anything.")
    }
}
