package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Folk Hero — Commander Legends: Battle for Baldur's Gate #650
 * {1}{W} · Legendary Enchantment — Background
 *
 * Commander creatures you own have "Whenever you cast a spell that shares a creature type with
 * this creature, draw a card. This ability triggers only once each turn."
 *
 * The ability lives on each commander creature you own ([GrantTriggeredAbility] over
 * `Creature.commander().ownedByYou()`), so "this creature" is the commander it rides on —
 * [CardPredicate.SharesCreatureTypeWithSource] reads the granted ability's source — and "you" is
 * that creature's controller. With two commander creatures, each has its own once-per-turn draw
 * (ruling).
 */
val FolkHero = card("Folk Hero") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Enchantment — Background"
    oracleText = "Commander creatures you own have \"Whenever you cast a spell that shares a creature type " +
        "with this creature, draw a card. This ability triggers only once each turn.\""

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.you.casts(
                    GameObjectFilter.Any.withCardPredicate(CardPredicate.SharesCreatureTypeWithSource)
                ),
                effect = Effects.DrawCards(1),
                oncePerTurn = true,
                descriptionOverride = "Whenever you cast a spell that shares a creature type with this " +
                    "creature, draw a card. This ability triggers only once each turn.",
            ),
            filter = GroupFilter(GameObjectFilter.Creature.commander().ownedByYou()),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "650"
        artist = "Kai Carpenter"
        flavorText = "Tales of your great deeds follow you no matter how far you travel from your hometown."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8b3aae8-fc36-45ed-84ed-efb8f543cada.jpg?1783922517"
        ruling("2022-06-10", "If a card refers to a commander creature you own, a Background won't usually be counted or included for that effect. If another spell or ability causes your Background to become a creature, however, it will be included.")
        ruling("2022-06-10", "If you control a Background that grants an ability to commander creatures you own, and you own more than one commander creature, each of them will have that ability.")
    }
}
