package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val DesdemonaFreedomsEdge = card("Desdemona, Freedom's Edge") {
    manaCost = "{2}{R}{W}"
    typeLine = "Legendary Creature — Human Rogue"
    power = 3
    toughness = 4
    oracleText = "Vigilance\nWhenever Desdemona attacks, target creature card in your graveyard that's an artifact or that has mana value 3 or less gains escape until end of turn. The escape cost is equal to its mana cost plus exile two other cards from your graveyard. (You may cast it from your graveyard for its escape cost this turn.)"

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val card = target(
            TargetFilter(
                GameObjectFilter.Creature and (GameObjectFilter.Artifact or GameObjectFilter.Any.manaValueAtMost(3)),
                zone = Zone.GRAVEYARD,
            ).ownedByYou()
        )
        effect = Effects.GrantEscape(card, additionalCost = Costs.additional.ExileOtherCards(2))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Anton Solovianchyk"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc38d445-1752-4921-bb83-5afcdf655015.jpg?1783912390"
        ruling("2024-03-08", "If a spell you're casting with escape has an additional cost of discarding cards or sacrificing permanents, you may exile cards discarded or sacrificed this way to pay that part of its escape cost.")
        ruling("2024-03-08", "Escape's permission doesn't change when you may cast the spell from your graveyard.")
        ruling("2024-03-08", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying (such as an escape cost), add any cost increases, then apply any cost reductions. The mana value of the spell remains unchanged, no matter what the total cost to cast it was and no matter whether an alternative cost was paid.")
        ruling("2024-03-08", "After an escaped permanent spell resolves, it enters the battlefield and will return to its owner's graveyard if it dies later.")
        ruling("2024-03-08", "If a card has multiple abilities giving you permission to cast it, such as two escape abilities or an escape ability and a flashback ability, you choose which one to apply. The others have no effect.")
        ruling("2024-03-08", "If you cast a spell with its escape permission, you can't choose to apply any other alternative costs or to cast it without paying its mana cost. You may pay any optional additional costs the spell has, such as kicker costs. If it has any mandatory additional costs, you must pay those.")
        ruling("2024-03-08", "Once you begin casting a spell with escape, it immediately moves to the stack. Players can't take any other actions until you're done casting the spell.")
    }
}
