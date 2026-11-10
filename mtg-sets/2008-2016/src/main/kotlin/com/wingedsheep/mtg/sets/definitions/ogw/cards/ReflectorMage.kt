package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reflector Mage
 * {1}{W}{U}
 * Creature — Human Wizard
 * 2/3
 * When this creature enters, return target creature an opponent controls to its owner's hand.
 * That creature's owner can't cast spells with the same name as that creature until your next turn.
 *
 * The ban names the creature as it was on the battlefield (2016-01-22 rulings): a Clone copying
 * something bans the copied name, a transformed double-faced card bans its back face's name, and a
 * face-down creature has no name, so it bans nothing. The ban is therefore recorded before the
 * bounce, while the creature is still there to be read — `CantCastSpells` freezes the target's
 * name the moment it resolves. Both happen in the same resolution, so no player can act between
 * them. It stops spells only: the returned card, if it is also a land, can still be played.
 */
val ReflectorMage = card("Reflector Mage") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, return target creature an opponent controls to its owner's hand. " +
        "That creature's owner can't cast spells with the same name as that creature until your next turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.CantCastSpells(
            target = EffectTarget.PlayerRef(Player.OwnerOf("target creature an opponent controls")),
            duration = Duration.UntilYourNextTurn,
            spellFilter = GameObjectFilter.Any.sharingNameWith(creature),
        ) then Effects.ReturnToHand(creature)
        description = "When this creature enters, return target creature an opponent controls to its owner's hand. " +
            "That creature's owner can't cast spells with the same name as that creature until your next turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/9473fe01-83f6-4432-ab01-f7953d2ca904.jpg?1783937896"
    }
}
