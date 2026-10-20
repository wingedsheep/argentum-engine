package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cosmic Horror
 * {3}{B}{B}{B}
 * Creature — Horror
 * 7/7
 * First strike
 * At the beginning of your upkeep, destroy this creature unless you pay {3}{B}{B}{B}. If this
 * creature is destroyed this way, it deals 7 damage to you.
 *
 * The upkeep tax is [Effects.PayOrSuffer]. The unpaid branch destroys the Horror through
 * `moveTracked(moveType = MoveType.Destroy)`, which records only what actually reached the
 * graveyard — so a regenerated, shielded or indestructible Horror wasn't "destroyed this way" and
 * the 7 damage is skipped. The damage source is the Horror itself (last-known information), dealt
 * to the trigger's controller.
 */
val CosmicHorror = card("Cosmic Horror") {
    manaCost = "{3}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    oracleText = "First strike\n" +
        "At the beginning of your upkeep, destroy this creature unless you pay {3}{B}{B}{B}. " +
        "If this creature is destroyed this way, it deals 7 damage to you."
    power = 7
    toughness = 7

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Mana("{3}{B}{B}{B}"),
            suffer = Effects.Pipeline {
                val self = gather(CardSource.Entity(EffectTarget.Self))
                val destroyed = moveTracked(
                    self,
                    CardDestination.ToZone(Zone.GRAVEYARD),
                    moveType = MoveType.Destroy,
                )
                ifNotEmpty(destroyed) {
                    run(Effects.DealDamage(7, EffectTarget.PlayerRef(Player.You)))
                }
            },
            consequenceDescription = "destroy Cosmic Horror; if it's destroyed this way, it deals 7 damage to you",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Jesper Myrfors"
        flavorText = "\"Then flashed the living lightning from her eyes,/ And screams of horror rend " +
            "th' affrighted skies.\" —Alexander Pope, *The Rape of the Lock*"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18bc6ac2-19e0-4765-852b-e303a5bb4040.jpg?1783948068"
    }
}
