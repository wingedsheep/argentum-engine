package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spiteful Hexmage
 * {B}
 * Creature — Human Warlock
 * 3/2
 *
 * When this creature enters, create a Cursed Role token attached to target creature you control.
 * (If you control another Role on it, put that one into the graveyard. Enchanted creature is 1/1.)
 *
 * The target is *not* "another" creature — the Hexmage is a legal target for its own trigger,
 * so [Targets.CreatureYouControl] (no `excludeSelf`) is correct. Replacing an existing Role on
 * the chosen creature is handled inside [Effects.CreateRoleToken], not here.
 */
val SpitefulHexmage = card("Spiteful Hexmage") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    oracleText = "When this creature enters, create a Cursed Role token attached to target creature you control. " +
        "(If you control another Role on it, put that one into the graveyard. Enchanted creature is 1/1.)"
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.CreateRoleToken("Cursed Role", t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Anna Steinbauer"
        flavorText = "\"Who'll never amount to anything now, Father?\""
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40c797b2-db51-4a39-b80e-44d58cd7a07c.jpg?1783915102"
    }
}
