package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Swathcutter Giant
 * {4}{R}{W}
 * Creature — Giant Soldier
 * 5/5
 * Vigilance
 * Whenever this creature attacks, it deals 1 damage to each creature defending player controls.
 *
 * "Defending player" is the player this Giant attacks (CR 508.5), so the referenced-player filter
 * is used rather than `defendingPlayerControls()`, which would sweep every defender in multiplayer.
 * The affected creatures are fixed at resolution.
 */
val SwathcutterGiant = card("Swathcutter Giant") {
    manaCost = "{4}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Giant Soldier"
    power = 5
    toughness = 5
    oracleText = "Vigilance\n" +
        "Whenever this creature attacks, it deals 1 damage to each creature defending player controls."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.dealDamageToAll(
            1,
            GroupFilter(
                GameObjectFilter.Creature.targetPlayerControls(EffectTarget.PlayerRef(Player.DefendingPlayer))
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        artist = "Aaron Miller"
        flavorText = "\"Now do you understand what we meant when we said *disperse*?\"\n—Eksari, Boros patrol leader"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/700b38c5-3fd2-4566-8d27-9a09d5ab02b2.jpg?1783934121"
    }
}
