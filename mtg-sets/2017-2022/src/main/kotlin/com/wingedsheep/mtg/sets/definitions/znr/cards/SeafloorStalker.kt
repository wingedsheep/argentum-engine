package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Seafloor Stalker — Zendikar Rising #78
 * {2}{U} · Creature — Merfolk Rogue · 2/3
 *
 * {4}{U}: This creature gets +1/+0 until end of turn and can't be blocked this turn. This ability
 * costs {1} less to activate for each creature in your party.
 *
 * The discount rides the activated ability's `genericCostReduction` (Towashi Guide-Bot's shape)
 * over the party size, so only the generic {4} shrinks — never below {U}.
 */
val SeafloorStalker = card("Seafloor Stalker") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Rogue"
    oracleText = "{4}{U}: This creature gets +1/+0 until end of turn and can't be blocked this turn. " +
        "This ability costs {1} less to activate for each creature in your party. " +
        "(Your party consists of up to one each of Cleric, Rogue, Warrior, and Wizard.)"
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Mana("{4}{U}")
        genericCostReduction = DynamicAmounts.partySize()
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self) then
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        description = "{4}{U}: This creature gets +1/+0 until end of turn and can't be blocked this turn. " +
            "This ability costs {1} less to activate for each creature in your party."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Cristi Balanescu"
        flavorText = "\"Sunken ruins? My specialty.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/8/985e9696-6c76-4241-97fc-f166fe2420ce.jpg?1783929387"
        ruling("2020-09-25", "Once you declare that you're activating Seafloor Stalker's ability, no player can take actions to try and change the number of creatures in your party and change the activation cost.")
        ruling("2020-09-25", "Once a creature has blocked Seafloor Stalker, activating its ability won't cause it to become unblocked or remove that blocking creature from combat.")
        ruling("2020-09-25", "You can activate Seafloor Stalker's ability more than once, even if it already can't be blocked this turn.")
    }
}
