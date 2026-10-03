package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Diregraf Captain
 * {1}{U}{B}
 * Creature — Zombie Soldier
 * 2/2
 * Deathtouch
 * Other Zombie creatures you control get +1/+1.
 * Whenever another Zombie you control dies, target opponent loses 1 life.
 */
val DiregrafCaptain = card("Diregraf Captain") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Zombie Soldier"
    oracleText =
        "Deathtouch\n" +
            "Other Zombie creatures you control get +1/+1.\n" +
            "Whenever another Zombie you control dies, target opponent loses 1 life."
    power = 2
    toughness = 2

    keywords(Keyword.DEATHTOUCH)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE).youControl(),
                excludeSelf = true,
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE).youControl()).dies()
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(1, opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Slawomir Maniak"
        flavorText =
            "Though its mind has long since rotted away, it wields a sword with devastating skill."
        imageUri =
            "https://cards.scryfall.io/normal/front/0/e/0e5f41eb-609b-4882-af9e-904daa717484.jpg?1562898409"
    }
}
