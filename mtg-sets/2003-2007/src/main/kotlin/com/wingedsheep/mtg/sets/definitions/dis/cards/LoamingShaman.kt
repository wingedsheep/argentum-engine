package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Loaming Shaman
 * {2}{G}
 * Creature — Centaur Shaman
 * 3/2
 * When this creature enters, target player shuffles any number of target cards from their
 * graveyard into their library.
 *
 * The card slot reads the player slot (`ownedByTargetPlayer`), so only cards from the chosen
 * player's graveyard are offered. The player shuffles even when no cards are chosen (ruling).
 */
val LoamingShaman = card("Loaming Shaman") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Shaman"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, target player shuffles any number of target cards from their graveyard into their library."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        val cards = targets(
            TargetFilter(GameObjectFilter.Any.ownedByTargetPlayer(), zone = Zone.GRAVEYARD),
            unlimited = true,
        )
        effect = Effects.ForEachTargetOf(cards, Effects.Move(EffectTarget.IterationEntity, Zone.LIBRARY)) then
            Effects.ShuffleLibrary(player)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Carl Critchlow"
        flavorText = "His work determines who is remembered and who feeds the worms."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e34b3f97-1c00-4ad2-a28d-6f064f499761.jpg?1783943411"
        ruling("2019-07-12", "If Loaming Shaman's triggered ability targets a player but doesn't target any cards in that player's graveyard, that player just shuffles their library.")
        ruling("2019-07-12", "If the target cards become illegal targets, the target player shuffles their library. If the target player becomes an illegal target, Loaming Shaman's ability has no effect.")
    }
}
