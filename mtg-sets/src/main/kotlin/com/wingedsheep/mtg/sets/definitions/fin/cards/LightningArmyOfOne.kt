package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lightning, Army of One
 * {1}{R}{W}
 * Legendary Creature — Human Soldier
 * 3/2
 * First strike, trample, lifelink
 * Stagger — Whenever Lightning deals combat damage to a player, until your next turn, if a source
 * would deal damage to that player or a permanent that player controls, it deals double that damage
 * instead.
 *
 * "Stagger" is an ability word (no rules meaning); the trigger installs a duration-bounded,
 * player-scoped damage-doubling replacement via [Effects.DoubleDamageToPlayer]. The affected player
 * is [Player.TriggeringPlayer] — the player just dealt combat damage — and the doubling lasts
 * `until your next turn` and applies to every source's damage (combat or noncombat) to that player
 * or a permanent they control. Because it is a floating effect independent of Lightning, it keeps
 * doubling even if Lightning later leaves the battlefield (CR 611.2). Per the printed rulings, combat
 * damage is assigned/divided before doubling (the read site sees per-recipient assigned amounts) and
 * the doubled damage is still dealt by the original source, not by Lightning.
 */
val LightningArmyOfOne = card("Lightning, Army of One") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "First strike, trample, lifelink\n" +
        "Stagger — Whenever Lightning deals combat damage to a player, until your next turn, if a " +
        "source would deal damage to that player or a permanent that player controls, it deals " +
        "double that damage instead."

    keywords(Keyword.FIRST_STRIKE, Keyword.TRAMPLE, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.DoubleDamageToPlayer(EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "233"
        artist = "Shiyu"
        flavorText = "\"Lightning. It flashes bright, then fades away. It can't protect. It only destroys.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/1/1103da9c-300c-406b-997d-9e5bb7cd02d6.jpg?1782686417"
    }
}
