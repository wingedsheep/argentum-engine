package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalETBOrLTBTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SuppressEntersTriggers

/**
 * Elesh Norn, Mother of Machines
 * {4}{W}
 * Legendary Creature — Phyrexian Praetor
 * 4/7
 * Vigilance
 * If a permanent entering causes a triggered ability of a permanent you control to trigger, that
 * ability triggers an additional time.
 * Permanents entering don't cause abilities of permanents your opponents control to trigger.
 *
 * The doubling half is the Panharmonicon family with the "you control" restriction lifted off the
 * *entering* permanent ([AdditionalETBOrLTBTriggers] with `mustBeYouControl = false`, as Starfield
 * Vocalist). The lock half is Torpor Orb narrowed by `abilitiesOf` to triggers whose source is a
 * permanent an opponent controls.
 */
val EleshNornMotherOfMachines = card("Elesh Norn, Mother of Machines") {
    manaCost = "{4}{W}"
    typeLine = "Legendary Creature — Phyrexian Praetor"
    power = 4
    toughness = 7
    oracleText = "Vigilance\n" +
        "If a permanent entering causes a triggered ability of a permanent you control to trigger, " +
        "that ability triggers an additional time.\n" +
        "Permanents entering don't cause abilities of permanents your opponents control to trigger."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = AdditionalETBOrLTBTriggers(GameObjectFilter.Permanent, mustBeYouControl = false)
    }

    staticAbility {
        ability = SuppressEntersTriggers(
            GameObjectFilter.Permanent,
            abilitiesOf = GameObjectFilter.Permanent.opponentControls()
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "10"
        artist = "Martina Fačková"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44dcab01-1d13-4dfc-ae2f-fbaa3dd35087.jpg?1783918082"
    }
}
