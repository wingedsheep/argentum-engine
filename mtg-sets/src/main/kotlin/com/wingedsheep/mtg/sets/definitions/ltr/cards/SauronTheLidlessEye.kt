package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Sauron, the Lidless Eye
 * {3}{B}{R}
 * Legendary Creature — Avatar Horror
 * 4/4
 *
 * When Sauron enters, gain control of target creature an opponent controls until
 * end of turn. Untap it. It gains haste until end of turn.
 * {1}{B}{R}: Creatures you control get +2/+0 until end of turn. Each opponent loses 2 life.
 */
val SauronTheLidlessEye = card("Sauron, the Lidless Eye") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Avatar Horror"
    power = 4
    toughness = 4
    oracleText = "When Sauron enters, gain control of target creature an opponent controls until end of turn. Untap it. It gains haste until end of turn.\n{1}{B}{R}: Creatures you control get +2/+0 until end of turn. Each opponent loses 2 life."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("target creature an opponent controls", Targets.CreatureOpponentControls)
        effect = Effects.Composite(
            Effects.GainControl(creature, Duration.EndOfTurn),
            Effects.Untap(creature),
            Effects.GrantKeyword(Keyword.HASTE, creature)
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}{R}")
        effect = GroupPatterns.modifyStatsForAll(2, 0, Filters.Group.creaturesYouControl)
            .then(Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "288"
        artist = "Yigit Koroglu"
        flavorText = "There is an Eye in the Dark Tower that does not sleep."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d82a4c78-d2fc-425a-8d0e-2e64509a08f1.jpg?1715720382"
    }
}
