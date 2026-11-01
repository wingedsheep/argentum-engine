package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ilharg, the Raze-Boar — War of the Spark #133 (canonical printing)
 * {3}{R}{R} · Legendary Creature — Boar God · 6/6
 *
 * Trample
 * Whenever Ilharg attacks, you may put a creature card from your hand onto the battlefield tapped
 * and attacking. Return that creature to your hand at the beginning of the next end step.
 * When Ilharg dies or is put into exile from the battlefield, you may put it into its owner's
 * library third from the top.
 *
 * The attack trigger is Kinscaer Sentry's up-to-one hand pick, entering with
 * [EnteringAttackDefender.CONTROLLER_CHOOSES] — per the 2024-01-12 ruling the new creature needn't
 * attack what Ilharg attacks (CR 508.4). Each card put in schedules its own end-step return on
 * [EffectTarget.IterationEntity], which the delayed trigger binds with its battlefield identity, so a
 * creature that died or blinked before the end step is left alone (2019-05-03 ruling); the
 * `fromZone` guard keeps a card whose entry was replaced from being pulled out of another zone.
 * The God-Eternal tail is God-Eternal Bontu's two-trigger shape (`positionFromTop = 2` is third).
 */
val IlhargTheRazeBoar = card("Ilharg, the Raze-Boar") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Boar God"
    power = 6
    toughness = 6
    oracleText = "Trample\n" +
        "Whenever Ilharg attacks, you may put a creature card from your hand onto the battlefield tapped " +
        "and attacking. Return that creature to your hand at the beginning of the next end step.\n" +
        "When Ilharg dies or is put into exile from the battlefield, you may put it into its owner's " +
        "library third from the top."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val handCreatures = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature))
            val chosen = chooseUpTo(
                1,
                from = handCreatures,
                prompt = "You may put a creature card onto the battlefield tapped and attacking",
                selectedLabel = "Put onto the battlefield tapped and attacking"
            )
            move(
                chosen,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.TappedAndAttacking),
                attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES
            )
            run(
                Effects.ForEachInCollection(
                    chosen,
                    Effects.CreateDelayedTrigger(
                        step = Step.END,
                        effect = Effects.Move(EffectTarget.IterationEntity, Zone.HAND, fromZone = Zone.BATTLEFIELD)
                    )
                )
            )
        }
        description = "Whenever Ilharg attacks, you may put a creature card from your hand onto the battlefield " +
            "tapped and attacking. Return that creature to your hand at the beginning of the next end step."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.May(
            Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.GRAVEYARD, positionFromTop = 2),
            prompt = "Put Ilharg, the Raze-Boar into its owner's library third from the top?"
        )
        description = "When Ilharg dies, you may put it into its owner's library third from the top."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves(to = Zone.EXILE)
        effect = Effects.May(
            Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.EXILE, positionFromTop = 2),
            prompt = "Put Ilharg, the Raze-Boar into its owner's library third from the top?"
        )
        description = "When Ilharg is put into exile from the battlefield, you may put it into its owner's " +
            "library third from the top."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "133"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0109b60-09aa-4a03-92e7-0c651d976d51.jpg?1783933428"

        ruling(
            "2024-01-12",
            "You choose which player, planeswalker, or battle the new creature is attacking. It doesn't have to " +
                "be attacking the same player, planeswalker, or battle that Ilharg is attacking."
        )
        ruling(
            "2019-05-03",
            "If the new creature leaves the battlefield before the end step, most likely because it died in " +
                "combat, that card remains in its current zone. It won't return to your hand."
        )
        ruling(
            "2019-05-03",
            "Although the new creature is attacking, it was never declared as an attacking creature (for the " +
                "purposes of abilities that trigger whenever a creature attacks, for example, such as those of " +
                "Trusted Pegasus and Makeshift Battalion)."
        )
        ruling(
            "2019-05-03",
            "If one of these Gods leaves the graveyard or exile while its last ability is on the stack, it will " +
                "remain in its new zone, even if that zone is a graveyard or exile."
        )
        ruling(
            "2019-05-03",
            "If the God's owner has two or fewer cards in their library, the God is put on the bottom of their " +
                "library as its last ability resolves."
        )
    }
}
