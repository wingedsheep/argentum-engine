package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lukka, Coppercoat Outcast — Ikoria: Lair of Behemoths #125 (canonical printing)
 * {3}{R}{R} · Legendary Planeswalker — Lukka · Loyalty 5
 *
 * - +1: impulse exile of the top three, then a [MayPlayExpiry.Permanent] permission on just the
 *   creature cards among them, gated on an [Exists] check for any Lukka planeswalker you control.
 *   The printed text is a granted static ("as long as"), not a duration, so the gate is
 *   re-evaluated on every query and reopens when any Lukka returns — exactly as the rulings say.
 * - −2: the exiled creature's mana value is stored *before* it leaves (its last battlefield
 *   value), then a reveal-until for a creature card whose mana value is not at most that number
 *   (Lavinia's `Not(ManaValueAtMostDynamic)` shape for "greater"); the match enters, the rest go
 *   to the bottom in a random order.
 * - −7: Rakdos Charm's per-creature iteration, each creature the source of damage equal to its
 *   power to each opponent.
 */
val LukkaCoppercoatOutcast = card("Lukka, Coppercoat Outcast") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Lukka"
    startingLoyalty = 5
    oracleText = "+1: Exile the top three cards of your library. Creature cards exiled this way gain " +
        "\"You may cast this card from exile as long as you control a Lukka planeswalker.\"\n" +
        "−2: Exile target creature you control, then reveal cards from the top of your library until " +
        "you reveal a creature card with greater mana value. Put that card onto the battlefield and " +
        "the rest on the bottom of your library in a random order.\n" +
        "−7: Each creature you control deals damage equal to its power to each opponent."

    loyaltyAbility(+1) {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(3))
            exile(exiled)
            val creatures = filter(exiled, GameObjectFilter.Creature)
            run(
                Effects.GrantMayPlayFromExile(
                    from = creatures,
                    expiry = MayPlayExpiry.Permanent,
                    condition = Exists(
                        player = Player.You,
                        zone = Zone.BATTLEFIELD,
                        filter = GameObjectFilter.Planeswalker.withSubtype("Lukka"),
                    ),
                )
            )
        }
    }

    loyaltyAbility(-2) {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.Pipeline {
            val exiledManaValue = storeNumber(DynamicAmounts.manaValueOf(creature))
            run(Effects.Exile(creature))
            val (found, revealed) = gatherUntilMatch(
                GameObjectFilter.Creature.withCardPredicate(
                    CardPredicate.Not(CardPredicate.ManaValueAtMostDynamic(exiledManaValue.amount))
                )
            )
            reveal(revealed, fromZone = Zone.LIBRARY, toZone = Zone.BATTLEFIELD)
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            toLibraryBottom(exclude(revealed, found), order = CardOrder.Random)
        }
    }

    loyaltyAbility(-7) {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.DealDamage(
                amount = DynamicAmounts.powerOf(EffectTarget.IterationEntity),
                target = EffectTarget.PlayerRef(Player.EachOpponent),
                damageSource = EffectTarget.IterationEntity,
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "125"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/5798fdf0-d178-43d9-b821-8f3f654654b4.jpg?1783931048"
        ruling("2020-04-17", "You may cast creature cards exiled with Lukka's first ability as noncreature spells if a rule or effect allows you to do so. For example, adventurer cards from the Throne of Eldraine set may be cast as Adventures.")
        ruling("2020-04-17", "If a creature card in a player's library or a creature on the battlefield has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-04-17", "If you reveal your entire library without revealing a creature card with higher mana value, you put the library back in a random order and continue.")
        ruling("2020-04-17", "You still pay any costs for spells cast from Lukka's first ability. You may pay alternative costs, such as mutate costs.")
        ruling("2020-04-17", "Once you begin to cast an exiled card, losing control of Lukka won't affect the spell. You can finish casting it as normal.")
        ruling("2020-04-17", "For each creature card exiled with Lukka's first ability, you can cast it as long as you control any Lukka planeswalker, not just the one that exiled the card.")
    }
}
