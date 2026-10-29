package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Siege Striker — Core Set 2021 #37 (canonical printing)
 * {2}{W} · Creature — Human Soldier · 1/1
 *
 * Double strike
 * Whenever this creature attacks, you may tap any number of untapped creatures you control. This
 * creature gets +1/+1 until end of turn for each creature tapped this way.
 *
 * Orphans of the Wheat's shape. "You may tap any number" is a choose-any-number that admits zero.
 * The untapped creatures are gathered on resolution, so a Siege Striker with vigilance (or one
 * untapped since attacking) may tap itself (ruling); the bonus is a locked-in count of the
 * creatures tapped, unaffected by them later leaving or untapping (ruling).
 */
val SiegeStriker = card("Siege Striker") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Double strike (This creature deals both first-strike and regular combat damage.)\n" +
        "Whenever this creature attacks, you may tap any number of untapped creatures you control. " +
        "This creature gets +1/+1 until end of turn for each creature tapped this way."

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.ControlledPermanents(
                    player = Player.You,
                    filter = GameObjectFilter.Creature.youControl().untapped()
                )
            )
            val tapped = chooseAnyNumber(
                from = candidates,
                prompt = "You may tap any number of untapped creatures you control",
                useTargetingUI = true
            )
            run(Effects.TapCollection(tapped, tap = true))
            run(Effects.ModifyStats(
                power = tapped.count,
                toughness = tapped.count,
                target = EffectTarget.Self
            ))
        }
        description = "Whenever this creature attacks, you may tap any number of untapped creatures " +
            "you control. This creature gets +1/+1 until end of turn for each creature tapped this way."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a0ffa89-e6ee-466c-8edc-dd24c8b52e80.jpg?1783930733"
        ruling(
            "2020-06-23",
            "You choose how many and which creatures to tap while Siege Striker's ability is resolving. " +
                "No player may take other actions between the time you make this choice and the time " +
                "Siege Striker's power and toughness are raised."
        )
        ruling(
            "2020-06-23",
            "If an attacking creature with vigilance is tapped for Siege Striker's ability, that creature " +
                "remains an attacking creature."
        )
        ruling(
            "2020-06-23",
            "If Siege Striker gains vigilance and attacks or attacks and becomes untapped before its last " +
                "ability resolves, it can be tapped for its own ability."
        )
        ruling(
            "2020-06-23",
            "Once Siege Striker's ability has resolved, it keeps the power-and-toughness bonus even if " +
                "some of the tapped creatures leave the battlefield or become untapped."
        )
    }
}
