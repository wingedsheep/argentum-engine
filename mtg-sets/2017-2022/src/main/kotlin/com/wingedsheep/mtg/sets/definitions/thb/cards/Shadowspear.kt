package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Shadowspear
 * {1}
 * Legendary Artifact — Equipment
 * Equipped creature gets +1/+1 and has trample and lifelink.
 * {1}: Permanents your opponents control lose hexproof and indestructible until end of turn.
 * Equip {2}
 *
 * Loxodon Warhammer's equipment statics plus The Fire Nation Drill's hexproof/indestructible strip:
 * [Patterns.Group.removeKeywordFromAll] snapshots the opponents' permanents as it resolves, so a
 * permanent that enters or gains either keyword afterwards keeps it (ruling).
 */
val Shadowspear = card("Shadowspear") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 and has trample and lifelink.\n" +
        "{1}: Permanents your opponents control lose hexproof and indestructible until end of turn.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK, Filters.EquippedCreature)
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Patterns.Group.removeKeywordFromAll(
            Keyword.HEXPROOF,
            Filters.Group.permanents { opponentControls() }
        ) then
            Patterns.Group.removeKeywordFromAll(
                Keyword.INDESTRUCTIBLE,
                Filters.Group.permanents { opponentControls() }
            )
        description = "{1}: Permanents your opponents control lose hexproof and indestructible until end of turn."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "236"
        artist = "Yeong-Hao Han"
        flavorText = "A weapon of darkness for a warrior of light."
        imageUri = "https://cards.scryfall.io/normal/front/9/3/939c6e19-4b27-4023-bb9c-ae440f91e21c.jpg?1783931515"
        ruling(
            "2020-01-24",
            "If a permanent enters the battlefield under an opponent's control with hexproof or indestructible " +
                "after Shadowspear's second ability resolves, it won't lose that ability unless you activate " +
                "Shadowspear's second ability again. The same is true if an opponent's permanent gains " +
                "indestructible or hexproof after Shadowspear's second ability resolves."
        )
        ruling(
            "2020-01-24",
            "You can activate Shadowspear's second ability whether or not it's equipped to a creature."
        )
    }
}
