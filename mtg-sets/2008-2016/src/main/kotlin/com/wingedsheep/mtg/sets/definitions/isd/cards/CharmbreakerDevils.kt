package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Charmbreaker Devils
 * {5}{R}
 * Creature — Devil
 * 4/4
 * At the beginning of your upkeep, return an instant or sorcery card at random from your graveyard
 * to your hand.
 * Whenever you cast an instant or sorcery spell, this creature gets +4/+0 until end of turn.
 */
val CharmbreakerDevils = card("Charmbreaker Devils") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil"
    oracleText = "At the beginning of your upkeep, return an instant or sorcery card at random from your " +
        "graveyard to your hand.\n" +
        "Whenever you cast an instant or sorcery spell, this creature gets +4/+0 until end of turn."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val spells = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.InstantOrSorcery)
            )
            val chosen = chooseRandom(1, from = spells)
            toHand(chosen)
        }
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(4, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "134"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9197a67-6609-496c-9aae-825ede4f755b.jpg?1783940941"
        ruling(
            "2017-11-17",
            "The instant or sorcery card returned to your hand is chosen at random as Charmbreaker Devils's " +
                "first ability resolves. If any player responds to the ability, that player won't yet know what " +
                "card will be returned."
        )
        ruling(
            "2017-11-17",
            "Because the first ability doesn't target the instant or sorcery card, any instants or sorceries " +
                "put into your graveyard in response to that ability may be returned to your hand."
        )
    }
}
