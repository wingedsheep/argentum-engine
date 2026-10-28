package com.wingedsheep.mtg.sets.definitions.`40k`.cards

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
 * Exalted Flamer of Tzeentch
 * {2}{U}{R}
 * Creature — Demon
 * 2/4
 *
 * Sorcerous Inspiration — At the beginning of your upkeep, return an instant or sorcery card at
 * random from your graveyard to your hand.
 * Fire of Tzeentch — Whenever you cast an instant or sorcery spell, this creature deals 1 damage
 * to each opponent.
 *
 * The upkeep return is Ghoulraiser's gather → chooseRandom → toHand pipeline (an empty graveyard
 * simply returns nothing).
 */
val ExaltedFlamerOfTzeentch = card("Exalted Flamer of Tzeentch") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Demon"
    power = 2
    toughness = 4
    oracleText = "Sorcerous Inspiration — At the beginning of your upkeep, return an instant or sorcery " +
        "card at random from your graveyard to your hand.\n" +
        "Fire of Tzeentch — Whenever you cast an instant or sorcery spell, this creature deals 1 " +
        "damage to each opponent."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val spells = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.InstantOrSorcery))
            val chosen = chooseRandom(1, from = spells)
            toHand(chosen)
        }
        description = "Sorcerous Inspiration — At the beginning of your upkeep, return an instant or " +
            "sorcery card at random from your graveyard to your hand."
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Fire of Tzeentch — Whenever you cast an instant or sorcery spell, this " +
            "creature deals 1 damage to each opponent."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "119"
        artist = "Alexey Kruglov"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47ebc3bc-113f-437c-b60a-9597939d91ea.jpg?1783920852"
    }
}
