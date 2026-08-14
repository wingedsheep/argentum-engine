package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scalding Viper // Steam Clean
 * {1}{R}
 * Creature — Elemental Snake
 * 2/1
 * Whenever an opponent casts a spell with mana value 3 or less, this creature deals 1 damage to
 * that player.
 *
 * Adventure: Steam Clean — {1}{U}, Sorcery — Adventure
 * Return target nonland permanent to its owner's hand.
 *
 * The trigger is a cast watcher scoped to opponents ([Triggers.opponentCasts]) narrowed by mana
 * value; "that player" is the caster ([Player.TriggeringPlayer]), not a target, so the ability
 * never fizzles. Per the WOE rulings, a spell with {X} in its cost uses the chosen X when its mana
 * value is computed, which is what the spell-on-stack the filter reads reports.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val ScaldingViper = card("Scalding Viper") {
    manaCost = "{1}{R}"
    colorIdentity = "RU"
    typeLine = "Creature — Elemental Snake"
    oracleText = "Whenever an opponent casts a spell with mana value 3 or less, " +
        "this creature deals 1 damage to that player."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.opponentCasts(GameObjectFilter.Any.manaValueAtMost(3))
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    adventure("Steam Clean") {
        manaCost = "{1}{U}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Return target nonland permanent to its owner's hand. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target("target", Targets.NonlandPermanent)
            effect = Effects.ReturnToHand(t)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "235"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58e72bfb-6f64-4647-afb6-b5ad4737121c.jpg?1783915062"
    }
}
