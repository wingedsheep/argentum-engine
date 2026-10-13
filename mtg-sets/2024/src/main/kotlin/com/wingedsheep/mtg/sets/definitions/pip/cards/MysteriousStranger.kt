package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.OwnerDistribution

val MysteriousStranger = card("Mysterious Stranger") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 2
    oracleText = "Flash\nWhen this creature enters, for each graveyard with an instant or sorcery card in it, exile target instant or sorcery card from that graveyard. If two or more cards are exiled this way, choose one of them at random and copy it. You may cast the copy without paying its mana cost."
    keywords(Keyword.FLASH)
    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter(GameObjectFilter.InstantOrSorcery, zone = Zone.GRAVEYARD),
            ownerDistribution = OwnerDistribution.EXACTLY_ONE)
        effect = Effects.Pipeline {
            val targets = gather(CardSource.ChosenTargets)
            val exiled = moveTracked(targets, CardDestination.ToZone(Zone.EXILE))
            ifNotEmpty(exiled, minSize = 2) {
                val chosen = chooseRandom(1, exiled)
                val copies = copyCards(chosen)
                run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(copies)))
            }
        }
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Xavier Ribeiro"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/063ca0fa-1947-48da-86f9-76dd00dfb550.jpg?1783912403"
        ruling("2024-03-08", "You cast the copy while Mysterious Stranger’s last ability is resolving and still on the stack. You can’t wait to cast it later in the turn.")
        ruling("2024-03-08", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2024-03-08", "If you cast a spell “without paying its mana cost,” you can’t choose to cast it for any alternative costs. You can, however, pay additional costs. If the spell has any mandatory additional costs, those must be paid to cast the card.")
        ruling("2024-03-08", "If you don’t want to cast the copy, you can choose not to; the copy ceases to exist the next time state-based actions are checked.")
    }
}
