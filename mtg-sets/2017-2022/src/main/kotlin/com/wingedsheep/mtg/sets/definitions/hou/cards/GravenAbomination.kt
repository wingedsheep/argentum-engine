package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Graven Abomination
 * {3}
 * Artifact Creature — Horror
 * 3/1
 * Whenever this creature attacks, exile target card from defending player's graveyard.
 *
 * "Defending player's graveyard" is `ownedBy(PlayerRef(DefendingPlayer))`, resolved off the
 * attacking source at target selection — the same shape as Hive of the Eye Tyrant's granted trigger.
 */
val GravenAbomination = card("Graven Abomination") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Horror"
    oracleText = "Whenever this creature attacks, exile target card from defending player's graveyard."
    power = 3
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val card = target(TargetFilter.CardInGraveyard.ownedBy(EffectTarget.PlayerRef(Player.DefendingPlayer)))
        effect = Effects.Exile(card)
        description = "Whenever this creature attacks, exile target card from defending player's graveyard."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Igor Kieryluk"
        flavorText = "Ironically, the statues had been constructed as a warning."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89880f30-aa38-4231-8d16-25c644bde6bf.jpg?1783936002"
    }
}
