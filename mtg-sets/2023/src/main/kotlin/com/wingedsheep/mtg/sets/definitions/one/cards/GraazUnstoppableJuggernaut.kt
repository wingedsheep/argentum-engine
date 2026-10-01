package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Graaz, Unstoppable Juggernaut — Phyrexia: All Will Be One #229
 * {8}
 * Legendary Artifact Creature — Juggernaut
 * 7/5
 *
 * Juggernauts you control attack each combat if able.
 * Juggernauts you control can't be blocked by Walls.
 * Other creatures you control have base power and toughness 5/3 and are Juggernauts in addition
 * to their other creature types.
 *
 * The "Juggernauts you control" groups are read against projected subtypes, so the creatures the
 * third ability turns into Juggernauts (Layer 4) are also bound by the attack requirement and get
 * the Wall evasion. The base P/T is a Layer 7b set over the other creatures you control.
 */
private val juggernautsYouControl =
    GroupFilter(GameObjectFilter.Creature.withSubtype("Juggernaut").youControl())

val GraazUnstoppableJuggernaut = card("Graaz, Unstoppable Juggernaut") {
    manaCost = "{8}"
    typeLine = "Legendary Artifact Creature — Juggernaut"
    power = 7
    toughness = 5
    oracleText = "Juggernauts you control attack each combat if able.\n" +
        "Juggernauts you control can't be blocked by Walls.\n" +
        "Other creatures you control have base power and toughness 5/3 and are Juggernauts in " +
        "addition to their other creature types."

    staticAbility {
        ability = MustAttack(juggernautsYouControl)
    }

    staticAbility {
        ability = CantBeBlockedBy(
            blockerFilter = GameObjectFilter.Creature.withSubtype(Subtype.WALL),
            filter = juggernautsYouControl
        )
    }

    staticAbility {
        ability = SetBasePowerToughnessStatic(5, 3, GroupFilter.OtherCreaturesYouControl)
    }

    staticAbility {
        ability = GrantSubtype("Juggernaut", GroupFilter.OtherCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0e73b63-17cc-4dca-abd6-728b74bc37a8.jpg?1783917991"
        ruling(
            "2023-02-04",
            "If Graaz loses its abilities for some reason, then Juggernauts you control will not have to " +
                "attack each combat and will be able to be blocked by Walls. However, due to how continuous " +
                "effects are applied in layers, other creatures you control will continue to be 5/3 " +
                "Juggernauts in addition to their other types."
        )
    }
}
