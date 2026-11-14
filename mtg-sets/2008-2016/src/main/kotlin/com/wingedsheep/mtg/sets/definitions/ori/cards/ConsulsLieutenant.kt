package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Consul's Lieutenant
 * {W}{W}
 * Creature — Human Soldier
 * 2/1
 *
 * First strike
 * Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)
 * Whenever this creature attacks, if it's renowned, other attacking creatures you control get +1/+1 until end of turn.
 */
val ConsulsLieutenant = card("Consul's Lieutenant") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "First strike\n" +
        "Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)\n" +
        "Whenever this creature attacks, if it's renowned, other attacking creatures you control get +1/+1 until end of turn."
    power = 2
    toughness = 1

    keywords(Keyword.FIRST_STRIKE)
    keywordAbility(KeywordAbility.renown(1))

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.SourceIsRenowned
        effect = Patterns.Group.modifyStatsForAll(
            power = 1,
            toughness = 1,
            filter = GroupFilter(GameObjectFilter.Creature.attacking().youControl(), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/301a1b68-2890-4702-a90e-bad8d309c40d.jpg?1783938363"
    }
}
