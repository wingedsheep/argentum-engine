package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Colossal Whale
 * {5}{U}{U}
 * Creature — Whale
 * 5/5
 * Islandwalk
 * Whenever this creature attacks, you may exile target creature defending player controls until
 * this creature leaves the battlefield.
 *
 * A single zone change with a duration: `MoveUntilSourceLeaves` returns the card immediately when
 * the Whale leaves (no separate return trigger), and moves nothing if the Whale has already left
 * by the time the ability resolves — both per the 2013-07-01 rulings. The defending-player scope
 * is Necrite's `ControlledByReferencedPlayer(PlayerRef(DefendingPlayer))`.
 */
val ColossalWhale = card("Colossal Whale") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Whale"
    power = 5
    toughness = 5
    oracleText = "Islandwalk (This creature can't be blocked as long as defending player controls an Island.)\n" +
        "Whenever this creature attacks, you may exile target creature defending player controls until this " +
        "creature leaves the battlefield. (That creature returns under its owner's control.)"

    keywords(Keyword.ISLANDWALK)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.ControlledByReferencedPlayer(
                        EffectTarget.PlayerRef(Player.DefendingPlayer)
                    )
                )
            )
        )
        effect = Effects.May(Effects.MoveUntilSourceLeaves(creature, Zone.EXILE))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7f7caca-14ee-4d6a-97c3-e19898f86635.jpg?1783939936"

        ruling("2013-07-01", "If a creature token is exiled, it ceases to exist. It won't be returned to the battlefield.")
        ruling("2013-07-01", "Colossal Whale's triggered ability triggers and resolves during the declare attackers step, before blocking creatures are declared.")
        ruling("2013-07-01", "Auras attached to the exiled creature will be put into their owners' graveyards. Equipment attached to the exiled creature will become unattached and remain on the battlefield. Any counters on the exiled creature will cease to exist.")
        ruling("2013-07-01", "If Colossal Whale leaves the battlefield before its triggered ability resolves, the target creature won't be exiled.")
        ruling("2013-07-01", "The exiled card returns to the battlefield immediately after Colossal Whale leaves the battlefield. Nothing happens between the two events, including state-based actions. The two creatures aren't on the battlefield at the same time.")
    }
}
