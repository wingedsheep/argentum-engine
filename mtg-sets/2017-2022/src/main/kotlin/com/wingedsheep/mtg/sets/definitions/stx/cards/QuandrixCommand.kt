package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Quandrix Command — Strixhaven: School of Mages #217 (canonical printing)
 * {1}{G}{U} · Instant
 *
 * Choose two —
 * • Return target creature or planeswalker to its owner's hand.
 * • Counter target artifact or enchantment spell.
 * • Put two +1/+1 counters on target creature.
 * • Target player shuffles up to three target cards from their graveyard into their library.
 *
 * The fourth mode's card slot is `ownedByTargetPlayer()` in the graveyard (Drafna's Restoration's
 * shape): the cards are offered only once the player is chosen, so they come from that player's
 * graveyard. The library shuffles even when every card target has become illegal, as long as the
 * player is still a legal target (ruling 2021-04-16).
 */
val QuandrixCommand = card("Quandrix Command") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText =
        "Choose two —\n" +
        "• Return target creature or planeswalker to its owner's hand.\n" +
        "• Counter target artifact or enchantment spell.\n" +
        "• Put two +1/+1 counters on target creature.\n" +
        "• Target player shuffles up to three target cards from their graveyard into their library."

    spell {
        modal(chooseCount = 2) {
            mode("Return target creature or planeswalker to its owner's hand") {
                val permanent = target(Targets.CreatureOrPlaneswalker)
                effect = Effects.ReturnToHand(permanent)
            }
            mode("Counter target artifact or enchantment spell") {
                target(TargetFilter(GameObjectFilter.ArtifactOrEnchantment, zone = Zone.STACK))
                effect = Effects.CounterSpell()
            }
            mode("Put two +1/+1 counters on target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
            }
            mode("Target player shuffles up to three target cards from their graveyard into their library") {
                val player = target(Targets.Player)
                val cards = targets(
                    TargetFilter(GameObjectFilter.Any.ownedByTargetPlayer(), zone = Zone.GRAVEYARD),
                    count = 3,
                    optional = true,
                )
                effect = Effects.ForEachTargetOf(cards, Effects.Move(EffectTarget.IterationEntity, Zone.LIBRARY)) then
                    Effects.ShuffleLibrary(player)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/021b62d8-d160-47f5-bc51-0474f160d13f.jpg?1783927299"
        ruling("2021-04-16", "If at least one target is still legal as Quandrix Command tries to resolve, it will resolve and do as much as it can.")
        ruling("2021-04-16", "As Quandrix Command tries to resolve, if the fourth mode is chosen and all of the target cards are illegal, but the target player is still legal, that player will still shuffle their library.")
        ruling("2021-04-16", "If all of Quandrix Command's targets are illegal as it tries to resolve, it will do nothing.")
    }
}
