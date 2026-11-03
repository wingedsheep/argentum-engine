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
 * Hive Mind Coprocessor
 * {4}{B}
 * Artifact Creature — Borg
 * 4/4
 *
 * Artifact creatures you control have "Whenever this creature deals combat damage to a player,
 * you may pay 1 life. If you do, draw a card."
 *
 * A static [GrantTriggeredAbility] over the artifact creatures you control (itself included); the
 * granted trigger is self-bound, and the "may pay 1 life. If you do" is [Effects.MayPay].
 */
val HiveMindCoprocessor = card("Hive Mind Coprocessor") {
    manaCost = "{4}{B}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "B"
    power = 4
    toughness = 4
    oracleText = "Artifact creatures you control have \"Whenever this creature deals combat damage to a " +
        "player, you may pay 1 life. If you do, draw a card.\""

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                effect = Effects.MayPay(
                    cost = Effects.PayLife(1),
                    then = Effects.DrawCards(1),
                ),
            ),
            filter = GroupFilter(GameObjectFilter.ArtifactCreature.youControl()),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Andrey Kuzinskiy"
        flavorText = "\"Consensus achieved. Disseminating.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4734c59-ff41-47ac-bbb4-c8c3bf052fe6.jpg?1785981497"
    }
}
