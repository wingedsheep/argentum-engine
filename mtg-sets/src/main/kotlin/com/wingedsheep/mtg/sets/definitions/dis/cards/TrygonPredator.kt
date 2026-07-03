package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Trygon Predator
 * {1}{G}{U}
 * Creature — Beast
 * 2/3
 *
 * Flying
 * Whenever this creature deals combat damage to a player, you may destroy target artifact or
 * enchantment that player controls.
 *
 * The trigger is SELF-bound. On this engine's singular combat-damage path the damaged player is
 * carried as the *triggering entity*, not the triggering player, so `Player.TriggeringPlayer`
 * can't scope the target filter here (it stays null). We therefore scope to artifacts/enchantments
 * an opponent controls — identical to the damaged player in a two-player game, and matching the
 * established precedent for this exact card shape (Dawning Purist). "You may" is the [MayEffect]
 * wrapper; if the opponent controls no artifact or enchantment the ability never goes on the
 * stack (CR 603.3d).
 */
val TrygonPredator = card("Trygon Predator") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Beast"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, you may destroy target " +
        "artifact or enchantment that player controls."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        val t = target(
            "target artifact or enchantment that player controls",
            TargetPermanent(filter = TargetFilter.ArtifactOrEnchantment.opponentControls()),
        )
        effect = MayEffect(Effects.Destroy(t))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Carl Critchlow"
        flavorText = "Held aloft by metabolized magic, trygons are ravenous for sources of mystic fuel."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f31f54bf-7bf0-48f0-853d-1468713784eb.jpg?1782717143"
    }
}
