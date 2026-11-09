package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Declaration in Stone (Shadows over Innistrad #12)
 * {1}{W}
 * Sorcery
 *
 * Exile target creature and all other creatures its controller controls with the same name as
 * that creature. That player investigates for each nontoken creature exiled this way.
 *
 * Only the first creature is targeted: the same-named ones are gathered at resolution from the
 * target's controller's battlefield, so a hexproof namesake is exiled too, and everything leaves in
 * one move. A face-down creature has no name (CR 708.2a) and shares it with nothing, so a face-down
 * target is exiled alone and a face-down namesake is never swept up. "Exiled this way" counts the
 * nontoken creatures that actually moved (a replacement can keep one from reaching exile); "that
 * player" is the target's last-known controller, read after it has left the battlefield.
 */
val DeclarationInStone = card("Declaration in Stone") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile target creature and all other creatures its controller controls with the same name as " +
        "that creature. That player investigates for each nontoken creature exiled this way."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Pipeline {
            val targeted = storeEntity(t)
            val doomed = gather(
                CardSource.ControlledPermanents(
                    player = Player.ControllerOf("target creature"),
                    filter = GameObjectFilter.Creature.faceUp().sharingNameWith(t)
                )
            )
            ifNotEmpty(targeted, filter = GameObjectFilter.Creature.faceUp()) {
                // A named target is one of its own namesakes, so `doomed` already holds it.
                val nontoken = filter(doomed, GameObjectFilter.Creature.nontoken())
                val exiled = moveTracked(doomed, CardDestination.ToZone(Zone.EXILE))
                val nontokenExiled = exclude(nontoken, exclude(nontoken, exiled))
                run(Effects.Investigate(nontokenExiled.count, controller = EffectTarget.TargetController))
            } orElse {
                val nontoken = filter(targeted, GameObjectFilter.Creature.nontoken())
                val exiled = moveTracked(targeted, CardDestination.ToZone(Zone.EXILE))
                val nontokenExiled = exclude(nontoken, exclude(nontoken, exiled))
                run(Effects.Investigate(nontokenExiled.count, controller = EffectTarget.TargetController))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Tyler Jacobson"
        flavorText = "The message was clear to Sorin—Nahiri had paid Markov Manor a visit."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f685445d-e2af-455f-b3b2-97cf15362f3c.jpg?1783937822"
        ruling("2016-04-08", "If the target creature is an illegal target when Declaration in Stone tries to resolve, it won't resolve and none of its effects will happen. No creatures will be exiled, including those with the same name as the target.")
        ruling("2016-04-08", "It's possible to have a creature and a noncreature permanent with the same name, such as two copies of the same land, one of which has become a land creature. Only creatures with that name are exiled by Declaration in Stone.")
        ruling("2016-04-08", "A face-down creature, such as one cast with a megamorph ability from the Dragons of Tarkir set, has no name and can't share a name with any other creatures.")
        ruling("2016-04-08", "Declaration in Stone has only one target. The other creatures with that name aren't targeted. For example, a creature with hexproof will be exiled if it has the same name as the target creature.")
    }
}
