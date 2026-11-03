package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Antagonist Incorporator
 * {1}{W}
 * Artifact Creature — Borg
 * 2/3
 *
 * Artifact creatures you control have "Whenever this creature deals combat damage to a player,
 * you may pay {1}. If you do, create a 1/1 colorless Borg artifact creature token."
 *
 * The granted ability belongs to each artifact creature (itself included), so "you" is that
 * creature's controller — always this card's controller, given the "you control" filter.
 */
val AntagonistIncorporator = card("Antagonist Incorporator") {
    manaCost = "{1}{W}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "W"
    oracleText = "Artifact creatures you control have \"Whenever this creature deals combat damage " +
        "to a player, you may pay {1}. If you do, create a 1/1 colorless Borg artifact creature token.\""
    power = 2
    toughness = 3
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                effect = Effects.MayPay(
                    cost = Effects.PayMana("{1}"),
                    then = Effects.CreateToken(
                        power = 1, toughness = 1,
                        creatureTypes = setOf("Borg"), artifactToken = true
                    )
                )
            ),
            filter = GroupFilter(GameObjectFilter.ArtifactCreature.youControl())
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Miklós Ligeti"
        flavorText = "There are no dead. Only raw materials."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63918e59-0c03-4d98-9216-e58ee157b7ea.jpg?1792668297"
    }
}
