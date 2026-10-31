package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.RemoveKeywordStatic

/**
 * Colossus Hammer
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +10/+10 and loses flying.
 * Equip {8}
 *
 * Same "gets +N/+N and loses flying" shape as Starforged Sword: a layer-7c stat bonus plus a
 * layer-6 keyword removal, both on the attached creature. Flying granted later carries a later
 * timestamp and survives (ruling below).
 */
val ColossusHammer = card("Colossus Hammer") {
    manaCost = "{1}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +10/+10 and loses flying.\n" +
        "Equip {8} ({8}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(10, 10)
    }

    staticAbility {
        ability = RemoveKeywordStatic(Keyword.FLYING)
    }

    equipAbility("{8}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Dmitry Burmak"
        flavorText = "\"There's only one way to be sure it's really dead.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa35b2b5-3e91-4a6c-90b1-8581b4ecaf8b.jpg?1783932945"
        ruling("2019-07-12", "Colossus Hammer can be attached to a creature that didn't have flying to begin with. It just won't lose flying.")
        ruling("2019-07-12", "If the equipped creature gains flying after Colossus Hammer became attached to it, it will keep flying.")
        ruling("2019-07-12", "If an ability of the equipped creature states that it has flying \"as long as\" a particular condition is true, that condition becoming true will not cause that creature to gain flying. For example, if the equipped creature \"has flying as long as it's attacking,\" it will not gain flying, even if it attacks after Colossus Hammer becomes attached to it.")
    }
}
