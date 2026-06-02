package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.YouAttackEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Landroval, Horizon Witness
 * {4}{W}
 * Legendary Creature — Bird Noble
 * 3/4
 * Flying
 * Whenever two or more creatures you control attack a player, target attacking creature
 * without flying gains flying until end of turn.
 */
val LandrovalHorizonWitness = card("Landroval, Horizon Witness") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Bird Noble"
    power = 3
    toughness = 4
    oracleText = "Flying\nWhenever two or more creatures you control attack a player, target attacking creature without flying gains flying until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = TriggerSpec(YouAttackEvent(minAttackers = 2), TriggerBinding.ANY)
        val creature = target(
            "target attacking creature without flying",
            TargetCreature(filter = TargetFilter.AttackingCreature.withoutKeyword(Keyword.FLYING))
        )
        effect = Effects.GrantKeyword(Keyword.FLYING, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "John Tedrick"
        flavorText = "He passed over Udûn and Gorgoroth and saw all the land in ruin and tumult beneath him, and before him Mount Doom blazing, pouring out its fire."
        imageUri = "https://cards.scryfall.io/normal/front/5/6/5684483b-9a6a-499b-a5e1-e2815ee03cdb.jpg?1686967838"
    }
}
