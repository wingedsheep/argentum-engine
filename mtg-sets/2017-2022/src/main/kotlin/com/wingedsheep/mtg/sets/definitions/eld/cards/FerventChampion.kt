package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceEquipCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fervent Champion — Throne of Eldraine #124
 * {R} · Creature — Human Knight · 1/1
 *
 * First strike, haste
 * Whenever this creature attacks, another target attacking Knight you control gets +1/+0 until end of turn.
 * Equip abilities you activate that target this creature cost {3} less to activate.
 *
 * The attack trigger targets an attacking Knight you control, excluding the Champion itself via
 * `excludeSelf`. The equip discount is the target-restricted [ReduceEquipCost] (Cloud, Planet's
 * Champion): `targetFilter = sourceItself()` limits the {3} reduction to equip abilities targeting it.
 */
val FerventChampion = card("Fervent Champion") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Knight"
    power = 1
    toughness = 1
    oracleText = "First strike, haste\n" +
        "Whenever this creature attacks, another target attacking Knight you control gets +1/+0 until end of turn.\n" +
        "Equip abilities you activate that target this creature cost {3} less to activate."

    keywords(Keyword.FIRST_STRIKE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val knight = target(
            TargetFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.KNIGHT).attacking().youControl(),
                excludeSelf = true
            )
        )
        effect = Effects.ModifyStats(1, 0, knight)
    }

    staticAbility {
        ability = ReduceEquipCost(amount = 3, targetFilter = GameObjectFilter.Any.sourceItself())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Steve Argyle"
        flavorText = "2018 World Champion Javier Domínguez"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c52d66db-5570-48a1-99cf-e0417517747b.jpg?1783932624"
    }
}
