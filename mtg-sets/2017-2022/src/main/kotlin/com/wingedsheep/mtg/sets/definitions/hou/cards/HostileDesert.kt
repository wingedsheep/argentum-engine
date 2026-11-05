package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hostile Desert — Hour of Devastation #178 (canonical printing; C20 is a later reprint)
 * Land — Desert
 * {T}: Add {C}.
 * {2}, Exile a land card from your graveyard: This land becomes a 3/4 Elemental creature until
 * end of turn. It's still a land.
 *
 * The Mishra's Factory / Stalking Stones animate shape: `BecomeCreature` on itself adds the
 * Creature type in Layer 4 while keeping its printed Land — Desert type line ("It's still a land").
 */
val HostileDesert = card("Hostile Desert") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Desert"
    oracleText = "{T}: Add {C}.\n{2}, Exile a land card from your graveyard: This land becomes a " +
        "3/4 Elemental creature until end of turn. It's still a land."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.ExileFromGraveyard(1, GameObjectFilter.Land))
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 4,
            creatureTypes = setOf(Subtype.ELEMENTAL.value),
            duration = Duration.EndOfTurn,
        )
        description = "{2}, Exile a land card from your graveyard: This land becomes a 3/4 " +
            "Elemental creature until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "178"
        artist = "Josh Hass"
        flavorText = "Amonkhet's desiccated land thirsts for blood."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/250c35cb-77e7-4600-a915-ccf327fe3385.jpg?1783935996"
        ruling(
            "2017-07-14",
            "If Hostile Desert becomes a creature the same turn it enters the battlefield, you can't " +
                "attack with it or tap it for mana."
        )
        ruling(
            "2017-04-18",
            "Desert is a land subtype with no special meaning. It doesn't grant the land an " +
                "intrinsic mana ability. Other cards may care about which lands are Deserts."
        )
    }
}
