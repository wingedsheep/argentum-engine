package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.RegenerateEffect
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Tribal Golem
 * {6}
 * Artifact Creature — Golem
 * 4/4
 * Tribal Golem has trample as long as you control a Beast, haste as long as you
 * control a Goblin, first strike as long as you control a Soldier, flying as long
 * as you control a Wizard, and "{B}: Regenerate Tribal Golem" as long as you
 * control a Zombie.
 */
val TribalGolem = card("Tribal Golem") {
    manaCost = "{6}"
    typeLine = "Artifact Creature — Golem"
    power = 4
    toughness = 4
    oracleText = "Tribal Golem has trample as long as you control a Beast, haste as long as you control a Goblin, first strike as long as you control a Soldier, flying as long as you control a Wizard, and \"{B}: Regenerate Tribal Golem\" as long as you control a Zombie."

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, StaticTarget.SourceCreature)
        condition = Conditions.ControlCreatureOfType(Subtype("Beast"))
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, StaticTarget.SourceCreature)
        condition = Conditions.ControlCreatureOfType(Subtype("Goblin"))
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, StaticTarget.SourceCreature)
        condition = Conditions.ControlCreatureOfType(Subtype("Soldier"))
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, StaticTarget.SourceCreature)
        condition = Conditions.ControlCreatureOfType(Subtype("Wizard"))
    }

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = RegenerateEffect(EffectTarget.Self)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.ControlCreatureOfType(Subtype("Zombie")))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "311"
        artist = "Edward P. Beard, Jr."
        flavorText = "It awakens to the call of its kindred."
        imageUri = "https://cards.scryfall.io/large/front/6/e/6e208be1-8b24-4048-90b2-6389f08043d1.jpg?1562920935"
    }
}
