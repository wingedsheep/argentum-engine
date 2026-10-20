package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Remove Enchantments
 * {W}
 * Instant
 * Return to your hand all enchantments you both own and control, all Auras you own attached to
 * permanents you control, and all Auras you own attached to attacking creatures your opponents
 * control. Then destroy all other enchantments you control, all other Auras attached to permanents
 * you control, and all other Auras attached to attacking creatures your opponents control.
 *
 * Two sequential sweeps. The first returns everything in the three "you own" groups; the second
 * re-scans the battlefield with the same three groups minus the ownership gate, so "all other" is
 * exactly what the return left behind — the returned cards are already in hand and can't match.
 */
val RemoveEnchantments = card("Remove Enchantments") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Return to your hand all enchantments you both own and control, all Auras you own " +
        "attached to permanents you control, and all Auras you own attached to attacking creatures " +
        "your opponents control. Then destroy all other enchantments you control, all other Auras " +
        "attached to permanents you control, and all other Auras attached to attacking creatures " +
        "your opponents control."

    spell {
        val aura = GameObjectFilter.Enchantment.withSubtype("Aura")
        val auraOnYourPermanent = aura.attachedTo(GameObjectFilter.Permanent.youControl())
        val auraOnOpponentsAttacker = aura.attachedTo(GameObjectFilter.Creature.attacking().opponentControls())

        val returned = GameObjectFilter.Enchantment.withControllerPredicate(
            ControllerPredicate.And(listOf(ControllerPredicate.OwnedByYou, ControllerPredicate.ControlledByYou))
        ) or auraOnYourPermanent.ownedByYou() or auraOnOpponentsAttacker.ownedByYou()

        val destroyed = GameObjectFilter.Enchantment.youControl() or
            auraOnYourPermanent or
            auraOnOpponentsAttacker

        effect = Patterns.Group.returnAllToHand(GroupFilter(returned)) then
            Effects.DestroyAll(destroyed)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf2e3a8a-b386-474d-b8e9-4c2d56a2b742.jpg?1783948081"
    }
}
